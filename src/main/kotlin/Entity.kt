import javafx.scene.canvas.GraphicsContext
import utils.Utils
import utils.Vector2D
import java.io.DataInputStream
import java.util.*

/**
 * simplified representation of the server-entity used for rendering it on the client
 * @param position the position of the entity
 * @param rotation the rotation of the entity in rad
 * @param uuid the uuid of the entity (must match an uuid on the server)
 * @param isThisPlayer true if the entity is the player of this client
 */
abstract class Entity(var position: Vector2D, var rotation: Double, var uuid: UUID, val isThisPlayer: Boolean) {

    /**
     * renders the entity
     */
    abstract var renderer: Renderer
        protected set

    /**
     * identifies the type of entity uniquely
     */
    abstract val identifier: Int

    var isMarkedForRemoval: Boolean = false
        internal set

    /**
     * renders the entity using the [renderer]
     */
    open fun render(gc: GraphicsContext, client: Client) {
        renderer.render(gc, client)
    }

    /**
     * renders the background of the entity
     */
    open fun renderBg(gc: GraphicsContext, client: Client) { }

    /**
     * marks the entity for removal _(only on the client, not on the server!)_
     */
    fun markForRemoval() {
        isMarkedForRemoval = true
    }

    /**
     * updates the entity according to the incrementalUpdate
     */
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

/**
 * represents a polygonEntity
 * @param position the position of the entity
 * @param vertices the vertices of the polygon _note: because this entity is only used for rendering and does
 * not perform any physics calculations, the polygon can be concave_
 * @param rotation the rotation of the entity in rad
 * @param uuid the uuid of the entity (must match an uuid on the server)
 * @param isThisPlayer true if the entity is the player of this client
 */
open class PolygonEntity(
    position: Vector2D,
    vertices: Array<Vector2D>,
    rotation: Double,
    uuid: UUID,
    isThisPlayer: Boolean
) : Entity(position, rotation, uuid, isThisPlayer) {

    /**
     * the relative vertices of the polygon
     */
    val verticesRelative: Array<Vector2D> = Utils.getShapeWithCentroidZero(vertices)

    /**
     * renders the polygon
     */
    override var renderer: Renderer = PolyColorRenderer(this)

    /**
     * the identifier of the polygonEntity
     */
    override val identifier: Int = Int.MAX_VALUE

    /**
     * the absolute vertices of the polygon (the vertices in the game world)
     *
     * _Note: these are recalculated every time the getter is called, so instead of calling it multiple times, it is
     * better to cache the result in a local variable._
     */
    val verticesAbsolute: Array<Vector2D>
        get() {
            return Array(verticesRelative.size) {
                Utils.rotatePointAroundPoint(verticesRelative[it] + this.position, position, rotation)
            }
        }

    /**
     * gets the relative vertices of the polygon scaled by a factor
     * @param scale the scale
     *
     * _note: same as [verticesAbsolute]_
     */
    fun getScaledVerts(scale: Double): Array<Vector2D> {
        return Array(verticesRelative.size) {
            Utils.rotatePointAroundPoint(verticesRelative[it] + this.position, position, rotation)
        }
    }

    companion object {

        /**
         * the identifier of the polygonEntity
         */
        const val identifier: Int = Int.MAX_VALUE

        /**
         * deserializes a PolygonEntity
         */
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

/**
 * dosent work, dont worry about it
 */
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