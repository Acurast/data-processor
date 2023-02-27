package com.acurast.attested.executor.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.HeartbeatBroadcastReceiver
import com.acurast.attested.executor.JobFetcherBroadcastReceiver
import com.acurast.attested.executor.V8ExecutorBroadcastReceiver
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse

fun String.hexStringToByteArray() =
    ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

fun ByteArray.toHex() = this.fold("", { str, it -> str + "%02x".format(it) })
fun ByteArray.trim() = this.fold(
    ByteArray(0),
    { result, it -> if (it == 0.toByte() && result.isEmpty()) result else result + it })

class Scheduler {
    companion object {
        /**
         * Schedule a task for sending an heartbeat on-chain.
         */
        fun scheduleHeartbeat(context: Context) {
            try {
                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val alarmIntent =
                    Intent(context, HeartbeatBroadcastReceiver::class.java).let { intent ->
                        PendingIntent.getBroadcast(
                            context,
                            Constants.HEARTBEAT_REQUEST_CODE,
                            intent,
                            Constants.PENDING_INTENT_FLAG
                        )
                    }

                val systemNextScheduleTimestamp = System.currentTimeMillis() + Constants.HEARTBEAT_INTERVAL

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    systemNextScheduleTimestamp,
                    alarmIntent
                )
            } catch (exception: Exception) {
                Log.d("ERR schedule_heartbeat", exception.toString())
            }
        }

        /**
         * Schedule a task for fetching the Jobs.
         */
        fun scheduleFetcher(context: Context) {
            try {
                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val alarmIntent =
                    Intent(context, JobFetcherBroadcastReceiver::class.java).let { intent ->
                        PendingIntent.getBroadcast(
                            context,
                            Constants.JOB_FETCHER_REQUEST_CODE,
                            intent,
                            Constants.PENDING_INTENT_FLAG
                        )
                    }

                val systemNextScheduleTimestamp =
                    ((System.currentTimeMillis() / Constants.JOB_FETCHER_SCHEDULE_INTERVAL + 1) * Constants.JOB_FETCHER_SCHEDULE_INTERVAL) + Constants.JOB_FETCHER_SCHEDULE_SHIFT

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    systemNextScheduleTimestamp,
                    alarmIntent
                )
            } catch (exception: Exception) {
                Log.d("ERR schedule_fetcher", exception.toString())
            }
        }

        fun scheduleV8Execution(
            context: Context,
            ipfsUri: String,
            requester: ByteArray,
            startTimestamp: Long,
            endTimestamp: Long,
            interval: Long,
            fee: Long,
            gasLimit: Long,
            storageLimit: Long
        ) {
            if (endTimestamp >= System.currentTimeMillis()) {
                IPFS.resolveUri(context, ipfsUri, { _ ->

                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    val systemNextScheduleTimestamp = if (interval > 0) {
                        // Acurast already provides interval in milliseconds
                        val intervalMillis =
                            if (Constants.USING_ACURAST) interval else interval * 1000
                        val timeShiftMillis = startTimestamp % intervalMillis
                        Math.min(
                            Math.max(
                                startTimestamp,
                                (((System.currentTimeMillis() / intervalMillis) + 1) * intervalMillis) + timeShiftMillis
                            ), endTimestamp
                        )
                    } else {
                        startTimestamp
                    }

                    val sharedPreferences =
                        context.getSharedPreferences(
                            Constants.SCHEDULE_SHARED_PREFERENCES_KEY,
                            Context.MODE_PRIVATE
                        )
                    val nextRequestCounter =
                        sharedPreferences.getInt(Constants.NEXT_REQUEST_COUNTER_KEY, 0)

                    val counterKey = "${ipfsUri}_${systemNextScheduleTimestamp}"

                    val requestCode = if (sharedPreferences.contains(counterKey)) {
                        sharedPreferences.getInt(counterKey, nextRequestCounter)
                    } else {
                        with(sharedPreferences.edit()) {
                            putInt(counterKey, nextRequestCounter)
                            putInt(Constants.NEXT_REQUEST_COUNTER_KEY, nextRequestCounter + 1)
                            apply()
                        }
                        nextRequestCounter
                    }

                    val isLastReport = systemNextScheduleTimestamp + interval > endTimestamp

                    val alarmIntent =
                        Intent(context, V8ExecutorBroadcastReceiver::class.java).let { intent ->
                            intent.putExtra("ipfsUri", ipfsUri)
                            intent.putExtra("requester", requester)
                            intent.putExtra("isLastReport", isLastReport)
                            intent.putExtra("start", startTimestamp)
                            intent.putExtra("end", endTimestamp)
                            intent.putExtra("interval", interval)
                            intent.putExtra("fee", fee)
                            intent.putExtra("gasLimit", gasLimit)
                            intent.putExtra("storageLimit", storageLimit)
                            PendingIntent.getBroadcast(
                                context,
                                requestCode,
                                intent,
                                Constants.PENDING_INTENT_FLAG
                            )
                        }

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        systemNextScheduleTimestamp,
                        alarmIntent
                    )
                }, { Log.d("ERR IPFS", it.toString()) })
            }
        }

        fun sendSafetyNetRequest(
            context: Context,
            nonce: ByteArray,
            successCallback: (String) -> Unit,
            errorCallback: (Exception) -> Unit
        ) {
            val integrityManager =
                IntegrityManagerFactory.create(context)

            val integrityTokenResponse: Task<IntegrityTokenResponse> =
                integrityManager.requestIntegrityToken(
                    IntegrityTokenRequest.builder()
                        .setNonce(nonce.toString(charset = Charsets.UTF_8))
                        .build()
                )
            integrityTokenResponse.addOnSuccessListener {
                Log.d("integrity_token", it.token())
                successCallback(it.token())
            }
            integrityTokenResponse.addOnFailureListener {
                errorCallback(it)
            }
        }
    }
}