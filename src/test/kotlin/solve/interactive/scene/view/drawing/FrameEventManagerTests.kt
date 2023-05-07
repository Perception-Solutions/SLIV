package solve.interactive.scene.view.drawing

import javafx.beans.property.SimpleDoubleProperty
import javafx.event.Event
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.shape.Rectangle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import solve.interactive.InteractiveTestClass
import solve.scene.view.drawing.CanvasEventHandler
import solve.scene.view.drawing.FrameEventManager
import solve.scene.view.drawing.RectangleFrameElement
import tornadofx.*

@ExtendWith(ApplicationExtension::class)
internal class FrameEventManagerTests : InteractiveTestClass() {
    private val node = Rectangle(10.0, 10.0)
    private val scale = SimpleDoubleProperty(1.0)

    @BeforeEach
    fun setUp() {
        scale.value = 1.0
    }

    @Test
    fun `Add mouse pressed handler`(robot: FxRobot) {
        val eventManager = FrameEventManager(node, scale)
        val frameElement = RectangleFrameElement(0, c("FFFF00"), 5, 5)
        val handledEvents = mutableListOf<MouseEvent>()
        val eventHandler = CanvasEventHandler<MouseEvent>(frameElement) {
            handledEvents.add(it)
        }
        val x = 1.0
        val y = 1.0
        val mouseButton = MouseButton.PRIMARY
        eventManager.subscribeMousePressed(eventHandler)
        robot.interact {
            node.fireMousePressed(x, y, mouseButton)
        }
        assertEquals(1, handledEvents.size)
        val pressEvent = handledEvents.single()
        assertEquals(x, pressEvent.x)
        assertEquals(y, pressEvent.y)
        assertEquals(mouseButton, pressEvent.button)
    }

    @Test
    fun `Add mouse released handler`(robot: FxRobot) {
        val eventManager = FrameEventManager(node, scale)
        val frameElement = RectangleFrameElement(0, c("FFFF00"), 5, 5)
        val handledEvents = mutableListOf<MouseEvent>()
        val eventHandler = CanvasEventHandler<MouseEvent>(frameElement) {
            handledEvents.add(it)
        }
        val x = 1.0
        val y = 1.0
        val mouseButton = MouseButton.PRIMARY
        eventManager.subscribeMouseReleased(eventHandler)
        robot.interact {
            node.fireMouseReleased(x, y, mouseButton)
        }
        assertEquals(1, handledEvents.size)
        val pressEvent = handledEvents.single()
        assertEquals(x, pressEvent.x)
        assertEquals(y, pressEvent.y)
        assertEquals(mouseButton, pressEvent.button)
    }

    @Test
    fun `More than one subscriber for one frame element`(robot: FxRobot) {
        val eventManager = FrameEventManager(node, scale)
        val frameElement = RectangleFrameElement(0, c("FFFF00"), 5, 5)
        val handledEvents = mutableListOf<MouseEvent>()
        val eventHandler1 = CanvasEventHandler<MouseEvent>(frameElement) {
            handledEvents.add(it)
        }
        val eventHandler2 = CanvasEventHandler<MouseEvent>(frameElement) {
            handledEvents.add(it)
        }
        val x = 1.0
        val y = 1.0
        val mouseButton = MouseButton.PRIMARY
        eventManager.subscribeMouseReleased(eventHandler1)
        eventManager.subscribeMouseReleased(eventHandler2)
        robot.interact {
            node.fireMouseReleased(x, y, mouseButton)
        }
        assertEquals(1, handledEvents.size)
    }

    @Test
    fun `Missing click`(robot: FxRobot) {
        val eventManager = FrameEventManager(node, scale)
        val frameElement = RectangleFrameElement(0, c("FFFF00"), 5, 5)
        val handledEvents = mutableListOf<MouseEvent>()
        val eventHandler = CanvasEventHandler<MouseEvent>(frameElement) {
            handledEvents.add(it)
        }
        val x = 6.0
        val y = 6.0
        val mouseButton = MouseButton.PRIMARY
        eventManager.subscribeMouseReleased(eventHandler)
        robot.interact {
            node.fireMouseReleased(x, y, mouseButton)
        }
        assertEquals(0, handledEvents.size)
    }

    @Test
    fun `Overlapping frame elements`(robot: FxRobot) {
        val eventManager = FrameEventManager(node, scale)
        val frameElement1 = RectangleFrameElement(0, c("FFFF00"), 5, 5)
        val frameElement2 = RectangleFrameElement(1, c("FFFF00"), 5, 5)
        val handledEvents1 = mutableListOf<MouseEvent>()
        val eventHandler1 = CanvasEventHandler<MouseEvent>(frameElement1) {
            handledEvents1.add(it)
        }
        val handledEvents2 = mutableListOf<MouseEvent>()
        val eventHandler2 = CanvasEventHandler<MouseEvent>(frameElement2) {
            handledEvents2.add(it)
        }
        val x = 1.0
        val y = 1.0
        val mouseButton = MouseButton.PRIMARY
        eventManager.subscribeMouseReleased(eventHandler1)
        eventManager.subscribeMouseReleased(eventHandler2)
        robot.interact {
            node.fireMouseReleased(x, y, mouseButton)
        }
        assertEquals(0, handledEvents1.size)
        assertEquals(1, handledEvents2.size)
    }

    @Test
    fun `Scale frame`(robot: FxRobot) {
        val eventManager = FrameEventManager(node, scale)
        val frameElement = RectangleFrameElement(0, c("FFFF00"), 5, 5)
        val handledEvents = mutableListOf<MouseEvent>()
        val eventHandler = CanvasEventHandler<MouseEvent>(frameElement) {
            handledEvents.add(it)
        }
        val x = 6.0
        val y = 6.0
        val mouseButton = MouseButton.PRIMARY
        eventManager.subscribeMouseReleased(eventHandler)
        robot.interact {
            node.fireMouseReleased(x, y, mouseButton)
        }
        assertEquals(0, handledEvents.size)
    }

    private fun Node.fireMousePressed(x: Double, y: Double, button: MouseButton) =
        this.fireMouseEvent(MouseEvent.MOUSE_PRESSED, x, y, button)

    private fun Node.fireMouseReleased(x: Double, y: Double, button: MouseButton) =
        this.fireMouseEvent(MouseEvent.MOUSE_RELEASED, x, y, button)

    private fun Node.fireMouseEvent(kind: EventType<MouseEvent>, x: Double, y: Double, button: MouseButton) {
        Event.fireEvent(
            this,
            MouseEvent(
                kind,
                x, y, x, y, button, 1,
                false, false, false, false,
                true, false, false, false,
                false, false, null
            )
        )
    }
}