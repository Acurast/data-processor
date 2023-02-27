package com.acurast.attested.executor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acurast.attested.executor.services.HeartbeatService
import com.acurast.attested.executor.utils.Scheduler

class HeartbeatBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.scheduleHeartbeat(context)
        Intent(context, HeartbeatService::class.java).also {
            context.startService(it)
        }
    }
}