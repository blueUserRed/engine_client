import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import utils.Vector2D

interface Renderer {

    fun render(gc: GraphicsContext, client: Client)

    companion object {

        fun gameCoordsToScreenCoords(gameCoords: Vector2D, client: Client): Vector2D {
            return Vector2D(gameCoords.x, client.targetCanvas.height - gameCoords.y - 30.0)
        }

        fun gameCoordsToScreenCoords(gameCoords: Array<Vector2D>, client: Client): Array<Vector2D> {
            return Array(gameCoords.size) { gameCoordsToScreenCoords(gameCoords[it], client) }
        }

    }

}

class PolygonRenderer (val ent: PolygonEntity) : Renderer {

    var color: Color = Color.valueOf("#8800cc")

    override fun render(gc: GraphicsContext, client: Client) {
        val verts = Renderer.gameCoordsToScreenCoords(this.ent.verticesAbsolute, client)
        gc.fill = this.color
        gc.fillPolygon(extractXPoints(verts), extractYPoints(verts), verts.size)

        gc.fill = Color.valueOf("#00ff00")
        val centroid = Renderer.gameCoordsToScreenCoords(this.ent.position, client)
        gc.fillOval(centroid.x - 5, centroid.y - 5, 10.0, 10.0)
    }

}

class VectorRenderer(var vec: Vector2D, var position: Vector2D) : Renderer {

    override fun render(gc: GraphicsContext, client: Client) {
        val p1 = Renderer.gameCoordsToScreenCoords(position, client)
        val p2 = Renderer.gameCoordsToScreenCoords(position + vec, client)
        gc.stroke = Color.valueOf("#ff0000")
        gc.strokeLine(p1.x, p1.y, p2.x, p2.y)
        gc.fill = Color.valueOf("#0000ff")
        gc.fillOval(p2.x - 4, p2.y - 4, 8.0, 8.0)
    }

}

class PointRenderer(var point: Vector2D, var radius: Double) : Renderer {

    var color: Color = Color.valueOf("#0000ff")

    override fun render(gc: GraphicsContext, client: Client) {
        gc.fill = this.color
        val sPoint = Renderer.gameCoordsToScreenCoords(point, client)
        gc.fillOval(sPoint.x - radius / 2, sPoint.y - radius / 2, radius, radius)
    }

}

private fun extractXPoints(vertices: Array<Vector2D>): DoubleArray {
    return DoubleArray(vertices.size) { vertices[it].x }
}

private fun extractYPoints(vertices: Array<Vector2D>): DoubleArray {
    return DoubleArray(vertices.size) { vertices[it].y }
}