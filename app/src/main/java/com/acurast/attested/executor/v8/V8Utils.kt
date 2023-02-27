package com.acurast.attested.executor.v8

import com.acurast.attested.executor.Constants
import com.eclipsesource.v8.V8Object

class V8Utils {
    companion object {
        fun withV8Lock(receiver: V8Object, callback: () -> Unit) {
            while (!receiver.runtime.locker.tryAcquire()) {
                if (receiver.runtime.isReleased) {
                    return
                }
                Thread.sleep(Constants.ACQUIRE_SLEEP)
            }
            callback()
            receiver.runtime.locker.release()
        }
    }
}