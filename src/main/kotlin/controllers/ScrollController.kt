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

    /**
     * snaps the camera to the player instantly
     * @param client the client
     */
    abstract fun snap(client: Client)

    /**
     * freezes the camera in place until the snap-function is called
     */
    abstract fun freezeUntilSnap()
}

/**
 * the default ScrollController
 *
 * Tries to keep [focusedEntity] in the middle of the screen. If [focusedEntity] is null if falls back to
 * [thisPlayer][Client.thisPlayer].
 */
open class EntityFocusScreenController : ScrollController() {

    /**
     * the speed at which the camera follows the [focusedEntity]
     */
    var scrollSpeed: Double = 5.0

    /**
     * the controller tries to keep this entity in the middle of the screen
     */
    var focusedEntity: Entity? = null

    /**
     * true if the camera is frozen
     */
    protected var isFrozen: Boolean = false

    open override fun getScroll(client: Client): Vector2D {
        if (isFrozen) return client.scrollOffset
        val focusedEntity = focusedEntity ?: client.thisPlayer ?: return Vector2D()
        val targetPos = focusedEntity.position -
                Vector2D(client.targetCanvas.width / 2, client.targetCanvas.height / 2)
        val offset = (targetPos - client.scrollOffset)
        return client.scrollOffset +
                if (!offset.mag.compare(0.0, scrollSpeed))  offset.getWithMag(scrollSpeed) else offset
    }

    open override fun snap(client: Client) {
        val focusedEntity = focusedEntity ?: client.thisPlayer ?: return
        client.translateScroll(-client.scrollOffset + focusedEntity.position -
                Vector2D(client.targetCanvas.width / 2, client.targetCanvas.height / 2))
        isFrozen = false
    }

    open override fun freezeUntilSnap() {
        isFrozen = true
    }
}