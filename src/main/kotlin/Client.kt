import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import networking.ClientInfoMessage
import networking.Message
import networking.ServerConnection
import java.io.DataInputStream

abstract class Client {

    var inGame: Boolean = false
        private set

    var serverConnection: ServerConnection? = null

    var entities: MutableList<Entity> = mutableListOf()
        private set

    private var setEntitiesCache: MutableList<Entity>? = null

    val keyPressHelper: KeyPressHelper = KeyPressHelper()

    var targetCanvas: Canvas = Canvas()
        set(value) {
            field = value
            gc = value.graphicsContext2D
        }

    private var gc: GraphicsContext = targetCanvas.graphicsContext2D

    var messageTag: Int? = null

    val gameView = View(Pane(targetCanvas)) {
        it.width = 800.0
        it.height = 600.0
        targetCanvas.width = 800.0
        targetCanvas.height = 600.0
        it.resizableProperty().value = false //TODO: fix
    }

    lateinit var args: Array<String>
        private set

    var gameDeserializer: GameDeserializer = MainGameDeserializer()
    private val entityDeserializers: MutableMap<Int, EntityDeserializer> = mutableMapOf()

    fun launch(args: Array<String>) {
        this.args = args
        GameDeserializer.registerDeserializers(this)
        MainWindow().create(args, this)
    }

    internal fun onInitialize() {
        MainWindow.viewManager.changeView(gameView)
        initialize()
    }

    fun connect(ip: String, port: Int) {
        messageTag = 0
        this.serverConnection = ServerConnection(ip, port, this)
        Message.registerDeserializers(this)
        serverConnection!!.start()
    }

    fun joinGame(tag: Int) {
        messageTag = tag
        entities = mutableListOf()
        inGame = true
    }

    fun leaveGame() {
        messageTag = 0
    }

    fun addEntityDeserializer(identifier: Int, entityDeserializer: EntityDeserializer) {
        entityDeserializers[identifier] = entityDeserializer
    }

    fun getEntityDeserializer(identifier: Int): EntityDeserializer? {
        return entityDeserializers[identifier]
    }

    fun setAllEntities(entities: MutableList<Entity>) {
        this.setEntitiesCache = entities
    }

    internal fun tick() {
        val message = ClientInfoMessage(keyPressHelper.keys)
        serverConnection?.send(message)
        if (setEntitiesCache != null) entities = setEntitiesCache as MutableList<Entity>
        render()
    }

    private fun render() {
        gc.fill = Color.valueOf("#000000")
        gc.fillRect(0.0, 0.0, targetCanvas.width, targetCanvas.height)
        for (entity in entities) {
            gc.save()
            entity.render(gc, this)
            gc.restore()
        }
    }

    fun close() {
        leaveGame()
        serverConnection?.close()
    }

    abstract fun initialize()

}

typealias EntityDeserializer = (DataInputStream) -> Entity?