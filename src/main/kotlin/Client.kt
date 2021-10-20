import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import networking.ClientInfoMessage
import networking.Message
import networking.ServerConnection
import utils.Stopwatch
import java.io.DataInputStream
import java.util.*

abstract class Client {

    var inGame: Boolean = false
        private set

    var serverConnection: ServerConnection? = null

    var entities: MutableList<Entity> = mutableListOf()
        private set

    private var setAllEntitiesCache: MutableList<Entity>? = null
    private var addEntitiesCache: MutableList<Entity> = mutableListOf()

    private var lastFrameCountTime: Long = 0
    var frameRate: Int = 0
        private set
    private var curFrameCount: Int = 0

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

    private val rendererDeserializers: MutableMap<Int, RendererDeserializer> = mutableMapOf()

    fun launch(args: Array<String>) {
        this.args = args
        Renderer.registerRendererDeserializers(this)
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

    fun getEntityDeserializer(identifier: Int): EntityDeserializer? = entityDeserializers[identifier]

    fun addRendererDeserializer(identifier: Int, rendererDeserializer: RendererDeserializer) {
        rendererDeserializers[identifier] = rendererDeserializer
    }

    fun getRendererDeserializer(identifier: Int): RendererDeserializer? = rendererDeserializers[identifier]

    fun setAllEntities(entities: MutableList<Entity>) {
        this.setAllEntitiesCache = entities
    }

    internal fun tick() {
        curFrameCount++
        val message = ClientInfoMessage(keyPressHelper.keys)
        serverConnection?.send(message)
        if (setAllEntitiesCache != null) entities = setAllEntitiesCache as MutableList<Entity>
        for (entity in addEntitiesCache) entities.add(entity)
        addEntitiesCache.clear()
        render()
    }

    private fun render() {
        if (lastFrameCountTime + 1000 <= System.currentTimeMillis()) {
            lastFrameCountTime = System.currentTimeMillis()
            frameRate = curFrameCount
            println(frameRate)
            curFrameCount = 0
        }
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

    fun getEntity(uuid: UUID): Entity? {
        for (ent in entities) if (ent.uuid == uuid) return ent
        return null
    }

    fun addEntity(entity: Entity) {
        addEntitiesCache.add(entity)
    }

    abstract fun initialize()

}

typealias EntityDeserializer = (input: DataInputStream) -> Entity?
typealias RendererDeserializer = (input: DataInputStream, ent: Entity) -> Renderer?