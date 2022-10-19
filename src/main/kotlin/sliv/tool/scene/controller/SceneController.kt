package sliv.tool.scene.controller

import javafx.beans.property.SimpleObjectProperty
import sliv.tool.scene.model.*
import tornadofx.*

class SceneController : Controller() {
    val scene = SimpleObjectProperty<Scene>(Scene({ _ -> null }, 0, emptyList()))
}