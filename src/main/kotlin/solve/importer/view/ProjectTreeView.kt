package solve.importer.view

import javafx.scene.control.*
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import javafx.scene.image.ImageView
import solve.constants.IconsImporterDescriptionPath
import solve.constants.IconsImporterErrorFilePath
import solve.constants.IconsImporterErrorFolderPath
import solve.constants.IconsImporterPhotoPath
import solve.importer.ProjectParser
import solve.importer.controller.ImporterController
import solve.importer.model.FileInfo
import solve.importer.model.FileInTree
import solve.utils.loadResourcesImage
import solve.utils.toStringWithoutBrackets
import tornadofx.*

open class ProjectTreeView : View() {
    private var rootTree = TreeItem(FileInTree(FileInfo("", false)))

    private val controller: ImporterController by inject()

    private fun updateTree() {
        controller.directoryPath.onChange {
            if (!it.isNullOrEmpty()) {
                rootTree.children.clear()
                if (controller.project.value != null) {
                    rootTree = ProjectParser.createTreeWithFiles(controller.project.value, rootTree)
                }
            }
        }
    }

    override val root = treetableview(rootTree) {
        visibleWhen { controller.project.isNotNull }
        this.isShowRoot = false

        val filesColumn: TreeTableColumn<FileInTree, FileInfo> = TreeTableColumn<FileInTree, FileInfo>().apply {
            isResizable = false
            prefWidth = 220.0
        }
        val errorsColumn: TreeTableColumn<FileInTree, FileInfo> = TreeTableColumn<FileInTree, FileInfo>().apply {
            isResizable = false
            prefWidth = 150.0
        }

        filesColumn.cellValueFactory = TreeItemPropertyValueFactory("file")
        errorsColumn.cellValueFactory = TreeItemPropertyValueFactory("file")

        filesColumn.setCellFactory { _ ->
            object : TreeTableCell<FileInTree, FileInfo?>() {
                private val imageIcon = loadResourcesImage(IconsImporterPhotoPath)
                private val fileIcon = loadResourcesImage(IconsImporterDescriptionPath)
                private val errorFolderIcon = loadResourcesImage(IconsImporterErrorFolderPath)
                private val errorFileIcon = loadResourcesImage(IconsImporterErrorFilePath)

                override fun updateItem(item: FileInfo?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else item?.name
                    graphic = if (empty) {
                        null
                    } else {
                        if (item != null) {
                            if (item.isLeaf) {
                                if (item.errors.isEmpty()) {
                                    ImageView(fileIcon)
                                } else {
                                    ImageView(errorFileIcon)
                                }
                            } else {
                                if (item.errors.isEmpty()) {
                                    ImageView(imageIcon)
                                } else {
                                    ImageView(errorFolderIcon)
                                }
                            }
                        }
                        else {
                            ImageView()
                        }
                    }
                }
            }
        }

        errorsColumn.setCellFactory {
            object : TreeTableCell<FileInTree, FileInfo?>() {
                override fun updateItem(item: FileInfo?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) {
                        null
                    } else item?.errors?.toStringWithoutBrackets()
                    if (!empty && text.isNotEmpty()) {
                        tooltip(text)
                    }
                }
            }
        }

        this.columns.addAll(filesColumn, errorsColumn)
        updateTree()
    }
}