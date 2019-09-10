package org.janelia.saalfeldlab.fx.util

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.stage.Stage
import javafx.stage.Window
import java.util.function.Consumer
import java.util.function.Predicate

class OnWindowInitListener(
        private val windowCheck: Predicate<Window?>,
        private val windowConsumer: Consumer<Window>) : ChangeListener<Window?> {

    constructor(windowCheck: (Window?) -> Boolean, windowConsumer: (Window) -> Unit) : this(
            Predicate { windowCheck(it) },
            Consumer { windowConsumer(it) })

    constructor(windowConsumer: (Window) -> Unit) : this( Consumer { windowConsumer(it) })
    constructor(windowConsumer: Consumer<Window>) : this( Predicate { it != null }, windowConsumer)

    override fun changed(observable: ObservableValue<out Window?>, oldValue: Window?, newValue: Window?) {
        if (this.windowCheck.test(newValue)) {
            observable.removeListener(this)
            this.windowConsumer.accept(newValue!!)
        }
    }

    companion object {

        fun doOnStageInit(stageConsumer: Consumer<Stage>): OnSceneInitListener {
            val onWindowInit = OnWindowInitListener(
                    { window -> window != null && window is Stage },
                    { window -> stageConsumer.accept(window as Stage) }
            )
            return OnSceneInitListener { scene -> scene.windowProperty().addListener(onWindowInit) }
        }
    }

}
