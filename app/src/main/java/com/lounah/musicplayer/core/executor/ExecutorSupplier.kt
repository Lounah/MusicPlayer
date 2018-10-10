package com.lounah.musicplayer.core.executor

import android.os.Process
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


class ExecutorSupplier private constructor() {

    private object Holder {
        val INSTANCE = ExecutorSupplier()
    }

    companion object {
        val instance: ExecutorSupplier by lazy { Holder.INSTANCE }
    }

    private val NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()

    var backgroundThreadExecutor: ThreadPoolExecutor

    init {
        val backgroundPriorityThreadFactory = PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThreadExecutor = ThreadPoolExecutor(
                NUMBER_OF_CORES * 2,
                NUMBER_OF_CORES * 2,
                60L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>(),
                backgroundPriorityThreadFactory
        )
    }
}