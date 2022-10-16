package sliv.tool.project.model

import java.nio.file.Path

data class ProjectFrame(val timestamp: Long, val imagePath: Path, val outputs: List<LandmarkFile>)