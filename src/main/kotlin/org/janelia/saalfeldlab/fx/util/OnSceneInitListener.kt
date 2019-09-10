package org.janelia.saalfeldlab.fx.util

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Scene
import java.util.function.Consumer
import java.util.function.Predicate

class OnSceneInitListener(
        private val sceneCheck: Predicate<Scene?>,
        private val sceneConsumer: Consumer<Scene>) : ChangeListener<Scene?> {

    constructor(sceneCheck: (Scene?) -> Boolean, sceneConsumer: (Scene) -> (Unit)) : this(
            Predicate { sceneCheck(it) },
            Consumer { sceneConsumer(it) })
    constructor(sceneConsumer: (Scene) -> (Unit)) : this(Consumer { sceneConsumer(it) })
    constructor(sceneConsumer: Consumer<Scene>) : this(Predicate { it != null }, sceneConsumer)

    override fun changed(observable: ObservableValue<out Scene?>, oldValue: Scene?, newValue: Scene?) {
        if (sceneCheck.test(newValue)) {
            observable.removeListener(this)
            sceneConsumer.accept(newValue!!)
        }
    }

}
