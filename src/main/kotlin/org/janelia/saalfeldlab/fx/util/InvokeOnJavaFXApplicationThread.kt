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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.javafx.awaitPulse
import org.janelia.saalfeldlab.fx.ChannelLoop
import java.util.function.Supplier

class InvokeOnJavaFXApplicationThread {

	companion object {

		private val sharedMainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

		@JvmStatic
		operator fun <T> invoke(task: suspend CoroutineScope.() -> T) = sharedMainScope.async(block = task)

		@JvmStatic
		operator fun <T> invoke(task: Supplier<T>) = invoke<T> { task.get() }.asCompletableFuture()

		@JvmStatic
		operator fun invoke(task: Runnable) = invoke(Supplier<Unit> { task.run() })

		@JvmStatic
		@Throws(InterruptedException::class)
		fun <T> invokeAndWait(task: suspend CoroutineScope.() -> T) = runBlocking {
			sharedMainScope.launch { task() }.join()
		}

		/**
		 * [ChannelLoop] with a default delay of [awaitPulse].
		 *
		 * Allows for job submissions that may come very quick, where only that latest job is required to update on the UI thread.
		 */
		@JvmStatic
		fun conflatedPulseLoop() = ChannelLoop(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), Channel.CONFLATED) { awaitPulse() }
	}
}