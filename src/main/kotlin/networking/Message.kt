package networking

import Client
import Entity
import javafx.scene.input.KeyCode
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.IllegalStateException

/**
 * a message that can be sent over the network
 */
abstract class Message {

    /**
     * the identifier that is used to uniquely identify a type of message. Should be the class-name
     */
    abstract val identifier: String

    /**
     * this function is called when this message is received
     * @param client the client
     */
    abstract fun execute(client: Client)

    /**
     * serializes the message so it can be sent over the network
     */
    abstract fun serialize(output: DataOutputStream)

    companion object {

        /**
         * registers the deserializers for build-in messages
         */
        internal fun registerDeserializers(client: Client) {
            client.serverConnection?.addMessageDeserializer("HeartBeat") {
                HeartBeatMessage(it.readBoolean(), it.readUTF())
            }
            client.serverConnection?.addMessageDeserializer("fullUpdt") {
                FullUpdateMessage.deserialize(it, client)
            }
            client.serverConnection?.addMessageDeserializer("clInfo") {
                ClientInfoMessage.deserialize(it)
            }
            client.serverConnection?.addMessageDeserializer("incUpdt") {
                IncrementalUpdateMessage.deserialize(it, client)
            }
        }
    }

}

/**
 * a Heartbeatmessage; if it is received the client will send a response with the same testString
 * @param isResponse stores if the message is an initial request or a response. This used to decide whether to send
 * an answer or not
 */
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

/**
 * a message containing a completely serialized game
 */
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

        /**
         * deserializes the message
         */
        fun deserialize(input: DataInputStream, client: Client): FullUpdateMessage? {
            val entities = client.gameDeserializer.deserialize(input, client)
            return if (entities == null) null else FullUpdateMessage(entities)
        }
    }
}

/**
 * is sent from the client to the server and contains information from the client, like keyInputs
 * @param keys the currently pressed keys
 */
class ClientInfoMessage(private val keys: Set<KeyCode>) : Message() {

    override val identifier: String = "clInfo"

    override fun execute(client: Client) {

    }

    override fun serialize(output: DataOutputStream) {
        synchronized(keys) {
            output.writeInt(keys.size)
            for (key in keys) output.writeInt(key.code)
        }
    }

    companion object {

        /**
         * deserializes the message
         */
        fun deserialize(input: DataInputStream): ClientInfoMessage? {
            val keys = mutableSetOf<KeyCode>()
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

/**
 * a message to update the state of the game
 */
class IncrementalUpdateMessage : Message() {

    override val identifier: String = "incUpdt"

    override fun execute(client: Client) {
        client.tick()
    }

    override fun serialize(output: DataOutputStream) {
    }

    companion object {

        /**
         * deserializes the message
         */
        fun deserialize(input: DataInputStream, client: Client): IncrementalUpdateMessage? {
            val success = client.gameDeserializer.deserializeInc(input, client)
            return if (success) IncrementalUpdateMessage() else null
        }
    }

}