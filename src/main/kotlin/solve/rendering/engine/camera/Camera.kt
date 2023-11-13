package solve.rendering.engine.camera

import org.joml.Matrix2f
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import solve.rendering.engine.utils.times

class Camera(var position: Vector2f = Vector2f(), zoom: Float = 1f) {
    var zoom: Float = zoom
        set(value) {
            if (value <= 0f) {
                println("The camera scale should be positive number!")
                return
            }
            field = value
        }

    fun calculateProjectionMatrix(projectionSize: Vector2f): Matrix4f {
        val orthoMatrix = calculateOrthoMatrix(projectionSize)
        val zoomMatrix = calculateZoomMatrix()

        return orthoMatrix * zoomMatrix
    }

    private fun calculateZoomMatrix(): Matrix4f = Matrix4f().scale(zoom * DefaultCameraScaleCoefficient)

    private fun calculateOrthoMatrix(projectionSize: Vector2f): Matrix4f {
        val projectionLeft = position.x * zoom - projectionSize.x / 2f
        val projectionRight = position.x * zoom + projectionSize.x / 2f
        val projectionTop = position.y * zoom + projectionSize.y / 2f
        val projectionBottom = position.y * zoom - projectionSize.y / 2f

        return Matrix4f().ortho(
            projectionLeft,
            projectionRight,
            projectionBottom,
            projectionTop,
            ProjectionOrthoZNear,
            ProjectionOrthoZFar
        )
    }

    companion object {
        private const val DefaultCameraScaleCoefficient = 1f
        private const val ProjectionOrthoZNear = -100f
        private const val ProjectionOrthoZFar = 100f
    }
}