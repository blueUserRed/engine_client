import javafx.scene.canvas.GraphicsContext
import utils.Utils
import utils.Vector2D
import java.io.DataInputStream
import java.util.*

abstract class Entity(var position: Vector2D, var rotation: Double, var uuid: UUID) {

    abstract var renderer: Renderer
        protected set

    abstract val identifier: Int
    abstract fun render(gc: GraphicsContext, client: Client)

    open fun deserializeInc(input: DataInputStream, client: Client) {
        var tag = input.readByte()
        while (tag != 0xff.toByte()) {
            when(tag) {
                0.toByte() -> { position = Vector2D.deserialize(input)}
                1.toByte() -> { rotation = input.readDouble() }
                2.toByte() -> { renderer = Renderer.deserialize(input, client, this) ?: break }
            }
            tag = input.readByte()
        }
    }

}

class PolygonEntity(
    position: Vector2D,
    vertices: Array<Vector2D>,
    rotation: Double,
    uuid: UUID
) : Entity(position, rotation, uuid) {

    val verticesRelative: Array<Vector2D> = Utils.getShapeWithCentroidZero(vertices)

    override var renderer: Renderer = PolyColorRenderer(this)

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

        fun deserialize(input: DataInputStream, client: Client): Entity? {
            val uuid = UUID(input.readLong(), input.readLong())
            val numVerts = input.readInt()
            val verts = Array(numVerts) { Vector2D() }
            for (i in 0 until numVerts) {
                val vec = Vector2D.deserialize(input)
                verts[i] = vec
            }
            val pos = Vector2D.deserialize(input)
            val rot = input.readDouble()
            val polygonEntity = PolygonEntity(pos, verts, rot, uuid)
            polygonEntity.renderer = Renderer.deserialize(input, client, polygonEntity) ?: run {
                Conf.logger.warning("Couldn't deserialize Renderer!")
                return null
            }
            return polygonEntity
        }

    }

}