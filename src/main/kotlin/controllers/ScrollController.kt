package controllers

import Client
import Entity
import utils.Vector2D
import utils.compare

abstract class ScrollController {
    abstract fun getScroll(client: Client): Vector2D
}

class EntityFocusScreenController : ScrollController() {

    var scrollSpeed: Double = 5.0
    var focusedEntity: Entity? = null

    override fun getScroll(client: Client): Vector2D {
        val focusedEntity = focusedEntity ?: client.thisPlayer ?: return Vector2D()
        val targetPos = focusedEntity.position -
                Vector2D(client.targetCanvas.width / 2, client.targetCanvas.height / 2)
        val offset = (targetPos - client.scrollOffset)
        return client.scrollOffset +
                if (!offset.mag.compare(0.0, scrollSpeed))  offset.getWithMag(scrollSpeed) else offset
    }

}