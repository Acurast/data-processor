package com.acurast.attested.executor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acurast.attested.executor.services.JobFetcherService
import com.acurast.attested.executor.utils.Scheduler

class JobFetcherBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.scheduleFetcher(context)
        Intent(context, JobFetcherService::class.java).also {
            context.startService(it)
        }
    }
}