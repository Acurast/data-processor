package com.acurast.attested.executor.utils

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.R

class Device {
    companion object {
        fun updateApp(activity: Activity) {
            val selectApkUpdate = activity.getString(R.string.select_apk_update)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            intent.type = "application/vnd.android.package-archive"
            val chooser = Intent.createChooser(intent, selectApkUpdate)

            ActivityCompat.startActivityForResult(
                activity,
                chooser,
                Constants.OPEN_APK_REQUEST_CODE,
                intent.extras
            )
        }
        fun factoryReset(context: Context) {
            val devicePolicyManager =
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            devicePolicyManager.wipeData(DevicePolicyManager.WIPE_RESET_PROTECTION_DATA)
        }
    }
}