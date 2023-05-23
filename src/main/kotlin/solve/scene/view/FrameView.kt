package solve.scene.view

import io.github.palexdev.materialfx.controls.MFXContextMenu
import javafx.beans.InvalidationListener
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.MapChangeListener
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.stage.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import solve.scene.model.Layer
import solve.scene.model.VisualizationFrame
import solve.scene.view.association.AssociationLine
import solve.scene.view.association.AssociationsManager
import solve.scene.view.drawing.BufferedImageView
import solve.scene.view.drawing.FrameDrawer
import solve.scene.view.drawing.FrameEventManager
import solve.scene.view.drawing.ImageFrameElement
import solve.scene.view.drawing.RectangleFrameElement
import solve.scene.view.landmarks.LandmarkView
import solve.utils.CacheElement
import solve.utils.Storage
import solve.utils.Updatable
import solve.utils.dialogs.ChooserDialog
import solve.utils.materialfx.action
import solve.utils.materialfx.item
import solve.utils.materialfx.lineSeparator
import solve.utils.materialfx.mfxContextMenu
import tornadofx.*
import tornadofx.add
import solve.utils.structures.Size as DoubleSize

/**
 * Represents one frame, superimposes landmarks (canvas and non-canvas on top of the image).
 * Also, responsible for async frame data loading.
 * Shows loading indicator while data (image and landmarks) not loaded.
 *
 * @param size frame size including indent.
 * @param scale global scene scale property.
 * @param frameViewStorage storage, where FrameView should be placed when it no more in usage.
 * @param canvasLayersCount number of layers, which landmarks use canvas to draw, used to init frame buffer.
 * @param parameters parameters of FrameView which can not be shared between different scenes and should be updated on scene changes.
 * @param frame data object which produces image and landmarks.
 */
class FrameView(
    val size: DoubleSize,
    private val scale: DoubleProperty,
    private val frameViewStorage: Storage<FrameView>,
    canvasLayersCount: Int,
    parameters: FrameViewParameters,
    frame: VisualizationFrame?
) : Group(), CacheElement<FrameViewData>, Updatable<VisualizationFrame?> {
    private var coroutineScope = parameters.coroutineScope
    private var associationsManager = parameters.associationsManager
    private var orderManager = parameters.orderManager

    private var drawnLandmarks: Map<Layer, List<LandmarkView>>? = null
    private var drawnImage: Image? = null
    private var currentFrame: VisualizationFrame? = null
    private val canvas = BufferedImageView(size.width, size.height, scale.value)
    private val frameDrawer = FrameDrawer(canvas, canvasLayersCount + 1)
    private val frameEventManager = FrameEventManager(canvas, scale)
    private var currentJob: Job? = null

    private val scaleChangedListener = InvalidationListener { scaleImageAndLandmarks(scale.value) }

    private val orderChangedCallback = {
        draw()
    }

    private val associationsUpdatedListener =
        MapChangeListener<AssociationsManager.AssociationKey<VisualizationFrame>,
            Map<VisualizationFrame, List<AssociationLine>>> {
            hasAssociations.value = getAssociatedLayersNames(currentFrame ?: return@MapChangeListener).isNotEmpty()
        }

    private val hasKeypoints = SimpleBooleanProperty(frame?.hasPoints() ?: false)
    private val hasAssociations = SimpleBooleanProperty(false)

    init {
        // image should be drawn below landmarks
        canvas.viewOrder = IMAGE_VIEW_ORDER
        add(canvas)
        init(FrameViewData(frame, parameters))

        addAssociationListeners()
        addChooseSecondAssociationFrameAction()

        // associations context menu
        mfxContextMenu {
            addCopyTimestampAction()
            lineSeparator()
            addAssociationActions()
        }
    }

    /**
     * Refreshes has keypoints and has associations before context menu showing to disable buttons
     * if action can't be done.
     */
    private fun Node.addAssociationListeners() {
        setOnContextMenuRequested {
            hasKeypoints.value = currentFrame?.hasPoints()
            hasAssociations.value =
                getAssociatedLayersNames(currentFrame ?: return@setOnContextMenuRequested).isNotEmpty()
        }
    }

    /**
     * Chooses second point layer to associate with.
     */
    private fun Node.addChooseSecondAssociationFrameAction() {
        setOnMouseClicked { mouse ->
            if (mouse.button != MouseButton.PRIMARY) {
                return@setOnMouseClicked
            }
            val clickedFrame = currentFrame ?: return@setOnMouseClicked
            val chosenLayerName = associationsManager.chosenLayerName ?: return@setOnMouseClicked
            val layer = clickedFrame.layers.filterIsInstance<Layer.PointLayer>().single { it.name == chosenLayerName }
            val associationKey = AssociationsManager.AssociationKey(clickedFrame, layer.name)
            val associationParameters = AssociationsManager.AssociationParameters(associationKey, layer.getLandmarks())
            associationsManager.associate(
                associationParameters,
                layer.settings.commonColorProperty,
                layer.settings.enabledProperty
            )
        }
    }

    /**
     * Copies frame timestamp to clipboard.
     */
    private fun MFXContextMenu.addCopyTimestampAction() {
        item("Copy timestamp").action {
            val timestamp = currentFrame?.timestamp ?: return@action
            val clipboardContent = ClipboardContent().also { it.putString(timestamp.toString()) }
            Clipboard.getSystemClipboard().setContent(clipboardContent)
        }
    }

    private fun MFXContextMenu.addAssociationActions() {
        item("Associate keypoints").also { it.enableWhen(hasKeypoints) }.action {
            val clickedFrame = currentFrame ?: return@action
            val layer = choosePointLayer(clickedFrame, ownerWindow) ?: return@action
            val associationKey = AssociationsManager.AssociationKey(clickedFrame, layer.name)
            val associationParameters = AssociationsManager.AssociationParameters(associationKey, layer.getLandmarks())
            associationsManager.initAssociation(associationParameters)
        }

        item("Clear associations").also { it.enableWhen(hasAssociations) }.action {
            val clickedFrame = currentFrame ?: return@action
            val layer = chooseAssociatedPointLayer(clickedFrame, ownerWindow) ?: return@action
            val associationKey = AssociationsManager.AssociationKey(clickedFrame, layer.name)
            associationsManager.clearAssociation(associationKey)
        }
    }

    /**
     * Chooses available to clear associations layer.
     */
    private fun chooseAssociatedPointLayer(frame: VisualizationFrame, owner: Window): Layer.PointLayer? {
        val associatedLayersNames = getAssociatedLayersNames(frame)
        val enabledAssociatedPointLayers = frame.layers
            .filterIsInstance<Layer.PointLayer>()
            .filter { layer -> associatedLayersNames.any { it == layer.name } }
            .filter { it.settings.enabled }
        return chooseLayer(enabledAssociatedPointLayers, owner)
    }

    private fun getAssociatedLayersNames(frame: VisualizationFrame): List<String> {
        val layerNames = associationsManager.drawnAssociations
            .filter { it.key.frame == frame && it.value.isNotEmpty() }
            .map { it.key.layerName }
        return layerNames.filter { layerName -> frame.layers.any { it.name == layerName && it.settings.enabled } }
    }

    private fun choosePointLayer(frame: VisualizationFrame, owner: Window): Layer.PointLayer? {
        val enabledPointLayers = frame.layers
            .filterIsInstance<Layer.PointLayer>()
            .filter { it.settings.enabled }
        return chooseLayer(enabledPointLayers, owner)
    }

    /**
     * User chooses one layer from collection using dialog.
     */
    private fun <T> chooseLayer(layers: List<T>, owner: Window): T? {
        if (layers.isEmpty()) {
            return null
        }

        return if (layers.count() == 1) {
            layers.single()
        } else {
            val chooserDialog = ChooserDialog<T>("Choose layer", 200.0, 150.0, owner)
            chooserDialog.choose(layers)
        }
    }

    /**
     * Update frame parameters when frame is reused from cache.
     */
    override fun init(params: FrameViewData) {
        scale.addListener(scaleChangedListener)
        setFrame(params.frame)
        updateParameters(params.frameViewParameters)
        scaleImageAndLandmarks(scale.value)
    }

    private fun updateParameters(parameters: FrameViewParameters) {
        coroutineScope = parameters.coroutineScope
        associationsManager = parameters.associationsManager
        associationsManager.drawnAssociations.addListener(associationsUpdatedListener)
        orderManager = parameters.orderManager
        orderManager.addOrderChangedListener(orderChangedCallback)
    }

    override fun update(data: VisualizationFrame?) {
        setFrame(data)
    }

    /**
     * Update frame data.
     *
     * @param frame data object, null for empty frames when frames number is not multiple of columns number.
     */
    fun setFrame(frame: VisualizationFrame?) {
        // avoid redundant updates on zooming.
        if (DelayedFramesUpdatesManager.shouldDelay) {
            DelayedFramesUpdatesManager.delayUpdate(this, frame)
            return
        }
        if (frame == currentFrame) {
            return
        }

        // new frame can be set while old frame loading
        currentJob?.cancel()
        disposeLandmarkViews()
        removeLandmarksNodes()
        frameDrawer.clear()
        frameDrawer.fullRedraw()

        currentFrame = frame

        if (frame == null) {
            drawnImage = null
            drawnLandmarks = mapOf()
            return
        }

        currentJob = Job()
        // fills frame with grey rectangle while data is not loaded
        drawLoadingIndicator()

        coroutineScope.launch(currentJob!!) {
            if (!isActive) return@launch // if task cancelled
            // load landmarks from files for all layers
            val landmarkData = frame.layers.associateWith { it.getLandmarks() }

            if (!isActive) return@launch // if task cancelled
            val image = frame.getImage()

            // Visual actions can be performed only in UI thread
            withContext(Dispatchers.JavaFx) {
                if (!this@launch.isActive) return@withContext // if task cancelled
                val landmarkViews = landmarkData.mapValues {
                    it.value.map { landmark ->
                        LandmarkView.create(
                            landmark,
                            orderManager.indexOf(it.key.settings),
                            scale.value,
                            frameDrawer,
                            frameEventManager
                        )
                    }
                }
                validateImage(image)
                drawnImage = image
                drawnLandmarks = landmarkViews
                draw()
                addLandmarksNodes()
            }
        }

        currentFrame = frame
    }

    fun dispose() {
        scale.removeListener(scaleChangedListener)
        associationsManager.drawnAssociations.removeListener(associationsUpdatedListener)
        orderManager.removeOrderChangedListener(orderChangedCallback)
        disposeLandmarkViews()
        frameViewStorage.store(this)
    }

    /**
     * Performs all canvas drawing actions and manages landmarks view order.
     */
    private fun draw() {
        val image = drawnImage ?: return
        frameDrawer.clear()
        frameDrawer.addOrUpdateElement(ImageFrameElement(FrameDrawer.IMAGE_VIEW_ORDER, image))

        drawnLandmarks = drawnLandmarks?.toSortedMap(compareBy { layer -> orderManager.indexOf(layer.settings) })

        doForAllLandmarks { view, layerIndex ->
            view.viewOrder = layerIndex
            view.addToFrameDrawer()
        }

        frameDrawer.fullRedraw()
    }

    private fun disposeLandmarkViews() = doForAllLandmarks { view, _ -> view.dispose() }

    private fun scaleImageAndLandmarks(newScale: Double) {
        canvas.scale(newScale)
        doForAllLandmarks { view, _ -> view.scale = newScale }
    }

    private fun validateImage(image: Image) {
        if (image.height != size.height || image.width != size.width) {
            println("Image size doesn't equal to the frame size") // TODO: warn user
        }
    }

    private fun removeLandmarksNodes() {
        doForAllLandmarks { view, _ ->
            if (view.node != null) {
                children.remove(view.node)
            }
        }
    }

    private fun addLandmarksNodes() = doForAllLandmarks { view, _ ->
        if (view.node != null) {
            children.add(view.node)
        }
    }

    private fun drawLoadingIndicator() {
        frameDrawer.addOrUpdateElement(
            RectangleFrameElement(
                FrameDrawer.IMAGE_VIEW_ORDER,
                Color.GREY,
                frameDrawer.width,
                frameDrawer.height
            )
        )
        frameDrawer.fullRedraw()
    }

    private fun doForAllLandmarks(delegate: (LandmarkView, Int) -> Unit) =
        drawnLandmarks?.values?.forEachIndexed { layerIndex, landmarkViews ->
            landmarkViews.forEach { view -> delegate(view, layerIndex) }
        }

    private fun VisualizationFrame.hasPoints() =
        this.layers.filterIsInstance<Layer.PointLayer>().any { it.settings.enabled }
}
