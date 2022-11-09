package sliv.tool.scene.view

import javafx.beans.property.SimpleDoubleProperty
import sliv.tool.scene.controller.SceneController
import sliv.tool.scene.view.virtualizedfx.VirtualizedFXGridProvider
import tornadofx.*
import kotlin.math.max
import kotlin.math.min

class SceneView : View() {
    private val controller: SceneController by inject()

    init {
        controller.scene.onChange { scene ->
            if (scene != null) {
                draw()
            }
        }
    }

    override val root = vbox {
        label("Empty scene placeholder")
    }

    private fun draw() {
        root.children.clear()

        val scene = controller.scene.value
        val frames = (0 until scene.framesCount).mapNotNull { i -> scene.getFrame.invoke(i) }

        //TODO: get size from the first image after data virtualization will be done
        val width = 500.0
        val height = 500.0
        val margin = 10.0

        val scaleProperty = SimpleDoubleProperty(1.0)

        val columnsNumber = 30 //TODO: should be set from the UI

        val grid = VirtualizedFXGridProvider.createGrid(
            frames, columnsNumber, width + margin, height + margin, scaleProperty
        ) { frame ->
            FrameView(
                width, height, scaleProperty, frame
            )
        }

        grid.setUpPanning()

        val gridNode = grid.getNode()

        val scaleFactor = 0.05
        val minScale = 0.6 // TODO: invalid grid appearance if scale is too small. 0.65 looks great, 0.6 doesn't
        val maxScale = 8.0 // Canvas breaks if scale is too big

        gridNode.setOnScroll { event ->
            if(event.deltaY > 0) {
                scaleProperty.value = min(scaleProperty.value + scaleFactor, maxScale)
            } else if(event.deltaY < 0) {
                scaleProperty.value = max(scaleProperty.value - scaleFactor, minScale)
            }
        }

        add(gridNode)
    }
}