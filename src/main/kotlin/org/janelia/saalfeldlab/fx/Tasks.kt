package org.janelia.saalfeldlab.fx

import com.google.common.util.concurrent.ThreadFactoryBuilder
import javafx.concurrent.Task
import javafx.concurrent.Worker.State.*
import javafx.concurrent.WorkerStateEvent
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

private val TASK_SERVICE = Executors.newCachedThreadPool(THREAD_FACTORY)

/**
 * Convenience wrapper class around [Task]
 *
 * @param V type of super class Task<V>
 * @property onCall called during [Task.call], but wrapped with exception handling
 * @constructor Create empty Utility task
 */
class UtilityTask<V>(private val onCall: (UtilityTask<V>) -> V) : Task<V>() {

    private var onFailedSet = false

    companion object {
        private val LOG = LoggerFactory.getLogger(UtilityTask::class.java)
    }

    override fun call(): V? {
        try {
            /* If no `onEnd/onFail` has been set, then we should listen for thrown exceptions and throw them */
            if (!this.onFailedSet) setDefaultExceptionHandler()
            return onCall(this)
        } catch (e: Exception) {
            if (isCancelled) {
                LOG.debug("Task was cancelled")
                return null
            }
            throw e
        }
    }

    public override fun updateValue(value: V) {
        super.updateValue(value)
    }

    private fun setDefaultExceptionHandler() {
        InvokeOnJavaFXApplicationThread {
            this.onFailed { _, task -> LOG.error(task.exception.stackTraceToString()) }
        }
    }

    /**
     * Builder-style function to set [SUCCEEDED] callback.
     *
     * @param consumer to be called when [SUCCEEDED]
     * @return this
     */
    @JvmSynthetic
    fun onSuccess(consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
        this.setOnSucceeded { event -> consumer(event, this) }
        return this
    }

    /**
     * Builder-style function to set [CANCELLED] callback.
     *
     * @param consumer to be called when [CANCELLED]
     * @return this
     */
    @JvmSynthetic
    fun onCancelled(consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
        this.setOnCancelled { event -> consumer(event, this) }
        return this
    }

    /**
     * Builder-style function to set [FAILED] callback.
     *
     * @param consumer to be called when [FAILED]
     * @return this
     */
    @JvmSynthetic
    fun onFailed(consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
        onFailedSet = true
        this.setOnFailed { event -> consumer(event, this) }
        return this
    }

    /**
     * Builder-style function to set when the task ends, either by [SUCCEEDED], [CANCELLED], or [FAILED].
     *
     * @param consumer to be called when task ends
     * @return this
     */
    @JvmSynthetic
    fun onEnd(consumer: (UtilityTask<V>) -> Unit): UtilityTask<V> {
        this.stateProperty().addListener { _, _, newv ->
            when (newv) {
                SUCCEEDED, CANCELLED, FAILED -> consumer(this)
                else -> Unit
            }
        }
        return this
    }

    fun onSuccess(consumer: BiConsumer<WorkerStateEvent, UtilityTask<V>>): UtilityTask<V> {
        return onSuccess { e, t -> consumer.accept(e, t) }
    }

    fun onCancelled(consumer: BiConsumer<WorkerStateEvent, UtilityTask<V>>): UtilityTask<V> {
        return onCancelled { e, t -> consumer.accept(e, t) }
    }

    fun onFailed(consumer: BiConsumer<WorkerStateEvent, UtilityTask<V>>): UtilityTask<V> {
        return onFailed { e, t -> consumer.accept(e, t) }
    }

    fun onEnd(consumer: Consumer<UtilityTask<V>>): UtilityTask<V> {
        return onEnd { t -> consumer.accept(t) }
    }

    /**
     * Submit this task to the [executorService].
     *
     * @param executorService to execute this task on.
     */
    fun submit(executorService: ExecutorService) {
        executorService.submit(this)
    }

    /**
     * Submit this task to the [executorService], and block while waiting for it to return.
     *
     * @param executorService to execute this task on.
     */
    @JvmOverloads
    fun submitAndWait(executorService: ExecutorService = TASK_SERVICE) {
        executorService.submit(this).get()
    }

    /**
     * Submit this task on a default [ExecutorService].
     *
     * @return this
     */
    fun submit(): UtilityTask<V> {
        submit(TASK_SERVICE)
        return this
    }
}
