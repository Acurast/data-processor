package com.acurast.attested.executor.ui

import acurast.codec.extensions.toHex
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.acurast.attested.executor.App
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.JobFetcherBroadcastReceiver
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.services.JobFetcherService
import com.acurast.attested.executor.utils.*
import okhttp3.*
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import androidx.activity.compose.setContent
import com.acurast.attested.executor.services.HeartbeatService
import com.acurast.attested.executor.ui.theme.AcurastTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fit system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AcurastTheme {
                AcurastApp()
            }
        }

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            val packageName = packageName
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                startActivity(intent)
            }
        }

        // Schedule the Job fetcher
        val alarmIntent =
            Intent(this, JobFetcherBroadcastReceiver::class.java).let { intent ->
                PendingIntent.getBroadcast(
                    this,
                    Constants.REPEATING_HEARTBEAT_REQUEST_CODE,
                    intent,
                    Constants.PENDING_INTENT_FLAG
                )
            }

        val systemNextScheduleTimestamp = System.currentTimeMillis() + 100
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            systemNextScheduleTimestamp,
            AlarmManager.INTERVAL_HOUR,
            alarmIntent
        )

        // Fetch Jobs
        Scheduler.scheduleFetcher(this)
        Intent(this, JobFetcherService::class.java).also {
            this.startService(it)
        }

        setupHeartbeatService()

        Thread {
            for (parameterName in intent.data?.queryParameterNames ?: emptyList()) {
                App.writeSharedPreferencesString(
                    "${Constants.ENVIRONMENT_KEY_PREFIX}${parameterName}",
                    intent.data?.getQueryParameter(parameterName) ?: ""
                )
            }

            // Generate key pair for the processor if it does not exist.
            CryptoLegacy.generateProcessorKeyIfNecessary()

        }.start()
    }

    fun setupHeartbeatService() {
        Scheduler.scheduleHeartbeat(this)
        Intent(this, HeartbeatService::class.java).also {
            this.startService(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == Constants.OPEN_APK_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultData != null && resultData.data != null) {
                    val request: Request = Request.Builder()
                        .url(Constants.UPDATE_INTEGRITY_URL)
                        .build()

                    OkHttpClient().newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Toast.makeText(
                                this@MainActivity,
                                "integrity check failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val fileInputStream = FileInputStream(
                                contentResolver.openFileDescriptor(
                                    resultData.data!!,
                                    "r"
                                )?.fileDescriptor
                            )
                            val hash = CryptoLegacy.hashInputstream(
                                MessageDigest.getInstance(Constants.APK_INTEGRITY),
                                fileInputStream
                            )
                            if (response.body?.charStream()?.readText()?.contains(hash.toHex()) == true) {
                                fileInputStream.channel.position(0)

                                Package().installPackage(this@MainActivity, fileInputStream)
                            }
                        }
                    })
                }
            }
        }
    }
}