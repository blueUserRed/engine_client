import javafx.scene.image.Image
import networking.HeartBeatMessage
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

object MyClient : Client() {

    @JvmStatic
    fun main(args: Array<String>) {
//        java.awt.Desktop.getDesktop().browse(URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        this.launch(args)
    }

    override fun initialize() {
        val image = Image(Files.newInputStream(Paths.get("res/image.png")), 100.0, 100.0, false, false)
        this.addResource("cobble", ImageResource(image))
        this.connect("127.0.0.1", 3333)
        this.serverConnection!!.addMessageDeserializer("gameJoin") { GameJoinMessage.deserialize(it) }
        this.serverConnection!!.addMessageDeserializer("gameJoinAns") { GameJoinAnswer.deserialize(it) }
        this.serverConnection!!.blockUntilConnected()
        this.serverConnection!!.send(GameJoinMessage())
    }

}