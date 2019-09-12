/*-
 * #%L
 * Saalfeld lab JavaFX tools and extensions
 * %%
 * Copyright (C) 2019 Philipp Hanslovsky, Stephan Saalfeld
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
