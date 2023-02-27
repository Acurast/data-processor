package com.acurast.attested.executor.utils

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.acurast.attested.executor.App
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.R

class Notification {
    companion object {
        fun notify(title: String, text: String) {
            val builder = NotificationCompat.Builder(App.context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(App.context)) {
                // The notification currently gets overridden.
                // For stacking configurations, the id should be incremented on notification spawn
                // and decremented on notification dismiss.
                notify(0, builder.build())
            }
        }
    }
}