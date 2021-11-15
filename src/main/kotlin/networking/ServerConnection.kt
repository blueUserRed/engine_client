package networking

import Client
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.logging.Level

/**
 * represents a connection to the server
 * @param ip the ip of the server
 * @param port the port on the server
 * @param client the client
 */
class ServerConnection(val ip: String, val port: Int, val client: Client) : Thread() {

    /**
     * the socket to the server
     */
    private var socket: Socket = Socket(ip , port)

    private var input: DataInputStream = DataInputStream(socket.getInputStream())
    private var output: DataOutputStream = DataOutputStream(socket.getOutputStream())

    /**
     * true when the connection is fully initialized
     */
    private var isInitialized: Boolean = true

    /**
     * stores the deserializers added using the [addMessageDeserializer] function
     */
    private val messageDeserializers: MutableMap<String, ClientMessageDeserializer> = mutableMapOf()

    /**
     * true when the connection should close
     */
    private var stop: Boolean = false

    override fun run() {
        while(!stop) {
            try {
                val identifier = input.readUTF()
                val deserializer = messageDeserializers[identifier]
                if (deserializer == null) {
                    Conf.logger.log(Level.WARNING, "Client received message with unknown " +
                            "identifier '$identifier'")
                    resync(input)
                    continue
                }
                val message = deserializer(input)
                if (message == null) {
                    resync(input)
                    continue
                }
                message.execute(client)
                for (i in 1..7) input.readByte() //trailer
            } catch (e: IOException) { break }
        }
    }

    /**
     * Tries to resync the connection after the deserialization of a message failed and the client doesn't know when
     * the next one starts.
     */
    private fun resync(input: DataInputStream) {
        Conf.logger.warning("ServerConnection got desynced, now attempting to resync...")
        while (true) {
            if (input.readByte() != 0xff.toByte()) continue
            if (input.readByte() != 0x00.toByte()) continue
            if (input.readByte() != 0xff.toByte()) continue
            if (input.readByte() != 0x00.toByte()) continue

            val byte = input.readByte()
            if (byte == 0x01.toByte()) return
            if (byte != 0xff.toByte()) continue

            if (input.readByte() != 0x00.toByte()) continue
            if (input.readByte() != 0x01.toByte()) continue
            return
        }
    }

    /**
     * sends a trailer after a message has been sent. In case of an error it can be used to resync the connection.
     * @see resync
     */
    private fun sendTrailer(output: DataOutputStream) {
        output.writeByte(0xff)
        output.writeByte(0x00)
        output.writeByte(0xff)
        output.writeByte(0x00)
        output.writeByte(0xff)
        output.writeByte(0x00)
        output.writeByte(0x01)
    }

    /**
     * adds a serializer for a type of message
     * @param identifier the identifier of the message; should be the class-name
     * @param deserializer the deserializer
     */
    fun addMessageDeserializer(identifier: String, deserializer: ClientMessageDeserializer) {
        if (identifier in messageDeserializers.keys) {
            Conf.logger.log(Level.SEVERE, "Failed to add Message-Deserializer with identifier '$identifier' " +
                    "because identifier is already in use!")
            return
        }
        messageDeserializers[identifier] = deserializer
    }

    /**
    * sends a message to the server
    * @param message the message that should be sent
    */
    fun send(message: Message) {
        output.writeInt(client.messageTag ?: 0)
        output.writeUTF(message.identifier)
        message.serialize(output)
        sendTrailer(output)
        output.flush()
    }

    /**
    * @return true if the connection is active
    */
    fun isActive() = socket.isClosed

    /**
     * waits until a connection to the server is made
     */
    fun waitUntilConnected() {
        while(!isInitialized);
    }

    /**
     * closes the connection
     */
    fun close() {
        stop = true
        socket.close()
        input.close()
        output.close()
    }

}

typealias ClientMessageDeserializer = (input: DataInputStream) -> Message?