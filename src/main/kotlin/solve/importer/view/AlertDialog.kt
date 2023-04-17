package solve.importer.view

import io.github.palexdev.materialfx.controls.MFXContextMenu
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.BorderPane
import kotlinx.coroutines.cancel
import solve.constants.IconsAlertError
import solve.styles.Style
import solve.utils.*
import tornadofx.*


class AlertDialog(contentText: String) : View() {
    private val errorIcon = loadResourcesImage(IconsAlertError)

    override val root =
        borderpane {
            top {
                label("Error") {
                    graphicTextGap = 15.0
                    graphic = ImageView(errorIcon)
                    BorderPane.setMargin(this, Insets(0.0,0.0,0.0,14.0))
                    prefHeight=0.0
                    style = "-fx-font-style: ${Style.fontCondensed}; -fx-font-size: ${Style.headerFontSize};"
                }
            }
            center {
                label(contentText){
                    mfxContextMenu{
                        style = "-fx-font-style: ${Style.fontCondensed}; -fx-font-size: ${Style.mainFontSize};"
                        copyError(text)
                    }

                    BorderPane.setMargin(this, Insets(0.0, 14.0, 0.0, 14.0))
                    style = "-fx-font-family: ${Style.font}; -fx-font-size: ${Style.mainFontSize};  -fx-text-fill: #000000;"
                    BorderPane.setAlignment(this, Pos.CENTER_LEFT)
                    isWrapText = true
                    prefWidth = 300.0
                }
            }

            bottom {
                mfxButton("OK") {
                    BorderPane.setMargin(this, Insets(0.0, 14.0, 14.0, 0.0))
                    maxWidth = 40.0
                    prefHeight = 23.0
                    style = "-fx-font-family: ${Style.fontCondensed}; -fx-font-size: 14px; -fx-font-weight: ${Style.fontWeightBold}; -fx-text-fill: #78909C;"
                    BorderPane.setAlignment(this, Pos.TOP_RIGHT)
                    prefWidth = 180.0
                    action {
                        find<ControlPanel>().coroutineScope.cancel()
                        close()
                    }
                }
            }
        }

    private fun MFXContextMenu.copyError(text: String) {
        item("Copy Error").action {
            val clipboardContent = ClipboardContent().also { it.putString(text) }
            Clipboard.getSystemClipboard().setContent(clipboardContent)
            val clipboard = Clipboard.getSystemClipboard()
            val content = ClipboardContent()
            content.putString(text)
            clipboard.setContent(content)
        }
    }
}