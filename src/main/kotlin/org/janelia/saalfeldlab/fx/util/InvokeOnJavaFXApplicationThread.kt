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

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.javafx.awaitPulse
import org.janelia.saalfeldlab.fx.ChannelLoop
import java.util.function.Supplier
import kotlin.coroutines.cancellation.CancellationException

class InvokeOnJavaFXApplicationThread {

	companion object {

		private val LOG = KotlinLogging.logger {  }

		private val sharedMainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

		@JvmSynthetic
		operator fun <T> invoke(task: suspend CoroutineScope.() -> T) = sharedMainScope.async(block = task).apply {
			invokeOnCompletion { cause ->
				/* By default, `async` will only throw when `await` is called. Here we throw as soon as it finishes
				* unless it was cancelled. */
				cause?.takeIf { it !is CancellationException }?.let { LOG.error(it) { "Exception in JavaFx Thread coroutine"} }
			}
		}

		@JvmStatic
		fun <T> submit(task: Supplier<T>) = invoke { task.get() }.asCompletableFuture()

		@JvmStatic
		fun invoke(task: Runnable) = invoke { task.run() }.asCompletableFuture()

		/**
		 * [ChannelLoop] with a default delay of [awaitPulse].
		 *
		 * Allows for job submissions that may come very quick, where only that latest job is required to update on the UI thread.
		 *
		 * @param pulses how many pulses to wait between jobs (default 1)
		 */
		@JvmStatic
		fun conflatedPulseLoop(pulses: Int = 1) = ChannelLoop(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), Channel.CONFLATED) {
			repeat(pulses) { awaitPulse() }
		}
	}
}