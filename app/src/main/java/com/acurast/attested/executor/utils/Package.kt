package com.acurast.attested.executor.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.acurast.attested.executor.ui.MainActivity
import java.io.InputStream
import java.io.OutputStream

class Package {
    fun installPackage(context: Context, inputStream: InputStream) {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionId = packageInstaller.createSession(
            PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL))
        val session = packageInstaller.openSession(sessionId)

        val sizeBytes: Long = -1
        val installationSessionOutputStream: OutputStream = session.openWrite("packageinstaller", 0, sizeBytes)

        var total = 0
        val buffer = ByteArray(65536)
        var c: Int
        while (inputStream.read(buffer).also { c = it } != -1) {
            total += c
            installationSessionOutputStream.write(buffer, 0, c)
        }
        session.fsync(installationSessionOutputStream)
        inputStream.close()
        installationSessionOutputStream.close()

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context,
            1337111117, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        session.commit(pendingIntent.intentSender)
        session.close()
    }
}

