package com.acurast.attested.executor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.crypto.curves.GenericEcdsa
import com.acurast.attested.executor.crypto.curves.P256
import com.acurast.attested.executor.native.SB_CURVE
import com.acurast.attested.executor.protocol.acurast.Acurast
import com.acurast.attested.executor.protocol.ethereum.Ethereum
import com.acurast.attested.executor.protocol.tezos.Tezos
import com.acurast.attested.executor.utils.Networking
import it.airgap.tezos.core.Tezos as TezosSDK
import it.airgap.tezos.rpc.RpcModule

class App : Application() {
    class Signer {
        companion object {
            // Signers
            val SECP_256_K1 = GenericEcdsa(SB_CURVE.SB_SW_CURVE_SECP256K1)
            val SECP_256_R1 = P256()
        }
    }

    class Protocol {
        companion object {
            // Protocols
            val acurast = Acurast(Signer.SECP_256_R1)
            val ethereum = Ethereum(Signer.SECP_256_K1)
            val tezos = Tezos(Signer.SECP_256_R1)
        }
    }

    /**
     * A key-value storage shared between components
     */
    companion object {
        lateinit var sharedPreferences: SharedPreferences
        lateinit var context: Context

        fun containsSharedPreference(key: String): Boolean {
            return sharedPreferences.contains(key)
        }

        fun readSharedPreferencesLong(key: String, default: Long): Long {
            return sharedPreferences.getLong(key, default)
        }

        fun readSharedPreferencesString(key: String, default: String): String {
            return sharedPreferences.getString(key, default)!!
        }

        fun writeSharedPreferencesLong(key: String, value: Long) {
            with(sharedPreferences.edit()) {
                putLong(
                    key, value
                )
                apply()
            }
        }

        fun writeSharedPreferencesString(key: String, value: String) {
            with(sharedPreferences.edit()) {
                putString(
                    key, value
                )
                apply()
            }
        }

        fun writeSharedPreferencesBoolean(key: String, value: Boolean) {
            with(sharedPreferences.edit()) {
                putBoolean(
                    key, value
                )
                apply()
            }
        }

        fun readSharedPreferencesBoolean(key: String, default: Boolean): Boolean {
            return sharedPreferences.getBoolean(key, default)
        }

        fun clearSharedPreferences() {
            sharedPreferences.edit().clear().apply()
        }
    }

    /**
     * Creates a notification channel to notify the processor
     * about actions performed by the application
     */
    private fun createNotificationChannel() {
        // NotificationChannel is only supported on API 26+
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Bootstrap the application
     */
    override fun onCreate() {
        super.onCreate()
        context = this.applicationContext
        sharedPreferences = this.applicationContext.getSharedPreferences(
            Constants.CACHE_SHARED_PREFERENCES_KEY,
            Context.MODE_PRIVATE
        )

        // Setup Tezos SDK
        TezosSDK {
            isDefault = true
            cryptoProvider = CryptoLegacy.Tezos()
            use(RpcModule) {
                httpClientProvider = Networking.HttpClient()
            }
        }

        // Prepare Acurast protocol
        Protocol.acurast.init(
            onSuccess = {
                Log.d("APP", "Acurast protocol is ready.")
            },
            onError = {
                Log.e("ERROR", "$it")
            }
        )

        // Prepare Ethereum protocol
        Protocol.ethereum.init(
            onSuccess = {
                Log.d("APP", "Ethereum protocol is ready.")
            },
            onError = {
                Log.e("ERROR", "$it")
            }
        )

        // Prepare Tezos protocol
        Protocol.tezos.init(
            onSuccess = {
                Log.d("APP", "Tezos protocol is ready.")
            },
            onError = {
                Log.e("ERROR", "$it")
            }
        )

        createNotificationChannel()
    }
}