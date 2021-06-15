package org.janelia.saalfeldlab.fx

import com.google.common.util.concurrent.ThreadFactoryBuilder
import javafx.concurrent.Task
import javafx.concurrent.Worker.State.CANCELLED
import javafx.concurrent.Worker.State.FAILED
import javafx.concurrent.Worker.State.SUCCEEDED
import javafx.concurrent.WorkerStateEvent
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.function.BiConsumer
import java.util.function.Consumer

class Tasks {

    companion object {
        @JvmStatic
        fun <T> createTask(call: (UtilityTask<T>) -> T): UtilityTask<T> {
            return UtilityTask(call)
        }
    }
}

private val THREAD_FACTORY: ThreadFactory = ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("task-thread-%d")
        .build()

private val TASK_SERVICE = Executors.newCachedThreadPool(THREAD_FACTORY)

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

    @JvmSynthetic
    fun onSuccess(consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
        this.setOnSucceeded { event -> consumer(event, this) }
        return this
    }

    @JvmSynthetic
    fun onCancelled(consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
        this.setOnCancelled { event -> consumer(event, this) }
        return this
    }

    @JvmSynthetic
    fun onFailed(consumer: (WorkerStateEvent, UtilityTask<V>) -> Unit): UtilityTask<V> {
        onFailedSet = true
        this.setOnFailed { event -> consumer(event, this) }
        return this
    }

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

    fun submit(executorService: ExecutorService) {
        executorService.submit(this)
    }

    fun submit(): UtilityTask<V> {
        submit(TASK_SERVICE)
        return this;
    }
}
