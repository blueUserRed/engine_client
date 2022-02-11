package networking

import Client
import utils.Utils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.math.BigInteger
import java.net.Socket
import java.net.SocketException
import java.security.SecureRandom
import java.util.logging.Level
import kotlin.random.asKotlinRandom

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

    /**
     * stores all callbacks that should be executed when the connection closes
     */
    private val onStopCallbacks: MutableList<() -> Unit> = mutableListOf()

    /**
     * secret key for encrypting messages
     */
    private var key: Long? = null

    override fun run() {
        doKeyExchange()
        if (key == null) {
            Conf.logger.warning("Couldnt perform key Exchange, closing connection")
            close()
            return
        }
        while(!stop) {
            try {
                val identifier = input.readUTF()
                val deserializer = messageDeserializers[identifier]
                if (deserializer == null) {
                    Conf.logger.log(Level.WARNING, "Client received message with unknown " +
                            "identifier '$identifier'")
                    resync()
                    continue
                }
                val message = deserializer(input)
                if (message == null) {
                    resync()
                    continue
                }
                message.execute(client)
                for (i in 1..7) input.readByte() //trailer
            } catch (e: IOException) { break }
        }
        for (callback in onStopCallbacks) callback()
    }

    /**
     * Tries to resync the connection after the deserialization of a message failed and the client doesn't know when
     * the next one starts.
     */
    private fun resync() {
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
    private fun sendTrailer() = synchronized(this) {
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
    fun send(message: Message) = synchronized(this) { try {
        output.writeInt(client.messageTag ?: 0)
        output.writeUTF(message.identifier)
        message.serialize(output)
        sendTrailer()
        output.flush()
    } catch (e: SocketException) { close() } }

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

    /**
     * adds a callback that is executed when the connection is closed
     */
    fun addOnStopCallback(callback: () -> Unit) {
        onStopCallbacks.add(callback)
    }

    /**
     * removes a callback that was previously added using [addOnStopCallback]
     */
    fun removeOnStopCallback(callback: () -> Unit) {
        onStopCallbacks.remove(callback)
    }

    /**
     * performs a diffie-hellman key-exchange
     */
    private fun doKeyExchange() = synchronized(this) {
        val p = Hellman.PRIME
        val b = SecureRandom().asKotlinRandom().nextInt(10_000, p.toInt())
        val g = input.readLong()
        val bigB = g.toBigInteger().pow(b).mod(p.toBigInteger())

        val len = input.readInt()
        val bytesA = ByteArray(len)
        val actualRead = input.read(bytesA, 0, len)
        if (actualRead != len) {
            Conf.logger.warning("KeyExchange with server failed!")
            resync()
            return
        }

        val bytesB = bigB.toByteArray()
        output.writeInt(bytesB.size)
        output.write(bytesB)
        output.flush()

        val bigA = BigInteger(bytesA)
        val k = bigA.pow(b).mod(p.toBigInteger())
        try {
            key = k.longValueExact()
        } catch (e: ArithmeticException) {
            Conf.logger.warning("KeyExchange with server failed!")
            resync()
        }
    }

    private object Hellman {
        /**
         * prime used for the diffie-hellman key-exchange
         */
        const val PRIME: Long = 100_169
    }

    /**
     * encrypts and writes a string to the output stream
     */
    fun writeEncrypted(message: String) {
        output.writeUTF(Utils.AES.encrypt(message, key!!))
    }

    /**
     * reads and decrypts a string from the input stream
     */
    fun readEncrypted(): String {
        return Utils.AES.decrypt(input.readUTF(), key!!)
    }

    private operator fun BigInteger.rem(n: Long): BigInteger = this.mod(BigInteger.valueOf(n))

}

typealias ClientMessageDeserializer = (input: DataInputStream) -> Message?