package solve.catalogue.view.fields

import javafx.scene.control.Labeled
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import solve.catalogue.loadImage
import solve.catalogue.model.CatalogueField
import javafx.scene.image.Image
import tornadofx.*

class CatalogueFileNamesFieldsView : CatalogueFieldsView() {
    companion object {
        private const val ListViewFieldIconSize = 20.0
    }

    private val fileNamesFieldIconImage = loadImage("icons/catalogue_image_icon.png")

    override val dragViewMaxFieldsNumber = 100
    override val listViewCellHeight = 25.0

    init {
        initialize()
    }

    override fun setFileNamesListViewCellFormat(labeled: Labeled, item: CatalogueField?) {
        super.setFileNamesListViewCellFormat(labeled, item)
        labeled.text = item?.fileName
        if (fileNamesFieldIconImage != null) {
            labeled.graphic = imageview(fileNamesFieldIconImage) {
                fitHeight = ListViewFieldIconSize
                isPreserveRatio = true
            }
        }
    }

    override val root = fieldsListView
}