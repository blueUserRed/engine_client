import networking.HeartBeatMessage

object MyClient : Client() {

    @JvmStatic
    fun main(args: Array<String>) {
        this.launch(args)
    }

    override fun initialize() {
        this.connect("127.0.0.1", 3333)
        this.serverConnection!!.addMessageDeserializer("gameJoin") { GameJoinMessage.deserialize(it) }
        this.serverConnection!!.addMessageDeserializer("gameJoinAns") { GameJoinAnswer.deserialize(it) }
        this.serverConnection!!.blockUntilConnected()
        this.serverConnection!!.send(GameJoinMessage())
    }

}