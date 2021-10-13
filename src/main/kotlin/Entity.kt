import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import utils.Utils
import utils.Vector2D
import java.io.DataInputStream
import java.lang.Math.random

abstract class Entity (
    var position: Vector2D,
    var rotation: Double
    ) {

    abstract var renderer: Renderer
        protected set

    abstract val identifier: Int
    abstract fun render(gc: GraphicsContext, client: Client)

}

class PolygonEntity(position: Vector2D, vertices: Array<Vector2D>, rotation: Double) : Entity(position, rotation) {

    val verticesRelative: Array<Vector2D> = Utils.getShapeWithCentroidZero(vertices)

    override var renderer: Renderer = PolygonRenderer(this)

    override val identifier: Int = Int.MAX_VALUE

    val verticesAbsolute: Array<Vector2D>
        get() {
            return Array(verticesRelative.size) {
                Utils.rotatePointAroundPoint(verticesRelative[it] + this.position, position, rotation)
            }
        }

    override fun render(gc: GraphicsContext, client: Client) {
        renderer.render(gc, client)
    }

    companion object {

        val identifier: Int = Int.MAX_VALUE

        fun deserialize(input: DataInputStream): Entity? {
            val numVerts = input.readInt()
            val verts = Array(numVerts) { Vector2D() }
            for (i in 0 until numVerts) {
                val vec = Vector2D.deserializer(input)
                verts[i] = vec
            }
            val pos = Vector2D.deserializer(input)
            val rot = input.readDouble()
            return PolygonEntity(pos, verts, rot)
        }

    }

}