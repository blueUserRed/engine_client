import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.scene.input.KeyEvent

class MainWindow : Application() {

    companion object {
        lateinit var stage: Stage
            private set
        lateinit var viewManager: ViewManager
            private set
        private var client: Client? = null
    }

    override fun start(primaryStage: Stage?) {
        if (primaryStage == null) return
        viewManager = ViewManager(primaryStage)
        stage = primaryStage
        primaryStage.scene = Scene(Group())
        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED) { client?.keyPressHelper?.startPress(it.code) }
        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED) { client?.keyPressHelper?.endPress(it.code) }
        client?.onInitialize()
        primaryStage.show()
    }

    internal fun create(args: Array<String>, client: Client) {
        MainWindow.client = client
        launch(*args)
    }

    override fun stop() {
        super.stop()
         client?.close()
    }
}

class ViewManager (private val stage: Stage) {

    var currentView: View? = null
        private set

    private val overlays: MutableList<View> = mutableListOf()

    private val mainGroup: Group = Group()

    init {
        stage.scene.root = mainGroup
    }

    fun changeView(view: View) {
        this.currentView = view
        mainGroup.children.remove(currentView?.pane)
        mainGroup.children.add(view.pane)
        view.init(stage)
    }

    fun putOverlay(overlay: View) {
        mainGroup.children.add(overlay.pane)
        overlay.pane.toFront()
        overlays.add(overlay)
        overlay.init(stage)
    }

    fun removeOverlay(overlay: View) {
        mainGroup.children.remove(overlay.pane)
        overlays.remove(overlay)
    }

}

class View (val pane: Pane, val controller: Any?,  val init: (Stage) -> Unit)