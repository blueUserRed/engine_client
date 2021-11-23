import javafx.scene.input.KeyCode

class KeyPressHelper {

    val keys: MutableSet<KeyCode> = mutableSetOf()

    fun startPress(key: KeyCode) = synchronized(keys) {
        keys.add(key)
    }

    fun endPress(key: KeyCode) = synchronized(keys) {
        keys.remove(key)
    }

}