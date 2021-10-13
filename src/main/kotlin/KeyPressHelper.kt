import javafx.scene.input.KeyCode

class KeyPressHelper {

    val keys: MutableSet<KeyCode> = mutableSetOf()

    fun startPress(key: KeyCode) {
        println("started: ${key.char}")
        keys.add(key)
    }

    fun endPress(key: KeyCode) {
        println("stopped: ${key.char}")
        keys.remove(key)
    }

}