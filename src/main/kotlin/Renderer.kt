import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import utils.Vector2D
import utils.toDeg
import java.io.DataInputStream

interface Renderer {

    val identifier: Int

    fun render(gc: GraphicsContext, client: Client)

    companion object {

        fun gameCoordsToScreenCoords(gameCoords: Vector2D, client: Client): Vector2D {
            val scroll = client.scrollOffset
            return Vector2D(gameCoords.x - scroll.x, client.targetCanvas.height - (gameCoords.y - scroll.y) - 30.0)
        }

        fun gameCoordsToScreenCoords(gameCoords: Array<Vector2D>, client: Client): Array<Vector2D> {
            return Array(gameCoords.size) { gameCoordsToScreenCoords(gameCoords[it], client) }
        }

        internal fun registerRendererDeserializers(client: Client) {
            client.addRendererDeserializer(EmptyRenderer.identifier) { _, _, _ -> EmptyRenderer() }
            client.addRendererDeserializer(PolyColorRenderer.identifier, PolyColorRenderer.Companion::deserialize)
            client.addRendererDeserializer(PolyImageRenderer.identifier, PolyImageRenderer.Companion::deserialize)
            client.addRendererDeserializer(CircleColorRenderer.identifier, CircleColorRenderer.Companion::deserialize)
        }

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

class EmptyRenderer : Renderer {

    override val identifier: Int = Int.MAX_VALUE

    override fun render(gc: GraphicsContext, client: Client) {
    }

    companion object {
        val identifier: Int = Int.MAX_VALUE
    }

}

class PolyColorRenderer(val ent: PolygonEntity) : Renderer {

    override val identifier: Int = Int.MAX_VALUE - 1

    var scale: Double = 1.0

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

class PolyImageRenderer(
    val ent: PolygonEntity,
    val imageRes: ImageResource,
    var offset: Vector2D,
    val width: Double,
    val height: Double) : Renderer {

    override val identifier: Int = Int.MAX_VALUE - 2

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
        gc.drawImage(imageRes.image, offset.x, -offset.y, width, height)
    }

    companion object {

        val identifier: Int = Int.MAX_VALUE - 2

        fun deserialize(input: DataInputStream, ent: Entity, client: Client): PolyImageRenderer? {
            if (ent !is PolygonEntity) return null
            val offset = Vector2D.deserialize(input)
            val width = input.readDouble()
            val height = input.readDouble()
            val resource = client.getResource(input.readUTF())
            if (resource == null || resource !is ImageResource) {
                Conf.logger.warning("Resource '$identifier' does not exist or is not ImageResource")
                return null
            }
            return PolyImageRenderer(ent, resource, offset, width, height)
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

private fun extractXPoints(vertices: Array<Vector2D>): DoubleArray {
    return DoubleArray(vertices.size) { vertices[it].x }
}

private fun extractYPoints(vertices: Array<Vector2D>): DoubleArray {
    return DoubleArray(vertices.size) { vertices[it].y }
}