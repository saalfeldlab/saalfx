package org.janelia.saalfeldlab.fx


import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel


private val LOG = KotlinLogging.logger {}

/**
 * Channel wrapper than supports running jobs sequentially within a scope, with cancellation.
 *
 *
 * @param coroutineScope to execute the job's on
 * @param delay optional delay after a job finishes before attempting to execute the next job
 */
open class ChannelLoop(coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default), capacity: Int = Channel.RENDEZVOUS, val delay: suspend () -> Unit = {}) : CoroutineScope by coroutineScope {
	protected open val channel = Channel<Job>(capacity = capacity)
	protected var currentJob : Job?  =null

	/**
	 * Submit a block to execute in the conflated loop. Will cancel the current job and submit this block
	 *
	 * @param cancelCurrentJob cancel the current job if one exists
	 * @param block to execute
	 * @return the job to be submitted
	 */
	open fun submit(cancelCurrentJob : Boolean = false, block: suspend CoroutineScope.() -> Unit): Job {
		ensureActive()

		val job = launch(start = CoroutineStart.LAZY) {
			block()
		}
		if (cancelCurrentJob)
			currentJob?.cancel()
		launch { channel.send(job) }
		currentJob = job
		return job
	}

	init {
		launch {
			for (msg in channel) {
				runCatching {
					msg.start()
					msg.join()
				}.onFailure { it ->
					if (it is CancellationException)
						LOG.trace(it) { "Channel Loop job cancelled" }
					else
						LOG.error(it) { "Error in Channel Loop" }
				}
				delay()
			}
		}
	}
}