package org.janelia.saalfeldlab.fx

import com.google.common.util.concurrent.ThreadFactoryBuilder
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.concurrent.Worker.State.*
import javafx.concurrent.WorkerStateEvent
import javafx.event.EventHandler
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function

/**
 * Utility class for workign with [UtilityTask]
 */
class Tasks private constructor() {

	companion object {
		@JvmSynthetic
		fun <T> createTask(call: (UtilityTask<T>) -> T): UtilityTask<T> {
			return UtilityTask(call)
		}

		@JvmStatic
		fun <T> createTask(call: Function<UtilityTask<T>, T>): UtilityTask<T> {
			return createTask { call.apply(it) }
		}

		@JvmStatic
		fun createTask(call: Consumer<UtilityTask<Unit>>): UtilityTask<Unit> {
			return createTask { call.accept(it) }
		}
	}
}

private val THREAD_FACTORY: ThreadFactory = ThreadFactoryBuilder()
	.setDaemon(true)
	.setNameFormat("task-thread-%d")
	.build()

private val TASK_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1, THREAD_FACTORY)

/**
 * Convenience wrapper class around [Task]
 *
 * @param V type of super class Task<V>
 * @property onCall called during [Task.call], but wrapped with exception handling
 * @constructor Create empty Utility task
 */
class UtilityTask<V>(private val onCall: (UtilityTask<V>) -> V) : Task<V>() {

	private var executorService : ExecutorService = TASK_SERVICE
	private var onFailedSet = false

	companion object {
		private val LOG = LoggerFactory.getLogger(UtilityTask::class.java)
	}

	override fun call(): V? {
		try {
			/* If no `onEnd/onFail` has been set, then we should listen for thrown exceptions and throw them */
			if (!onFailedSet) setDefaultOnFailed()
			return onCall(this)
		} catch (e: Exception) {
			LOG.trace("Task Exception (cancelled=$isCancelled): ", e)
			if (isCancelled) {
				return null
			}
			throw e
		}
	}

	public override fun updateValue(value: V) {
		super.updateValue(value)
	}

	private fun setDefaultOnFailed() {
		InvokeOnJavaFXApplicationThread {
			this.onFailed { _, task -> LOG.error(task.exception.stackTraceToString()) }
		}
	}

	/**
	 * Builder-style function to set [SUCCEEDED] callback.
	 *
	 * @param append flag to determine behavior if an existing `onSuccess` callback is present:
	 *  - if `true`, the current callback will be called prior to this `consumer` being called
	 *  - if `false`, the prior callback will be removed and never called.
	 *  - if `null`, this will throw a runtime exception if an existing callback is present.
	 *      - This is meant to help unintended overrides of existing callbacks when `append` is not explicitly specified
	 * @param consumer to be called when [SUCCEEDED]
	 * @return this
	 */
	@JvmSynthetic
	fun onSuccess(append: Boolean? = null, consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
		val appendCallbacks = onSucceeded?.appendCallbacks(append, consumer)
		val consumerEvent = EventHandler<WorkerStateEvent> { event -> consumer(event, this) }
		setOnSucceeded(appendCallbacks ?: consumerEvent)
		return this
	}

	/**
	 * Builder-style function to set [CANCELLED] callback.
	 *
	 * @param append flag to determine behavior if an existing `onCancelled` callback is present:
	 *  - if `true`, the current callback will be called prior to this `consumer` being called
	 *  - if `false`, the prior callback will be removed and never called.
	 *  - if `null`, this will throw a runtime exception if an existing callback is present.
	 *      - This is meant to help unintended overrides of existing callbacks when `append` is not explicitly specified
	 * @param consumer to be called when [CANCELLED]
	 * @return this
	 */
	@JvmSynthetic
	fun onCancelled(append: Boolean? = null, consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
		val appendCallbacks = onCancelled?.appendCallbacks(append, consumer)
		val consumerEvent = EventHandler<WorkerStateEvent> { event -> consumer(event, this) }
		setOnCancelled(appendCallbacks ?: consumerEvent)
		return this
	}

	/**
	 * Builder-style function to set [FAILED] callback.
	 *
	 * @param append flag to determine behavior if an existing `onFailed` callback is present:
	 *  - if `true`, the current callback will be called prior to this `consumer` being called
	 *  - if `false`, the prior callback will be removed and never called.
	 *  - if `null`, this will throw a runtime exception if an existing callback is present.
	 *      - This is meant to help unintended overrides of existing callbacks when `append` is not explicitly specified
	 * @param consumer to be called when [FAILED]
	 * @return this
	 */
	@JvmSynthetic
	fun onFailed(append: Boolean? = null, consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
		this.onFailedSet = true
		val eventHandler = onFailed?.appendCallbacks(append, consumer) ?: EventHandler { event -> consumer(event, this) }
		this.setOnFailed(eventHandler)
		return this
	}


	private var onEndListener: ChangeListener<Worker.State>? = null

	/**
	 * Builder-style function to set when the task ends, either by [SUCCEEDED], [CANCELLED], or [FAILED].
	 *
	 * @param append flag to determine behavior if an existing `onEnd` callback is present:
	 *  - if `true`, the current callback will be called prior to this `consumer` being called
	 *  - if `false`, the prior callback will be removed and never called.
	 *  - if `null`, this will throw a runtime exception if an existing callback is present.
	 *      - This is meant to help unintended overrides of existing callbacks when `append` is not explicitly specified
	 * @param consumer to be called when task ends
	 * @return this
	 */
	@JvmSynthetic
	fun onEnd(append: Boolean? = null, consumer: (UtilityTask<V>) -> Unit): UtilityTask<V> {
		//TODO Caleb: Consider renaming `onEnd` to `finally` since this is trigger on end for ANY reason, even if an
		// Exception was thrown. Or Maybe a separate `finally` which does what this currently does, and then change `onEnd`
		// to NOT trigger if an excpetion occures (that isn't handled by the exception handler)
		onEndListener = onEndListener?.let { oldListener ->
			stateProperty().removeListener(oldListener)
			if (append == null)
				throw TaskStateCallbackOverrideException("Overriding existing handler; If intentional, pass `false` for `append`")
			if (append) {
				ChangeListener { obs, oldv, newv ->
					when (newv) {
						SUCCEEDED, CANCELLED, FAILED -> {
							oldListener.changed(obs, oldv, newv)
							consumer(this)
						}

						else -> Unit
					}
				}
			} else null
		} ?: ChangeListener { _, _, newv ->
			when (newv) {
				SUCCEEDED, CANCELLED, FAILED -> consumer(this)
				else -> Unit
			}
		}
		this.stateProperty().addListener(onEndListener)
		return this
	}


	/**
	 *
	 * @see [onSuccess]
	 */
	@JvmOverloads
	fun onSuccess(append: Boolean? = null, consumer: BiConsumer<WorkerStateEvent, UtilityTask<V>>): UtilityTask<V> {
		return onSuccess(append) { e, t -> consumer.accept(e, t) }
	}

	/**
	 *
	 * @see [onCancelled]
	 */
	@JvmOverloads
	fun onCancelled(append: Boolean? = null, consumer: BiConsumer<WorkerStateEvent, UtilityTask<V>>): UtilityTask<V> {
		return onCancelled(append) { e, t -> consumer.accept(e, t) }
	}

	/**
	 *
	 * @see [onFailed]
	 */@JvmOverloads
	fun onFailed(append: Boolean? = null, consumer: BiConsumer<WorkerStateEvent, UtilityTask<V>>): UtilityTask<V> {
		return onFailed(append) { e, t -> consumer.accept(e, t) }
	}

	/**
	 *
	 * @see [onEnd]
	 */
	@JvmOverloads
	fun onEnd(append: Boolean? = null, consumer: Consumer<UtilityTask<V>>): UtilityTask<V> {
		return onEnd(append) { t -> consumer.accept(t) }
	}

	/**
	 * Submit this task to the [executorService].
	 *
	 * @param executorService to execute this task on.
	 * @return this task
	 */
	fun submit(executorService: ExecutorService = this.executorService) : UtilityTask<V> {
		this.executorService = executorService;
		this.executorService.submit(this)
		return this
	}

	/**
	 * Submit this task to the [executorService], and block while waiting for it to return.
	 * This will return after the task completes, but possibbly BEFORE the [onSuccess]/[onEnd] call finish.
	 *
	 * @param executorService to execute this task on.
	 * @return the result of this task, blocking if not yet done.
	 */
	@JvmOverloads
	fun submitAndWait(executorService: ExecutorService = this.executorService): V {
		this.executorService = executorService
		this.executorService.submit(this)
		return this.get()
	}

	private fun EventHandler<WorkerStateEvent>.appendCallbacks(append: Boolean? = false, consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): EventHandler<WorkerStateEvent>? {
		if (append == null) throw TaskStateCallbackOverrideException("Overriding existing handler; If intentional, pass `false` for `append`")
		if (!append) return null
		return EventHandler { event ->
			this.handle(event)
			consumer(event, this@UtilityTask)
		}
	}

	private class TaskStateCallbackOverrideException(override val message: String?) : RuntimeException(message)
}
