import controllers.EntityFocusScreenController
import controllers.GameDeserializer
import controllers.MainGameDeserializer
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

/**
 * the client
 */
abstract class Client {

    /**
     * true if the client is in a game
     */
    var inGame: Boolean = false
        private set

    /**
     * the serverConnection; null if there is none
     */
    var serverConnection: ServerConnection? = null

    /**
     * a list containing all entities
     */
    var entities: MutableList<Entity> = mutableListOf()
        private set

    /**
     * cache to prevent ConcurrentModificationException
     */
    private var setAllEntitiesCache: MutableList<Entity>? = null

    /**
     * cache to prevent ConcurrentModificationException
     */
    private var addEntitiesCache: MutableList<Entity> = mutableListOf()

    /**
     * time at which [curFrameCount] was last reset
     *
     * _used for counting the framerate_
     */
    private var lastFrameCountTime: Long = 0

    /**
     * the current frameRate
     */
    var frameRate: Int = 0
        private set

    /**
     * the current frame-count
     *
     * _used for counting the framerate_
     */
    private var curFrameCount: Int = 0

    /**
     * used to keep track of currently pressed keys
     */
    val keyPressHelper: KeyPressHelper = KeyPressHelper()

    /**
     * the canvas to which the game is rendered
     *
     * _setting this will automatically set [gc]
     */
    var targetCanvas: Canvas = Canvas()
        set(value) {
            field = value
            gc = value.graphicsContext2D
        }

    /**
     * the graphicsContext used for rendering
     */
    var gc: GraphicsContext = targetCanvas.graphicsContext2D
        private set

    /**
     * the offset of the camera from 0,0
     */
    var scrollOffset: Vector2D = Vector2D()
        private set

    /**
     * the tag of the messages sent to the server
     *
     * _Note: the server uses these to identify to which game the message belongs. is 0 if the message goes to the server
     * itself._
     */
    var messageTag: Int? = null

    /**
     * the view the game is rendered to
     */
    open val gameView = View(Pane(targetCanvas), null) {
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

    /**
     * the program arguments
     */
    lateinit var args: Array<String>
        private set

    /**
     * the gameDeserializer used to deserialize the game
     */
    var gameDeserializer: GameDeserializer = MainGameDeserializer()

    /**
     * the scrollController used to control the position of the camera
     */
    var scrollController: ScrollController = EntityFocusScreenController()

    /**
     * the entity that corresponds to the player of this client; null if there is none
     */
    var thisPlayer: Entity? = null
        private set

    /**
     * list containing deserializers added using the [addEntityDeserializer] function
     */
    private val entityDeserializers: MutableMap<Int, EntityDeserializer> = mutableMapOf()

    /**
     * list containing deserializers added using the [addRendererDeserializer] function
     */
    private val rendererDeserializers: MutableMap<Int, RendererDeserializer> = mutableMapOf()

    /**
     * list containing all resources
     */
    private val resources: MutableMap<String, Resource> = mutableMapOf()

    /**
     * launches the client
     */
    fun launch(args: Array<String>) {
        this.args = args
        Renderer.registerRendererDeserializers(this)
        GameDeserializer.registerDeserializers(this)
        MainWindow().create(args, this)
    }

    /**
     * used internally for knowing when the [MainWindow] is initialized
     */
    internal fun onInitialize() {
        MainWindow.viewManager.changeView(gameView)
        initialize()
    }

    /**
     * attempts to connect to a server
     * @param ip the ip of the server
     * @param port the port on the server
     */
    fun connect(ip: String, port: Int) {
        messageTag = 0
        this.serverConnection = ServerConnection(ip, port, this)
        Message.registerDeserializers(this)
        serverConnection!!.start()
    }

    /**
     * sets the game-tag
     * @see messageTag
     */
    fun joinGame(tag: Int) {
        messageTag = tag
        entities = mutableListOf()
        inGame = true
    }

    /**
     * resets the game tag to 0
     * @see messageTag
     */
    fun leaveGame() {
        messageTag = 0
    }

    /**
     * adds a new deserializer for a custom entity
     * @param identifier the identifier used to uniquely identify the entity
     * @param entityDeserializer the deserializer
     */
    fun addEntityDeserializer(identifier: Int, entityDeserializer: EntityDeserializer) {
        entityDeserializers[identifier] = entityDeserializer
    }

    /**
     * gets the deserializer for an entity
     * @param identifier the identifier used to uniquely identify the entity
     * @return the deserializer for the entity; null if there is deserializer registered for the identifier
     */
    fun getEntityDeserializer(identifier: Int): EntityDeserializer? = entityDeserializers[identifier]

    /**
     * adds a new deserializer for a custom renderer
     * @param identifier the identifier used to uniquely identify the renderer
     * @param rendererDeserializer the deserializer
     */
    fun addRendererDeserializer(identifier: Int, rendererDeserializer: RendererDeserializer) {
        rendererDeserializers[identifier] = rendererDeserializer
    }

    /**
     * gets the deserializer for a renderer
     * @param identifier the identifier used to uniquely identify the renderer
     * @return the deserializer for the renderer; null if there is deserializer registered for the identifier
     */
    fun getRendererDeserializer(identifier: Int): RendererDeserializer? = rendererDeserializers[identifier]

    /**
     * sets all entities in the game
     */
    fun setAllEntities(entities: MutableList<Entity>) {
//        this.setAllEntitiesCache = entities
        this.entities = entities
        thisPlayer = null
        for (ent in entities) if (ent.isThisPlayer) {
            if (thisPlayer == null) thisPlayer = ent
            else Conf.logger.warning("There are two thisPlayer-Entities currently in the game!")
        }
    }

    /**
     * updates everything and renders
     */
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

    /**
     * renders the game to the [targetCanvas] (later)
     *
     * also counts frames
     */
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

    /**
     * closes the client
     */
    fun close() {
        leaveGame()
        serverConnection?.close()
    }

    /**
     * gets an entity based on the uuid
     * @param uuid the uuid
     * @return the entity that corresponds to the uuid; null if there is none
     */
    fun getEntity(uuid: UUID): Entity? {
        for (ent in entities) if (ent.uuid == uuid) return ent
        return null
    }

    /**
     * adds an Entity to the game
     * @param entity the entity that should be added
     */
    fun addEntity(entity: Entity) {
        entities.add(entity)
        if (entity.isThisPlayer) {
            if (thisPlayer == null) thisPlayer = entity
            else Conf.logger.warning("There are two thisPlayer-Entities currently in the game!")
        }
    }

    /**
     * adds a new resource
     * @param identifier the identifier that uniquely identifies the resource
     * @param resource the resource
     */
    fun addResource(identifier: String, resource: Resource) {
        resources[identifier] = resource
    }

    /**
     * gets a resource
     * @param identifier the identifier that uniquely identifies the resource
     * @return the resource that corresponds to the entity; null if there is none
     */
    fun getResource(identifier: String): Resource? = resources[identifier]

    /**
     * scrolls to a certain spot in the world
     * @param scroll the offset of the camera from 0,0
     */
    fun scrollTo(scroll: Vector2D) {
        scrollOffset = scroll
    }

    /**
     * offsets the camera from the current position
     * @param translation the translation from the current position
     */
    fun translateScroll(translation: Vector2D) {
        scrollOffset += translation
    }

    /**
     * called after the client has been initialized
     */
    abstract fun initialize()

}

typealias EntityDeserializer = (input: DataInputStream) -> Entity?
typealias RendererDeserializer = (input: DataInputStream, ent: Entity, client: Client) -> Renderer?