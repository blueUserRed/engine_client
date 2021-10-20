import javafx.scene.input.KeyCode

class KeyPressHelper {

    val keys: MutableSet<KeyCode> = mutableSetOf()

    fun startPress(key: KeyCode) {
        keys.add(key)
    }

    fun endPress(key: KeyCode) {
        keys.remove(key)
    }

}