package com.acurast.attested.executor.protocol.tezos

import android.util.Log
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.crypto.ICrypto
import com.acurast.attested.executor.utils.Tezos
import it.airgap.tezos.core.coder.encoded.decodeFromBytes
import it.airgap.tezos.core.converter.encoded.createHash
import it.airgap.tezos.core.type.encoded.P256PublicKey
import it.airgap.tezos.core.type.encoded.PublicKey
import it.airgap.tezos.core.type.encoded.PublicKeyHash
import it.airgap.tezos.rpc.TezosRpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class Utils {
    companion object {
        fun getPublicKeyHash(cryptoModule: ICrypto): PublicKeyHash = getPublicKey(cryptoModule).createHash()
        fun getPublicKey(cryptoModule: ICrypto): PublicKey = P256PublicKey.decodeFromBytes(cryptoModule.getPublicKey())

        /**
         * Look-up all configured nodes, returning in random order the ones that are synchronized.
         */
        fun getSyncedNodes(timeoutMillis: Long = Constants.SYNC_RESPONSE_TIMEOUT_MILLIS): List<String> {
            val countDownLatch = CountDownLatch(Constants.BLOCKCHAIN_NODES.size)
            val resultSet = ConcurrentHashMap.newKeySet<String>()
            Tezos.executor.submit {
                Constants.BLOCKCHAIN_NODES.map { node ->
                    val tezosRpc = TezosRpc(node)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val header = tezosRpc.getBlockHeader(
                                headers = listOf(Pair("Accept", "*/*")),
                                requestTimeout = timeoutMillis,
                                connectionTimeout = timeoutMillis,
                            ).header

                            val deltaMillis =
                                System.currentTimeMillis() - header.timestamp.toMillis().long
                            if (deltaMillis < Constants.SYNC_THRESHOLD_MILLIS) {
                                resultSet.add(node)
                            }
                        } catch (e: Throwable) {
                            Log.d("Tezos", "RPC ${node} is offline.")
                        } finally {
                            countDownLatch.countDown()
                        }
                    }
                }
            }
            countDownLatch.await(timeoutMillis * 2, TimeUnit.MILLISECONDS)
            return listOf(resultSet.toList().random())
        }
    }
}