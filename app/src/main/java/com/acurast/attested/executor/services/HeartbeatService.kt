package com.acurast.attested.executor.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.acurast.attested.executor.protocol.acurast.AcurastRPC

class HeartbeatService : Service() {
    private val TAG = "HeartbeatService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            AcurastRPC.extrinsic.heartbeat()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}