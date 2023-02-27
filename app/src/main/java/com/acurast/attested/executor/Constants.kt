package com.acurast.attested.executor

import android.app.PendingIntent
import android.os.Build
import android.os.UserManager

class Constants {
    companion object {

        // Acurast
        val USING_ACURAST = false
        val PROCESSOR_VERSION = "0.0.1"
        val ACURAST_RPC = "https://rpc.collator-1.acurast.papers.tech"
        val HEARTBEAT_INTERVAL = 15 * 60 * 1000L // 15 minutes
        val HEARTBEAT_REQUEST_CODE = 19875542
        val SHOW_ADVERTISEMENT_BUTTON = false

        // Crypto
        val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
        val ENCRYPTION_KEY_SIZE = 256
        val ENCRYPTION_BLOCK_SIZE = 128
        val ENTROPY_EXTRACTOR_HASH = "SHA-256"
        val APK_INTEGRITY = "SHA-256"

        val UPDATE_INTEGRITY_URL = "https://update-processor.acurast.com/update.json"

        val KEYSTORE_PROVIDER = "AndroidKeyStore"
        val PROCESSOR_KEY_ALIAS = "signer"
        val EC_CURVE = "secp256r1"
        val ATTESTATION_CHALLENGE = "" //https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder#setAttestationChallenge(byte[])
        val SIGNER_KEY_ALIAS = "signer"
        val ENCRYPTER_KEY_ALIAS = "encrypter"
        val KEY_FILE = "key"
        val SECP256R1 = "secp256r1"
        val TRANSACTION_COUNTER = "transaction_counter"
        val RANDOM_KEY_ALIAS = "random"
        val FULFILL_CONTRACT_ADDRESS =
            if (BuildConfig.NETWORK == "MAINNET") "KT1MFfBe4EtzDofUcfeBmNShJPesY4HhfjMC" else "KT1WkMY3Vj6h1FZTAYcD1EmwcV89zVnr9ckX"
        val ANDROID_KEYSTORE_ALIAS = "AndroidKeyStore"
        val BIGMAP_ID = if (BuildConfig.NETWORK == "MAINNET") 152890 else 43726
        val IPFS_NODE = "https://cloudflare-ipfs.com/ipfs/"
        val IPFS_SCHEMA = "ipfs://"
        val SCHEDULE_SHARED_PREFERENCES_KEY = "schedule"
        val CACHE_SHARED_PREFERENCES_KEY = "cache"
        val NEXT_REQUEST_COUNTER_KEY = "next_request_counter"
        val EXECUTION_TIME_LIMIT = 10 * 1000L //5 seconds+
        val BLOCKCHAIN_NODES = if (BuildConfig.NETWORK == "MAINNET") arrayListOf(
            "https://tezos-node-rolling.prod.gke.papers.tech",
            "https://tezos-node.prod.gke.papers.tech",
            "https://rpc.tzbeta.net",
            "https://teznode.letzbake.com",
            "https://mainnet.api.tez.ie"
        ) else arrayListOf(
            "https://tezos-ghostnet-node.prod.gke.papers.tech",
            "https://ghostnet.ecadinfra.com"
        )
        val NONE_WITH_ECDSA = "NONEwithECDSA"

        val JOB_FETCHER_SCHEDULE_INTERVAL = 1 * 60 * 1000L
        val JOB_FETCHER_SCHEDULE_SHIFT = 15 * 1000L
        val JOB_FETCHER_REQUEST_CODE = 19875541

        val REPEATING_HEARTBEAT_REQUEST_CODE = 152454
        val INJECTION_RETRY_COUNT = 3
        val INJECTION_BACKOFF_TIME = 1000L
        val NOTIFICATION_CHANNEL_ID = "JOB_FETCHER_CHANNEL_ID"
        val FOREGROUND_SERVICE_ID = 13244
        val ACQUIRE_SLEEP = 100L
        val ENVIRONMENT_KEY_PREFIX = "environment_"
        val HTTP_CONNECT_TIMEOUT = 30000

        val MANUAL_PERMISSION_REQUEST_CODE = 19835541

        val PENDING_INTENT_FLAG =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT

        val SYNC_THRESHOLD_MILLIS = 1000 * 120
        val SYNC_RESPONSE_TIMEOUT_MILLIS = 500L

        val REMOVAL_APPS = setOf(
            "com.android.contacts",
            "com.android.dialer",
            "com.android.phone",
            "com.samsung.android.phone",
            "com.google.android.contacts",
            "com.samsung.android.contacts",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.chrome",
            "com.android.vending",
            "com.google.android.apps.nbu.files",
            "com.sec.android.app.myfiles",
            "com.google.android.apps.wellbeing",
            "com.samsung.android.wellbeing",
            //"com.wos.face.service"
        )

        val RESTRICTIONS = arrayListOf(
            UserManager.DISALLOW_ADD_USER,
            UserManager.DISALLOW_ADJUST_VOLUME,
            UserManager.DISALLOW_AMBIENT_DISPLAY,
            UserManager.DISALLOW_APPS_CONTROL,
            UserManager.DISALLOW_AUTOFILL,
            UserManager.DISALLOW_BLUETOOTH,
            UserManager.DISALLOW_BLUETOOTH_SHARING,
            UserManager.DISALLOW_CONFIG_BLUETOOTH,
            UserManager.DISALLOW_CONFIG_BRIGHTNESS,
            UserManager.DISALLOW_CONFIG_CELL_BROADCASTS,
            UserManager.DISALLOW_CONFIG_CREDENTIALS,
            UserManager.DISALLOW_CONFIG_DATE_TIME,
            UserManager.DISALLOW_CONFIG_LOCALE,
            UserManager.DISALLOW_CONFIG_LOCATION,
            UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
            UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
            UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT,
            UserManager.DISALLOW_CONFIG_TETHERING,
            UserManager.DISALLOW_CONFIG_VPN,
            //UserManager.DISALLOW_CONFIG_WIFI,
            UserManager.DISALLOW_CONTENT_CAPTURE,
            UserManager.DISALLOW_CREATE_WINDOWS,
            UserManager.DISALLOW_CROSS_PROFILE_COPY_PASTE,
            UserManager.DISALLOW_DATA_ROAMING,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            //UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_FUN,
            UserManager.DISALLOW_INSTALL_APPS,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
            UserManager.DISALLOW_SET_WALLPAPER,
            UserManager.DISALLOW_PRINTING,
            UserManager.DISALLOW_MODIFY_ACCOUNTS,
            UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS,
            UserManager.DISALLOW_SMS,
            UserManager.DISALLOW_NETWORK_RESET,
            UserManager.DISALLOW_OUTGOING_BEAM,
            UserManager.DISALLOW_OUTGOING_CALLS,
            UserManager.DISALLOW_REMOVE_USER,
            UserManager.DISALLOW_UNIFIED_PASSWORD,
            UserManager.DISALLOW_UNINSTALL_APPS,
            UserManager.DISALLOW_UNMUTE_MICROPHONE,
            UserManager.DISALLOW_USER_SWITCH
        )
        val STREAM_BUFFER_LENGTH = 4096
        val OPEN_APK_REQUEST_CODE = 17234
    }

}