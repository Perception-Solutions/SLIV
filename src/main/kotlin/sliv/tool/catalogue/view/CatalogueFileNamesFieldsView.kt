package sliv.tool.catalogue.view

import javafx.collections.ObservableList
import javafx.scene.Scene
import javafx.scene.control.Labeled
import javafx.scene.control.SelectionMode
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import sliv.tool.catalogue.*
import sliv.tool.catalogue.controller.CatalogueController
import sliv.tool.catalogue.model.CatalogueField
import sliv.tool.project.model.ProjectFrame
import sliv.tool.scene.view.SceneView
import tornadofx.*
import kotlin.math.min

class CatalogueFileNamesFieldsView: View(), SelectionNode {
    companion object {
        private const val ListViewFieldCellHeight = 30.0
        private const val ListViewFieldIconSize = 20.0

        private const val DragViewMaxFieldsNumber = 100
    }

    private val fields: ObservableList<CatalogueField> by param()

    private val controller: CatalogueController by inject()
    private val sceneView: SceneView by inject()

    override val areSelectedAllItems: Boolean
        get() = fileNamesListView.selectedItemsCount == controller.model.frames.count()
    override val isSelectionEmpty: Boolean
        get() = fileNamesListView.selectedItems.isEmpty()
    override val selectedItems: List<CatalogueField>
        get() = fileNamesListView.selectedItems
    override val selectedFrames: List<ProjectFrame>
        get() = fileNamesListView.selectedItems.map { it.frame }

    private var isDragging = false

    private val fileNamesFieldIconImage = loadImage("catalogue_image_icon.png")
    private val fileNamesListView = listview(fields) {
        selectionModel.selectionMode = SelectionMode.MULTIPLE
        cellFormat {
            setFileNamesListViewCellFormat(this, it)
        }
    }

    override val root = fileNamesListView

    init {
        initializeDragEvents()
        initializeInteractionEvent()
    }

    override fun selectAllItems() = fileNamesListView.selectAllItems()

    override fun deselectAllItems() = fileNamesListView.deselectAllItems()

    private fun initializeInteractionEvent() {
        fileNamesListView.setOnMouseClicked {
            fire(CatalogueFieldsInteractionEvent)
        }
        fileNamesListView.onSelectionChanged {
            fire(CatalogueFieldsInteractionEvent)
        }
    }

    private fun initializeDragEvents() {
        fileNamesListView.setOnDragDetected {
            onCatalogueDragDetected()
        }
        sceneView.root.addEventFilter(DragEvent.DRAG_OVER, ::onSceneDragOver)
        sceneView.root.addEventFilter(DragEvent.DRAG_DROPPED, ::onSceneDragDropped)
    }

    private fun setFileNamesListViewCellFormat(labeled: Labeled, item: CatalogueField?) {
        labeled.text = item?.fileName
        if (fileNamesFieldIconImage != null) {
            labeled.graphic = imageview(fileNamesFieldIconImage) {
                fitHeight = ListViewFieldIconSize
                isPreserveRatio = true
            }
        }
        labeled.prefHeight = ListViewFieldCellHeight
    }

    private fun onSceneDragDropped(event: DragEvent) {
        if (isDragging) {
            controller.visualizeFramesSelection(selectedFrames)
        }

        isDragging = false
    }

    private fun onSceneDragOver(event: DragEvent) {
        if (isDragging) {
            event.acceptTransferModes(TransferMode.MOVE)
        }
    }

    private fun onCatalogueDragDetected() {
        val dragboard = root.startDragAndDrop(TransferMode.MOVE)
        val clipboardContent = ClipboardContent().apply { putString("") }
        dragboard.setContent(clipboardContent)
        dragboard.dragView = createFileNameFieldsSnapshot(fileNamesListView.selectedItems)
        isDragging = true
    }

    private fun createFileNameFieldsSnapshot(fields: List<CatalogueField>): Image {
        val snapshotFields = fields.take(DragViewMaxFieldsNumber).asObservable()
        val prefSnapshotHeight = (snapshotFields.count() * ListViewFieldCellHeight).floor()

        val fieldsSnapshotNode = listview(snapshotFields) {
            cellFormat {
                setFileNamesListViewCellFormat(this, it)
            }
        }
        fileNamesListView.getChildList()?.remove(fieldsSnapshotNode)
        val snapshotScene = Scene(fieldsSnapshotNode)

        val nodeSnapshot = fieldsSnapshotNode.snapshot(null, null)
        return WritableImage(
            nodeSnapshot.pixelReader, nodeSnapshot.width.floor(), min(nodeSnapshot.height.floor(), prefSnapshotHeight)
        )
    }
}