package networking

import Client
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.logging.Level

class ServerConnection(val ip: String, val port: Int, val client: Client) : Thread() {

    private var socket: Socket = Socket(ip , port)
    private var input: DataInputStream = DataInputStream(socket.getInputStream())
    private var output: DataOutputStream = DataOutputStream(socket.getOutputStream())

    private var isInitialized: Boolean = true

    private val messageDeserializers: MutableMap<String, ClientMessageDeserializer> = mutableMapOf()

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

    private fun sendTrailer(output: DataOutputStream) {
        output.writeByte(0xff)
        output.writeByte(0x00)
        output.writeByte(0xff)
        output.writeByte(0x00)
        output.writeByte(0xff)
        output.writeByte(0x00)
        output.writeByte(0x01)
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
        sendTrailer(output)
        output.flush()
    }

    fun isActive() = socket.isClosed

    fun waitUntilConnected() {
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