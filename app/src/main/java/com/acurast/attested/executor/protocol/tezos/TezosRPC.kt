package com.acurast.attested.executor.protocol.tezos

import android.util.Log
import com.acurast.attested.executor.App
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.utils.Once
import com.acurast.attested.executor.utils.toHex
import it.airgap.tezos.core.converter.encoded.Address
import it.airgap.tezos.core.type.encoded.PublicKeyHash
import it.airgap.tezos.operation.Operation
import it.airgap.tezos.operation.coder.forgeToBytes
import it.airgap.tezos.rpc.TezosRpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TezosRPC {
    companion object {
        private val executor: ExecutorService = Executors.newFixedThreadPool(10)
        private val singleThreadedExecutor: ExecutorService = Executors.newSingleThreadExecutor()

        /**
         * This method queries the current counter/nonce for a given [address] account.
         */
        fun fetchCounter(
            address: String,
            onSuccess: (BigInteger) -> Unit,
            onError: (Throwable) -> Unit
        ) {
            singleThreadedExecutor.submit {
                val onceSuccessCallback = Once<BigInteger> {
                    singleThreadedExecutor.submit {
                        App.writeSharedPreferencesString(
                            Constants.TRANSACTION_COUNTER,
                            it.toString()
                        )
                        App.writeSharedPreferencesLong(
                            "LAST_COUNTER_FETCH",
                            System.currentTimeMillis()
                        )
                        onSuccess(it)
                    }
                }

                val isCacheValid = (System.currentTimeMillis() - App.readSharedPreferencesLong(
                    "LAST_COUNTER_FETCH",
                    0L
                ) < 30L * 1000L)

                if (isCacheValid) {
                    onceSuccessCallback(
                        BigInteger(
                            App.readSharedPreferencesString(
                                Constants.TRANSACTION_COUNTER,
                                "0"
                            )
                        ).add(BigInteger.ONE)
                    )
                } else {
                    Utils.getSyncedNodes().map {
                        val tezosRpc = TezosRpc(it)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val counter = tezosRpc.getCounter(
                                    contractId = Address(address),
                                    headers = listOf(Pair("Accept", "*/*")),
                                ).counter

                                onceSuccessCallback(
                                    BigInteger(counter ?: "0")
                                )
                            } catch (e: Throwable) {
                                onError(e)
                            }
                        }
                    }
                }
            }
        }

        /**
         * This method queries the latest block hash.
         */
        fun fetchBranch(
            onSuccess: (String) -> Unit,
            onError: (Throwable) -> Unit
        ) {
            val onceSuccessCallback = Once(onSuccess)
            executor.submit {
                Utils.getSyncedNodes().map {
                    val tezosRpc = TezosRpc(it)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val hash = tezosRpc.getBlockHeader(
                                headers = listOf(
                                    Pair(
                                        "Accept",
                                        "*/*"
                                    )
                                )
                            ).header.hash
                            onceSuccessCallback(hash.base58)
                        } catch (e: Throwable) {
                            onError(e)
                        }
                    }
                }
            }
        }

        /**
         * Checks if the processor public key has been revealed.
         */
        fun isRevealed(
            address: PublicKeyHash,
            onSuccess: (Boolean) -> Unit,
            onError: (Throwable) -> Unit
        ) {
            val isRevealed = App.readSharedPreferencesBoolean("IS_REVEALED", false)
            if (isRevealed) {
                onSuccess(isRevealed)
            } else {
                val onceSuccessCallback = Once<Boolean> {
                    App.writeSharedPreferencesBoolean(
                        "IS_REVEALED",
                        it
                    )
                    onSuccess(it)
                }

                executor.submit {
                    Utils.getSyncedNodes().map { node ->
                        val tezosRpc = TezosRpc(node)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val managerKey = tezosRpc.getManagerKey(
                                    contractId = address,
                                    headers = listOf(Pair("Accept", "*/*")),
                                ).manager
                                onceSuccessCallback(managerKey != null)
                            } catch (e: Throwable) {
                                onError(e)
                            }
                        }
                    }
                }
            }
        }

        /**
         * This method injects an operation into the mempool of a node.
         */
        fun injectOperation(
            operation: Operation,
            retry: Int = Constants.INJECTION_RETRY_COUNT,
            onSuccess: (String) -> Unit,
            onError: (Throwable) -> Unit,
        ) {
            val onceSuccessCallback = Once(onSuccess)
            val signature = CryptoLegacy.signOperation(operation)
            val signedOperation = operation.forgeToBytes() + signature
            executor.submit {
                Utils.getSyncedNodes().map {
                    val tezosRpc = TezosRpc(it)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val operationHash = tezosRpc.injectOperation(
                                signedOperation.toHex(),
                                headers = listOf(
                                    Pair("Accept", "*/*"),
                                    Pair("Content-Type", "application/json")
                                ),
                            ).hash

                            Log.d("OPG", operationHash.base58)
                            onceSuccessCallback(operationHash.base58)
                        } catch (e: Throwable) {
                            if (retry > 0) {
                                Thread.sleep(Constants.INJECTION_BACKOFF_TIME * (Constants.INJECTION_RETRY_COUNT - retry + 1))
                                injectOperation(operation, retry - 1, onSuccess, onError)
                            } else {
                                onError(e)
                            }
                        }
                    }
                }
            }
        }
    }
}