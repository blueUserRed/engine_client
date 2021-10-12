import networking.Message
import java.io.DataInputStream
import java.io.DataOutputStream

class GameJoinMessage : Message() {

    override val identifier: String = "gameJoin"

    override fun execute(client: Client) {
    }

    override fun serialize(output: DataOutputStream) {
    }

    companion object {
        fun deserialize(input: DataInputStream): GameJoinMessage {
            return GameJoinMessage()
        }
    }

}

class GameJoinAnswer(val tag: Int) : Message() {

    override val identifier: String = "gameJoinAns"

    override fun execute(client: Client) {
        client.joinGame(tag)
    }

    override fun serialize(output: DataOutputStream) {
        output.writeInt(tag)
    }

    companion object {
        fun deserialize(input: DataInputStream): GameJoinAnswer {
            return GameJoinAnswer(input.readInt())
        }
    }

}