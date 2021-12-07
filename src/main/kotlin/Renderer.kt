import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import utils.Vector2D
import utils.toDeg
import java.io.DataInputStream

/**
 * a renderer that renders an entity to a canvas
 */
interface Renderer {

    /**
     * identifiers the renderer-type uniquely
     */
    val identifier: Int

    /**
     * renders to the canvas
     */
    fun render(gc: GraphicsContext, client: Client)

    companion object {

        /**
         * calculates the coords on the screen from the in-game coords
         * @param gameCoords the in-game coords
         * @param client the client
         */
        fun gameCoordsToScreenCoords(gameCoords: Vector2D, client: Client): Vector2D {
            val scroll = client.scrollOffset
            return Vector2D(gameCoords.x - scroll.x, client.targetCanvas.height - (gameCoords.y - scroll.y) - 30.0)
        }

        /**
         * calculates the coords on the screen from the in-game coords
         * @param gameCoords the in-game coords
         * @param client the client
         */
        fun gameCoordsToScreenCoords(gameCoords: Array<Vector2D>, client: Client): Array<Vector2D> {
            return Array(gameCoords.size) { gameCoordsToScreenCoords(gameCoords[it], client) }
        }

        /**
         * registers the deserializers for the built-in renderers
         */
        internal fun registerRendererDeserializers(client: Client) {
            client.addRendererDeserializer(EmptyRenderer.identifier) { _, _, _ -> EmptyRenderer() }
            client.addRendererDeserializer(PolyColorRenderer.identifier, PolyColorRenderer.Companion::deserialize)
            client.addRendererDeserializer(PolyImageRenderer.identifier, PolyImageRenderer.Companion::deserialize)
            client.addRendererDeserializer(CircleColorRenderer.identifier, CircleColorRenderer.Companion::deserialize)
        }

        /**
         * deserializes a renderer
         * @param ent the entity to which the renderer belongs
         */
        fun deserialize(input: DataInputStream, client: Client, ent: Entity): Renderer? {
            val identifier = input.readInt()
            val deserializer = client.getRendererDeserializer(identifier) ?: run {
                Conf.logger.warning("Couldn't deserialize Renderer with unknown id '$identifier'")
                return null
            }
            return deserializer(input, ent, client) ?: run {
                Conf.logger.warning("Couldn't deserialie Renderer with id '$identifier'")
                return null
            }
        }

    }
}

/**
 * renders nothing
 */
class EmptyRenderer : Renderer {

    override val identifier: Int = Int.MAX_VALUE

    override fun render(gc: GraphicsContext, client: Client) {
    }

    companion object {
        val identifier: Int = Int.MAX_VALUE
    }

}

/**
 * renders a polygonEntity in a certain color
 */
class PolyColorRenderer(val ent: PolygonEntity) : Renderer {

    override val identifier: Int = Int.MAX_VALUE - 1

    /**
     * the scale at which the polygon is rendered
     */
    var scale: Double = 1.0

    /**
     * the color in which the polygon is rendered
     */
    var color: Color = Color.valueOf("#8800cc")

    override fun render(gc: GraphicsContext, client: Client) {
        val verts = Renderer.gameCoordsToScreenCoords(this.ent.getScaledVerts(scale), client)
        gc.fill = this.color
        gc.fillPolygon(extractXPoints(verts), extractYPoints(verts), verts.size)

//        gc.fill = Color.valueOf("#00ff00")
//        val centroid = Renderer.gameCoordsToScreenCoords(this.ent.position, client)
//        gc.fillOval(centroid.x - 5, centroid.y - 5, 10.0, 10.0)
    }

    companion object {

        const val identifier: Int = Int.MAX_VALUE - 1

        /**
         * deserializes a PolyColorRenderer
         */
        fun deserialize(input: DataInputStream, ent: Entity, client: Client): PolyColorRenderer? {
            if (ent !is PolygonEntity) return null
            val color = Color.color(input.readDouble(), input.readDouble(), input.readDouble())
            val renderer = PolyColorRenderer(ent)
            renderer.scale = input.readDouble()
            renderer.color = color
            return renderer
        }
    }

}

/**
 * renders a polygonEntity using an image
 * @param ent the polygonEntity
 * @param imageRes the resource used for rendering
 * @param offset the offset at which the image is renderd
 * @param width the width of the image
 * @param height the height of the image
 */
class PolyImageRenderer(
    val ent: PolygonEntity,
    val imageRes: ImageResource,
    var offset: Vector2D,
    val width: Double,
    val height: Double
) : Renderer {

    override val identifier: Int = Int.MAX_VALUE - 2

    var flip: Boolean = false

    override fun render(gc: GraphicsContext, client: Client) {
        val absVerts = ent.verticesAbsolute
        val verts = Renderer.gameCoordsToScreenCoords(absVerts, client)
        gc.beginPath()
        gc.moveTo(verts[0].x, verts[0].y)
        for (i in 1 until verts.size) gc.lineTo(verts[i].x, verts[i].y)
        gc.closePath()
        gc.clip()
        val screenPos = Renderer.gameCoordsToScreenCoords(ent.position, client)
        gc.translate(screenPos.x, screenPos.y)
        gc.rotate(ent.rotation.toDeg())
        if (flip) gc.scale(-1.0, 1.0)
        gc.drawImage(imageRes.image, offset.x, -offset.y, width, height)
    }

    companion object {

        val identifier: Int = Int.MAX_VALUE - 2

        /**
         * deserializes a PolyImageRenderer
         */
        fun deserialize(input: DataInputStream, ent: Entity, client: Client): PolyImageRenderer? {
            if (ent !is PolygonEntity) return null
            val offset = Vector2D.deserialize(input)
            val width = input.readDouble()
            val height = input.readDouble()
            val resId = input.readUTF()
            val resource = client.getResource(resId)
            if (resource == null || resource !is ImageResource) {
                Conf.logger.warning("Resource '$resId' does not exist or is not ImageResource")
                return null
            }
            val flip = input.readBoolean()
            val polyImageRenderer = PolyImageRenderer(ent, resource, offset, width, height)
            polyImageRenderer.flip = flip
            return polyImageRenderer
        }
    }
}

class CircleColorRenderer(val ent: CircleEntity) : Renderer {

    override val identifier: Int = Int.MAX_VALUE - 3

    var color: Color = Color.valueOf("#8800cc")

    override fun render(gc: GraphicsContext, client: Client) {
        gc.fill = color
        val pos = Renderer.gameCoordsToScreenCoords(ent.position - Vector2D(ent.radius), client)
        gc.fillOval(pos.x, pos.y, ent.radius * 2, ent.radius * 2)
    }

    companion object {

        const val identifier: Int = Int.MAX_VALUE - 3

        fun deserialize(input: DataInputStream, ent: Entity, client: Client): CircleColorRenderer? {
            if (ent !is CircleEntity) return null
            val color = Color.color(input.readDouble(), input.readDouble(), input.readDouble())
            val renderer = CircleColorRenderer(ent)
            renderer.color = color
            return renderer
        }
    }

}

//
//class VectorRenderer(var vec: Vector2D, var position: Vector2D) : Renderer {
//
//    override fun render(gc: GraphicsContext, client: Client) {
//        val p1 = Renderer.gameCoordsToScreenCoords(position, client)
//        val p2 = Renderer.gameCoordsToScreenCoords(position + vec, client)
//        gc.stroke = Color.valueOf("#ff0000")
//        gc.strokeLine(p1.x, p1.y, p2.x, p2.y)
//        gc.fill = Color.valueOf("#0000ff")
//        gc.fillOval(p2.x - 4, p2.y - 4, 8.0, 8.0)
//    }
//
//}
//
//class PointRenderer(var point: Vector2D, var radius: Double) : Renderer {
//
//    var color: Color = Color.valueOf("#0000ff")
//
//    override fun render(gc: GraphicsContext, client: Client) {
//        gc.fill = this.color
//        val sPoint = Renderer.gameCoordsToScreenCoords(point, client)
//        gc.fillOval(sPoint.x - radius / 2, sPoint.y - radius / 2, radius, radius)
//    }
//
//}

/**
 * gets all the x-values of an array of vertices
 */
private fun extractXPoints(vertices: Array<Vector2D>): DoubleArray {
    return DoubleArray(vertices.size) { vertices[it].x }
}

/**
 * gets all the y-values of an array of vertices
 */
private fun extractYPoints(vertices: Array<Vector2D>): DoubleArray {
    return DoubleArray(vertices.size) { vertices[it].y }
}