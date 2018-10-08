package com.lounah.musicplayer.core.executor

import android.os.Process
import java.util.concurrent.ThreadFactory

class PriorityThreadFactory(private val mThreadPriority: Int) : ThreadFactory {

    override fun newThread(runnable: Runnable): Thread {
        val wrapperRunnable = Runnable {
            try {
                Process.setThreadPriority(mThreadPriority)
            } catch (t: Throwable) {

            }

            runnable.run()
        }
        return Thread(wrapperRunnable)
    }

}