package networking

import Client
import Entity
import javafx.scene.input.KeyCode
import utils.Utils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.IllegalStateException
import java.security.Key

abstract class Message {

    abstract val identifier: String

    abstract fun execute(client: Client)
    abstract fun serialize(output: DataOutputStream)

    companion object {
        fun registerDeserializers(client: Client) {
            client.serverConnection?.addMessageDeserializer("HeartBeat") {
                HeartBeatMessage(it.readBoolean(), it.readUTF())
            }
            client.serverConnection?.addMessageDeserializer("fullUpdt") {
                FullUpdateMessage.deserialize(it, client)
            }
            client.serverConnection?.addMessageDeserializer("clInfo") {
                ClientInfoMessage.deserialize(it)
            }
        }
    }

}

class HeartBeatMessage(val isResponse: Boolean, val testString: String) : Message() {

    override val identifier: String = "HeartBeat"

    override fun execute(client: Client) {
        if (isResponse) Conf.logger.info("Client received answer to HeartBeat: $testString")
        else client.serverConnection?.send(HeartBeatMessage(true, testString))
    }

    override fun serialize(output: DataOutputStream) {
        output.writeBoolean(isResponse)
        output.writeUTF(testString)
    }

}

class FullUpdateMessage(private val entities: MutableList<Entity>) : Message() {

    override val identifier: String = "fullUpdt"

    override fun execute(client: Client) {
        client.setAllEntities(entities)
        client.tick()
    }

    override fun serialize(output: DataOutputStream) {
        throw IllegalStateException("Cant send FullUpdateMessage from Client")
    }

    companion object {
        fun deserialize(input: DataInputStream, client: Client): FullUpdateMessage? {
            val entities = client.gameDeserializer.deserialize(input, client)
            return if (entities == null) null else FullUpdateMessage(entities)
        }
    }
}

class ClientInfoMessage(private val keys: List<KeyCode>) : Message() {

    override val identifier: String = "clInfo"

    override fun execute(client: Client) {

    }

    override fun serialize(output: DataOutputStream) {
        output.writeInt(keys.size)
        val copy = keys.toMutableList()
        for (key in copy) output.writeInt(key.code)
    }

    companion object {

        fun deserialize(input: DataInputStream): ClientInfoMessage? {
            val keys = mutableListOf<KeyCode>()
            val num = input.readInt()
            for (i in 0 until num) {
                keys.add(getKeyFromCode(input.readInt()) ?: return null)
            }
            return ClientInfoMessage(keys)
        }

        private fun getKeyFromCode(code: Int): KeyCode? {
            for (key in KeyCode.values()) if (key.code == code) return key
            return null
        }

    }

}