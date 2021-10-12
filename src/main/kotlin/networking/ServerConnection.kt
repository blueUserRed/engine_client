package networking

import Client
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.logging.Level

class ServerConnection(val ip: String, val port: Int, val client: Client) : Thread() {

    private lateinit var socket: Socket
    private lateinit var input: DataInputStream
    private lateinit var output: DataOutputStream

    private var isInitialized: Boolean = false

    private val messageDeserializers: MutableMap<String, ClientMessageDeserializer> = mutableMapOf()

    private var stop: Boolean = false

    override fun run() {
        socket = Socket(ip , port)
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())
        isInitialized = true
        while(!stop) {
            try {
                val identifier = input.readUTF()
                val deserializer = messageDeserializers[identifier]
                if (deserializer == null) {
                    Conf.logger.log(Level.WARNING, "Client received message with unknown " +
                            "identifier '$identifier'")
                    continue
                }
                val message = deserializer(input) ?: continue
                message.execute(client)
            } catch (e: IOException) { break }
        }
    }

    fun addMessageDeserializer(identifier: String, deserializer: ClientMessageDeserializer) {
        if (identifier in messageDeserializers.keys) {
            Conf.logger.log(Level.SEVERE, "Failed to add Message-Deserializer with identifier '$identifier' " +
                    "because identifier is already in use!")
            return
        }
        messageDeserializers[identifier] = deserializer
    }

    fun send(message: Message) {
        output.writeInt(client.messageTag ?: 0)
        output.writeUTF(message.identifier)
        message.serialize(output)
        output.flush()
    }

    fun isActive() = socket.isClosed

    fun blockUntilConnected() {
        while(!isInitialized) { }
    }

    fun close() {
        stop = true
        socket.close()
        input.close()
        output.close()
    }

}

typealias ClientMessageDeserializer = (input: DataInputStream) -> Message?