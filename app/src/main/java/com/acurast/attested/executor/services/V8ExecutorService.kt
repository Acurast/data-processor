package com.acurast.attested.executor.services

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.acurast.attested.executor.v8.V8Executor
import com.acurast.attested.executor.utils.IPFS

class V8ExecutorService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            Log.d("v8 executor", "called")
            val ipfsUri = intent.getStringExtra("ipfsUri")
            if (ipfsUri != null) {
                // Fetch the script content
                IPFS.resolveUri(this.applicationContext, ipfsUri, { script ->
                    var extras = intent.extras
                    if (extras == null) {
                        extras = Bundle()
                    }
                    V8Executor(ipfsUri.toByteArray(), script, extras).execute(
                        onSuccess = {
                            stopSelf(startId)
                        }, onError = {
                            Log.d("V8Executor", it.toString())
                            stopSelf(startId)
                        })
                }, {
                    Log.d("IPFS", it.toString())
                    stopSelf(startId)
                })
            }
        } catch (e: Exception) {
            Log.d("JobFetcher Err", e.toString())
            stopSelf(startId)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}