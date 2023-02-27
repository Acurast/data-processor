package com.acurast.attested.executor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acurast.attested.executor.services.V8ExecutorService

class V8ExecutorBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Dispatch the V8Executor service
        Intent(context, V8ExecutorService::class.java).also { serviceIntent ->
            intent.extras?.let {
                serviceIntent.putExtras(it)
                context.startService(serviceIntent)
            }
        }
    }
}