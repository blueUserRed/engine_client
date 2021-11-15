package controllers

import Client
import Entity
import utils.Vector2D
import utils.compare

/**
 * controls the camera position
 */
abstract class ScrollController {

    /**
     * get the new scroll-offset
     * @param client the client
     * @return the offset of the camera from 0,0
     */
    abstract fun getScroll(client: Client): Vector2D
}

/**
 * the default ScrollController
 *
 * Tries to keep [focusedEntity] in the middle of the screen. If [focusedEntity] is null if falls back to
 * [thisPlayer][Client.thisPlayer].
 */
class EntityFocusScreenController : ScrollController() {

    /**
     * the speed at which the camera follows the [focusedEntity]
     */
    var scrollSpeed: Double = 5.0

    /**
     * the controller tries to keep this entity in the middle of the screen
     */
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