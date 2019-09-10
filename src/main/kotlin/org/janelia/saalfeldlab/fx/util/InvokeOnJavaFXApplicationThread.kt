/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

class InvokeOnJavaFXApplicationThread {

    companion object {

        operator fun invoke(task: () -> Unit) = invoke(Runnable { task() })

        @JvmStatic
        operator fun invoke(task: Runnable) {
            if (Platform.isFxApplicationThread())
                task.run()
            else
                Platform.runLater(task)
        }

        @Throws(InterruptedException::class)
        fun invokeAndWait(task: () -> Unit) = invokeAndWait(Runnable { task() })

        @JvmStatic
        @Throws(InterruptedException::class)
        fun invokeAndWait(task: Runnable) {
            val latch = CountDownLatch(1)
            val countDownTask = Runnable {
                task.run()
                latch.countDown()
            }
            invoke(countDownTask)
            synchronized(latch) {
                latch.await()
            }
        }

        fun invokeAndWait(task: () -> Unit, exceptionHandler: (InterruptedException) -> Unit) = invokeAndWait(
                Runnable { task() },
                Consumer { exceptionHandler(it) })

        @JvmStatic
        fun invokeAndWait(task: Runnable, exceptionHandler: Consumer<InterruptedException>) {
            try {
                invokeAndWait(task)
            } catch (e: InterruptedException) {
                exceptionHandler.accept(e)
            }
        }
    }
}
