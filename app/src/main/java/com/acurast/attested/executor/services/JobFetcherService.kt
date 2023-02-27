package com.acurast.attested.executor.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.protocol.acurast.AcurastRPC
import com.acurast.attested.executor.utils.Tezos


class JobFetcherService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Constants.USING_ACURAST)
                AcurastRPC.fetchJobs(applicationContext)
            else
                Tezos.fetchJob(applicationContext)
        } catch (e: Exception) {
            Log.d("JobFetcher Err", e.toString())
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}