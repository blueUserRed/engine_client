import javafx.application.Application
import javafx.application.Platform
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
        stage = primaryStage
        primaryStage.scene = Scene(Group())
        viewManager = ViewManager(primaryStage)
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

class ViewManager internal constructor (private val stage: Stage) {

    var currentView: View? = null
        private set

    private val overlays: MutableList<View> = mutableListOf()

    private val mainGroup: Group = stage.scene.root as Group

    fun changeView(view: View) {
        currentView?.finish?.invoke(stage)
        mainGroup.children.remove(currentView?.pane)
        mainGroup.children.add(view.pane)
        currentView = view
        view.init(stage)
    }

    fun putOverlay(overlay: View) {
        mainGroup.children.add(overlay.pane)
        overlay.pane.toFront()
        overlays.add(overlay)
        overlay.init(stage)
    }

    fun removeOverlay(overlay: View) {
        overlay.finish?.invoke(stage)
        mainGroup.children.remove(overlay.pane)
        overlays.remove(overlay)
    }

}

open class View (open val pane: Pane, open val controller: Any?, open val init: (Stage) -> Unit) {
    var finish: ((Stage) -> Unit)? = null
}