import javafx.scene.input.KeyCode

class KeyPressHelper {

    val keys: MutableList<KeyCode> = mutableListOf()

    fun startPress(key: KeyCode) {
        println("started: ${key.char}")
        keys.add(key)
    }

    fun endPress(key: KeyCode) {
        println("stopped: ${key.char}")
        keys.remove(key)
    }

}