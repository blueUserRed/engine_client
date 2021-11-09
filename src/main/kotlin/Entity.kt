import javafx.scene.canvas.GraphicsContext
import utils.Utils
import utils.Vector2D
import java.io.DataInputStream
import java.util.*

abstract class Entity(var position: Vector2D, var rotation: Double, var uuid: UUID, val isThisPlayer: Boolean) {

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
    uuid: UUID,
    isThisPlayer: Boolean
) : Entity(position, rotation, uuid, isThisPlayer) {

    val verticesRelative: Array<Vector2D> = Utils.getShapeWithCentroidZero(vertices)

    override var renderer: Renderer = PolyColorRenderer(this)

    override val identifier: Int = Int.MAX_VALUE

    val verticesAbsolute: Array<Vector2D>
        get() {
            return Array(verticesRelative.size) {
                Utils.rotatePointAroundPoint(verticesRelative[it] + this.position, position, rotation)
            }
        }

    fun getScaledVerts(scale: Double): Array<Vector2D> {
        return Array(verticesRelative.size) {
            Utils.rotatePointAroundPoint(verticesRelative[it] + this.position, position, rotation)
        }
    }

    override fun render(gc: GraphicsContext, client: Client) {
        renderer.render(gc, client)
    }

    companion object {

        const val identifier: Int = Int.MAX_VALUE

        fun deserialize(input: DataInputStream, client: Client): Entity? {
            val uuid = UUID(input.readLong(), input.readLong())
            val isThisPlayer = input.readBoolean()
            val numVerts = input.readInt()
            val verts = Array(numVerts) { Vector2D() }
            for (i in 0 until numVerts) {
                val vec = Vector2D.deserialize(input)
                verts[i] = vec
            }
            val pos = Vector2D.deserialize(input)
            val rot = input.readDouble()
            val polygonEntity = PolygonEntity(pos, verts, rot, uuid, isThisPlayer)
            polygonEntity.renderer = Renderer.deserialize(input, client, polygonEntity) ?: run {
                Conf.logger.warning("Couldn't deserialize Renderer!")
                return null
            }
            return polygonEntity
        }
    }
}

class CircleEntity(
    position: Vector2D,
    rotation: Double,
    uuid: UUID,
    isThisPlayer: Boolean,
    val radius: Double
) : Entity(position, rotation, uuid, isThisPlayer) {

    override var renderer: Renderer = EmptyRenderer()

    override val identifier: Int = Int.MAX_VALUE - 1

    override fun render(gc: GraphicsContext, client: Client) {
        renderer.render(gc, client)
    }

    companion object {

        const val identifier: Int = Int.MAX_VALUE - 1

        fun deserialize(input: DataInputStream, client: Client): CircleEntity? {
            val uuid = UUID(input.readLong(), input.readLong())
            val isThisPlayer = input.readBoolean()
            val pos = Vector2D.deserialize(input)
            val rot = input.readDouble()
            val rad = input.readDouble()
            val circleEntity = CircleEntity(pos, rot, uuid, isThisPlayer, rad)
            circleEntity.renderer = Renderer.deserialize(input, client, circleEntity) ?: run {
                Conf.logger.warning("Couldn't deserialize Renderer!")
                return null
            }
            return circleEntity
        }
    }

}