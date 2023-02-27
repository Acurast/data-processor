package com.acurast.attested.executor.utils

import android.content.Context
import android.util.Log
import com.acurast.attested.executor.App
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.protocol.tezos.TezosRPC
import com.acurast.attested.executor.protocol.tezos.Utils
import it.airgap.tezos.michelson.converter.tryAs
import it.airgap.tezos.michelson.micheline.MichelineLiteral
import it.airgap.tezos.michelson.micheline.MichelinePrimitiveApplication
import it.airgap.tezos.michelson.micheline.MichelineSequence
import it.airgap.tezos.michelson.micheline.dsl.builder.expression.*
import it.airgap.tezos.michelson.micheline.dsl.micheline
import it.airgap.tezos.michelson.packer.packToScriptExpr
import it.airgap.tezos.rpc.TezosRpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.*

// TODO: remove this class once acurast parachain is live
class Tezos {
    companion object {
        val executor: ExecutorService = Executors.newFixedThreadPool(10)

        fun fetchJob(context: Context) {
            executor.submit {
                Utils.getSyncedNodes().map { node ->
                    val tezosRpc = TezosRpc(node)

                    CoroutineScope(Dispatchers.IO).launch {
                        val entry = try {
                            tezosRpc.getBigMapValue(
                                bigMapId = Constants.BIGMAP_ID.toString(),
                                key = micheline { string(App.Protocol.tezos.getAddress()) }.packToScriptExpr { address },
                                headers = listOf(Pair("Accept", "*/*")),
                            ).value
                        } catch (e: Throwable) {
                            Log.e("ERR fetch", Log.getStackTraceString(e))
                            null
                        }

                        try {
                            entry.tryAs<MichelineSequence>().nodes.forEach { it ->
                                val job = it.tryAs<MichelinePrimitiveApplication>()

                                val script = job
                                    .args[0].tryAs<MichelineLiteral.Bytes>()
                                    .toByteArray()

                                val status = job
                                    .args[1].tryAs<MichelinePrimitiveApplication>()
                                    .args[0].tryAs<MichelineLiteral.Integer>()
                                    .toInt()

                                val start = Instant.parse(
                                    job
                                        .args[1].tryAs<MichelinePrimitiveApplication>()
                                        .args[1].tryAs<MichelineLiteral.String>()
                                        .string
                                )

                                val end = Instant.parse(
                                    job
                                        .args[1].tryAs<MichelinePrimitiveApplication>()
                                        .args[2].tryAs<MichelineLiteral.String>()
                                        .string
                                )

                                val interval = job
                                    .args[1].tryAs<MichelinePrimitiveApplication>()
                                    .args[3].tryAs<MichelineLiteral.Integer>()
                                    .toLong()

                                val fee = job
                                    .args[1].tryAs<MichelinePrimitiveApplication>()
                                    .args[4].tryAs<MichelineLiteral.Integer>()
                                    .toLong()

                                val gasLimit = job
                                    .args[1].tryAs<MichelinePrimitiveApplication>()
                                    .args[5].tryAs<MichelineLiteral.Integer>()
                                    .toLong()

                                val storageLimit = job
                                    .args[1].tryAs<MichelinePrimitiveApplication>()
                                    .args[6].tryAs<MichelineLiteral.Integer>()
                                    .toLong()

                                Scheduler.scheduleV8Execution(
                                    context,
                                    String(script),
                                    byteArrayOf(),
                                    start.toEpochMilli(),
                                    end.toEpochMilli(),
                                    interval,
                                    fee,
                                    gasLimit,
                                    storageLimit
                                )

                                if (status == 0) {
                                    ack(script)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ERR parsing job", Log.getStackTraceString(e))
                        }
                    }
                }
            }
        }

        fun ack(script: ByteArray) {
            TezosRPC.fetchBranch(
                onSuccess = { branch ->
                    TezosRPC.fetchCounter(
                        address = App.Protocol.tezos.getAddress(),
                        onSuccess = { counter ->
                            TezosRPC.injectOperation(
                                operation = com.acurast.attested.executor.protocol.tezos.Operation.buildOperation(
                                    branch,
                                    com.acurast.attested.executor.protocol.tezos.Operation.buildTransaction(
                                        App.Protocol.tezos.getSigner(),
                                        BigInteger.valueOf(1740),
                                        counter.add(BigInteger.ONE),
                                        BigInteger.valueOf(11000),
                                        BigInteger.valueOf(11000),
                                        BigInteger.ZERO,
                                        Constants.FULFILL_CONTRACT_ADDRESS,
                                        "ack",
                                        micheline { bytes(script) },
                                    )
                                ),
                                onSuccess = {
                                    Log.d("OPG", it)
                                }, onError = {
                                    Log.e("Error", "$it")
                                })
                        },
                        onError = {
                            Log.d("Error", "$it")
                        }
                    )
                },
                onError = {
                    Log.d("Error", "$it")
                })
        }
    }
}