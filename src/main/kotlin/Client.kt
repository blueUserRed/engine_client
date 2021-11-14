import controllers.EntityFocusScreenController
import controllers.ScrollController
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import networking.ClientInfoMessage
import networking.Message
import networking.ServerConnection
import utils.Vector2D
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

    var scrollOffset: Vector2D = Vector2D()
        private set

    var messageTag: Int? = null

    val gameView = View(Pane(targetCanvas), null) {
        it.width = 800.0
        it.height = 600.0
        targetCanvas.width = 800.0
        targetCanvas.height = 600.0
        it.widthProperty().addListener { _, _, new ->
            targetCanvas.width = new.toDouble()
        }
        it.heightProperty().addListener { _, _, new ->
            targetCanvas.height = new.toDouble()
        }
    }

    lateinit var args: Array<String>
        private set

    var gameDeserializer: GameDeserializer = MainGameDeserializer()

    var scrollController: ScrollController = EntityFocusScreenController()

    var thisPlayer: Entity? = null
        private set

    private val entityDeserializers: MutableMap<Int, EntityDeserializer> = mutableMapOf()

    private val rendererDeserializers: MutableMap<Int, RendererDeserializer> = mutableMapOf()

    private val resources: MutableMap<String, Resource> = mutableMapOf()

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
//        this.setAllEntitiesCache = entities
        this.entities = entities
        thisPlayer = null
        for (ent in entities) if (ent.isThisPlayer) {
            if (thisPlayer == null) thisPlayer = ent
            else Conf.logger.warning("There are two thisPlayer-Entities currently in the game!")
        }
    }

    internal fun tick() {
        curFrameCount++
        val message = ClientInfoMessage(keyPressHelper.keys)
        serverConnection?.send(message)
        if (setAllEntitiesCache != null) entities = setAllEntitiesCache as MutableList<Entity>
        for (entity in addEntitiesCache) entities.add(entity)
        addEntitiesCache.clear()
        scrollOffset = scrollController.getScroll(this)
        render()
    }

    private fun render() = Platform.runLater {
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
        entities.add(entity)
        if (entity.isThisPlayer) {
            if (thisPlayer == null) thisPlayer = entity
            else Conf.logger.warning("There are two thisPlayer-Entities currently in the game!")
        }
    }

    fun addResource(identifier: String, resource: Resource) {
        resources[identifier] = resource
    }

    fun getResource(identifier: String): Resource? = resources[identifier]

    fun scrollTo(scroll: Vector2D) {
        scrollOffset = scroll
    }

    fun translateScroll(translation: Vector2D) {
        scrollOffset += translation
    }

    abstract fun initialize()

}

typealias EntityDeserializer = (input: DataInputStream) -> Entity?
typealias RendererDeserializer = (input: DataInputStream, ent: Entity, client: Client) -> Renderer?