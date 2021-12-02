import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.scene.input.KeyEvent

/**
 * This class represents the MainWindow of the game
 */
class MainWindow internal constructor(): Application() {

    companion object {

        /**
         * the stage of the main-Window
         */
        lateinit var stage: Stage
            private set

        /**
         * used to manage the views, show different menus, show overlays, etc.
         */
        lateinit var viewManager: ViewManager
            private set

        /**
         * the client
         */
        private var client: Client? = null
    }

    /**
     * the javafx-start-method
     */
    override fun start(primaryStage: Stage?) {
        if (primaryStage == null) return
        stage = primaryStage
        primaryStage.scene = Scene(Group())
        viewManager = ViewManager(primaryStage)
        client?.onInitialize()
        primaryStage.show()
    }

    /**
     * creates and shows the window
     */
    internal fun create(args: Array<String>, client: Client) {
        MainWindow.client = client
        launch(*args)
    }

    /**
     * stops the window
     */
    override fun stop() {
        super.stop()
        client?.close()
    }
}

/**
 * used to manage views, overlays, etc.
 * @param stage the stage of the mainWindow
 * @property stage the stage of the mainWindow
 */
class ViewManager internal constructor (private val stage: Stage) {

    /**
     * the view that is currently being shown; null if none
     */
    var currentView: View? = null
        private set

    /**
     * list of all overlays that are overlaid over the [currentView]
     */
    private val overlays: MutableList<View> = mutableListOf()

    /**
     * the group in which the [currentView] and [overlays] are put
     */
    private val mainGroup: Group = stage.scene.root as Group

    /**
     * changes the view to another
     */
    fun changeView(view: View) {
        currentView?.finish?.invoke(stage)
        mainGroup.children.remove(currentView?.pane)
        mainGroup.children.add(view.pane)
        currentView = view
        view.init(stage)
    }

    /**
     * adds a new overlay
     */
    fun putOverlay(overlay: View) {
        mainGroup.children.add(overlay.pane)
        overlay.pane.toFront()
        overlays.add(overlay)
        overlay.init(stage)
    }

    /**
     * removes an overlay
     */
    fun removeOverlay(overlay: View) {
        overlay.finish?.invoke(stage)
        mainGroup.children.remove(overlay.pane)
        overlays.remove(overlay)
    }

}

/**
 * a View that can be displayed on the mainWindow
 * @param pane the pane containing the nodes of the view
 * @param controller the fx:controller of the pane; null if none
 * @param init a callback that is executed every time the view is shown or put as an overlay
 */
open class View (open val pane: Pane, open val controller: Any?, open val init: (Stage) -> Unit) {

    /**
     * a callback that is executed when the view is removed
     */
    var finish: ((Stage) -> Unit)? = null
}