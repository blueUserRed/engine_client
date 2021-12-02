import javafx.scene.input.KeyCode

/**
 * very helpful with key-presses
 */
class KeyPressHelper {

    /**
     * all keys that are currently pressed
     */
    val keys: MutableSet<KeyCode> = mutableSetOf()

    /**
     * starts the press of a key
     */
    fun startPress(key: KeyCode) = synchronized(keys) {
        keys.add(key)
    }

    /**
     * stops a keyPress
     */
    fun endPress(key: KeyCode) = synchronized(keys) {
        keys.remove(key)
    }

}