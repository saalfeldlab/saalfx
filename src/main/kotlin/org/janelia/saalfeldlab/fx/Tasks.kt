package org.janelia.saalfeldlab.fx

import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.concurrent.Task
import javafx.concurrent.Worker.State.*
import kotlinx.coroutines.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier

class Tasks private constructor() {

	companion object {
		@JvmSynthetic
		fun <T> createTask(call: suspend () -> T): UtilityTask<T> {
			return UtilityTask(CoroutineScope(Dispatchers.Default)) { call() }
		}

		@JvmStatic
		fun <T> createTask(call: Supplier<T>): UtilityTask<T> {
			return createTask { call.get() }
		}

		@JvmStatic
		fun createTask(call: Runnable): UtilityTask<Unit> {
			return createTask { call.run() }
		}
	}
}


@Suppress("OPT_IN_USAGE")
class UtilityTask<V> internal constructor(
	private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
	private val block: suspend CoroutineScope.() -> V
) : Deferred<V> by scope.async(block = block) {

	companion object {
		private val LOG = KotlinLogging.logger { }
	}

	@JvmSynthetic
	fun onSuccess(onSuccess: (V) -> Unit) = apply {
		invokeOnCompletion { cause ->
			cause ?: onSuccess(getCompleted())
		}
	}

	fun onSuccess(onSuccess: Consumer<V>) = apply {
		onSuccess { onSuccess.accept(it) }
	}

	@JvmSynthetic
	fun onCancelled(onCancelled: (CancellationException) -> Unit) = apply {
		invokeOnCompletion { cause ->
			(cause as? CancellationException)?.let { onCancelled(it) }
		}
	}

	fun onCancelled(onCancelled: Consumer<CancellationException>) = apply {
		onCancelled { onCancelled.accept(it) }
	}

	@JvmSynthetic
	fun onFailed(onFailed: (Throwable) -> Unit) = apply {
		invokeOnCompletion { cause ->
			when (cause) {
				null, is CancellationException -> Unit
				else -> onFailed(cause)
			}
		}
	}

	fun onFailed(onFailed: Consumer<Throwable>) = apply {
		onFailed { onFailed.accept(it) }
	}


	@JvmSynthetic
	fun onEnd(onEnd: (V?, Throwable?) -> Unit) = apply {
		invokeOnCompletion { cause ->
			val (value, error) = cause?.let { null to it } ?: (getCompleted() to null)
			onEnd(value, error)
		}
	}

	fun onEnd(onEnd: BiConsumer<V?, Throwable?>) = apply {
		onEnd { result, cause -> onEnd.accept(result, cause) }
	}

	fun get() = runBlocking { await() }

	fun wait() = apply {
		runBlocking { join() }
	}
}
