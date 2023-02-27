package com.acurast.attested.executor.protocol.acurast

import acurast.codec.extensions.*
import acurast.codec.extrinsic.*
import acurast.codec.type.*
import acurast.codec.type.acurast.JobIdentifier
import acurast.codec.type.acurast.MarketplaceAdvertisement
import acurast.codec.type.acurast.MarketplacePricing
import acurast.codec.type.acurast.SchedulingWindow
import acurast.codec.type.manager.ProcessorPairing
import acurast.codec.type.marketplace.ExecutionResult
import acurast.rpc.RPC
import android.content.Context
import android.util.Log
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.crypto.Attestation
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.utils.Notification
import com.acurast.attested.executor.utils.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AcurastRPC {
    data class OperationData(
        val era: ExtrinsicEra,
        val nonce: Long,
        val tip: BigInteger,
        val specVersion: Long,
        val transactionVersion: Long,
        val genesisHash: ByteArray,
        val blockHash: ByteArray,
    )

    companion object {
        val executor: ExecutorService = Executors.newFixedThreadPool(10)

        fun fetchJobs(context: Context) {
            val rpc = RPC(Constants.ACURAST_RPC)
            val accountId = CryptoLegacy.getPublicKey().blake2b(256)

            executor.submit {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        rpc.getAssignedJobs(accountId).forEach { job ->
                            val registration = rpc.getJobRegistration(job.jobId)
                            if(job.acknowledged) {
                                Scheduler.scheduleV8Execution(
                                    context,
                                    String(registration.script),
                                    job.jobId.requester.toU8a(),
                                    registration.schedule.startTime,
                                    registration.schedule.endTime,
                                    registration.schedule.interval,
                                    0,
                                    0,
                                    0
                                )
                            } else {
                                // TODO : Add heuristics for accepting the job requirements
                                extrinsic.acknowledgeJob(job.jobId)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("Error", Log.getStackTraceString(e))
                    }
                }
            }
        }
    }

    class queries {
        companion object {
            /**
             * Verify if the account associated to the device is attested
             */
            fun isAttested(callback: (Boolean) -> Unit) {
                val rpc = RPC(Constants.ACURAST_RPC)
                val accountId = CryptoLegacy.getPublicKey().blake2b(256)

                CoroutineScope(Dispatchers.IO).launch {
                    callback(rpc.isAttested(accountId))
                }
            }

            /**
             * Get the manager paired with this device
             */
            suspend fun managerIdentifier(): Int? {
                val rpc = RPC(Constants.ACURAST_RPC)
                val accountId = CryptoLegacy.getPublicKey().blake2b(256)

                val key =
                    "AcurastProcessorManager".toByteArray().xxH128() +
                    "ProcessorToManagerIdIndex".toByteArray().xxH128() +
                    accountId.blake2b(128);

                val storage = rpc.state.getStorage(storageKey = key)

                if (storage == null || storage == "null") {
                    return null
                }

                return ByteBuffer.wrap(storage.hexToBa()).readU128().toInt()
            }
        }
    }

    class extrinsic {
        companion object {
            /**
             * Send an heartbeat.
             */
            fun heartbeat() {
                val callIndex = byteArrayOf(0x29, 0x03)
                val call = HeartbeatCall(callIndex)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val accountId = CryptoLegacy.getPublicKey().blake2b(256)
                        val multiAddress = MultiAddress(AccountIdentifier.AccountID, accountId)

                        val rpc = RPC(Constants.ACURAST_RPC)
                        val op = Utils.prepareOperation(rpc, accountId)
                        val extrinsic = Utils.prepareExtrinsic(call, op, multiAddress)

                        // Submit extrinsic
                        val txHash = rpc.author.submitExtrinsic(extrinsic)
                        Log.d("Extrinsic", "Transaction payload: ${extrinsic.toHex()}");
                        Log.d("Extrinsic", "Submitted: $txHash");
                    } catch (e: Throwable) {
                        Log.e("Heartbeat", Log.getStackTraceString(e))
                    }
                }
            }

            /**
             * Pair this device with a given manager.
             */
            suspend fun pairWithManager(
                account: AccountId32,
                signature: MultiSignature,
                timestamp: BigInteger
            ): String {
                val callIndex = byteArrayOf(0x29, 0x01)
                val call = PairWithManagerCall(
                    callIndex,
                    pairing = ProcessorPairing(
                        account,
                        proof = Option.some(
                            ProcessorPairing.Proof(
                                signature = signature,
                                timestamp = UInt128(timestamp)
                            )
                        )
                    )
                )

                val accountId = CryptoLegacy.getPublicKey().blake2b(256)
                val multiAddress = MultiAddress(AccountIdentifier.AccountID, accountId)

                val rpc = RPC(Constants.ACURAST_RPC)
                val op = Utils.prepareOperation(rpc, accountId)
                val extrinsic = Utils.prepareExtrinsic(call, op, multiAddress)

                // Submit extrinsic
                val txHash = rpc.author.submitExtrinsic(extrinsic)
                Log.d("Extrinsic", "Transaction payload: ${extrinsic.toHex()}")
                Log.d("Extrinsic", "Submitted: $txHash")
                return txHash
            }

            /**
             * Report the fulfillment of a job.
             */
            fun report(jobId: JobIdentifier, last: Boolean, result: ExecutionResult)  {
                val callIndex = byteArrayOf(0x2b, 0x04)
                val call = ReportCall(callIndex, jobId, last, result)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val accountId = CryptoLegacy.getPublicKey().blake2b(256)
                        val multiAddress = MultiAddress(AccountIdentifier.AccountID, accountId)

                        val rpc = RPC(Constants.ACURAST_RPC)
                        val op = Utils.prepareOperation(rpc, accountId)
                        val extrinsic = Utils.prepareExtrinsic(call, op, multiAddress)

                        // Submit extrinsic
                        val txHash = rpc.author.submitExtrinsic(extrinsic)
                        Log.d("Extrinsic", "Transaction payload: ${extrinsic.toHex()}");
                        Log.d("Extrinsic", "Submitted: $txHash");

                        Notification.notify("Success", "Fulfillment report submitted!")
                    } catch (e: Throwable) {
                        Log.d("Fulfillment Report", Log.getStackTraceString(e))
                        Notification.notify("Error", "Failed to report job fulfillment.")
                    }
                }
            }

            /**
             * Fulfill an assigned job.
             */
            fun fulfill(
                rpcURL: String,
                callIndex: ByteArray,
                script: ByteArray,
                payload: ByteArray
            )  {
                val call = FulfillCall(callIndex, script, payload)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val accountId = CryptoLegacy.getPublicKey().blake2b(256)
                        val multiAddress = MultiAddress(AccountIdentifier.AccountID, accountId)

                        val rpc = RPC(rpcURL)
                        val op = Utils.prepareOperation(rpc, accountId)
                        val extrinsic = Utils.prepareExtrinsic(call, op, multiAddress)

                        // Submit extrinsic
                        val txHash = rpc.author.submitExtrinsic(extrinsic)
                        Log.d("Extrinsic", "Transaction payload: ${extrinsic.toHex()}");
                        Log.d("Extrinsic", "Submitted: $txHash");

                        Notification.notify("Success", "Fulfillment submitted!")
                    } catch (e: Throwable) {
                        Log.d("Fulfill", Log.getStackTraceString(e))
                        Notification.notify("Error", "Failed fulfillment.")
                    }
                }
            }

            /**
             * Submit attestation to validate the mobile device.
             */
            suspend fun submitAttestation(): String {
                val callIndex = byteArrayOf(0x28, 0x05)

                val certs = Attestation.getCertificateChain()
                // The certificate chain must start by the Root certificate.
                val orderedCertificates = certs.map { c -> c.encoded }.reversed()
                val call = SubmitAttestationCall(callIndex, orderedCertificates)

                val accountId = CryptoLegacy.getPublicKey().blake2b(256)
                val multiAddress = MultiAddress(AccountIdentifier.AccountID, accountId)

                val rpc = RPC(Constants.ACURAST_RPC)
                val op = Utils.prepareOperation(rpc, accountId)
                val extrinsic = Utils.prepareExtrinsic(call, op, multiAddress)

                // Submit fulfill extrinsic
                val txHash = rpc.author.submitExtrinsic(extrinsic)
                Log.d("Extrinsic", "Transaction payload: ${extrinsic.toHex()}")
                Log.d("Extrinsic", "Submitted: $txHash")
                return txHash
            }

            /**
             * Advertise the processor resources and their cost.
             */
            fun advertise(rewardAssetId: Int, callback: (Boolean) -> Unit) {
                val monthInMillis = 30 * 24 * 60 * 60 * 1000L

                // TODO : Add UI for configuring these values
                val assetLocation = MultiLocation(
                    parents = 1,
                    interior = JunctionsV1(
                        kind = JunctionsV1.Kind.X3,
                        junctions = listOf(
                            JunctionV1(JunctionV1.Kind.Parachain).setParachain(1000),
                            JunctionV1(JunctionV1.Kind.PalletInstance).setPalletInstance(50),
                            JunctionV1(JunctionV1.Kind.GeneralIndex).setGeneralIndex(BigInteger.valueOf(rewardAssetId.toLong())),
                        )
                    )
                )
                val advertisement = MarketplaceAdvertisement(
                    pricing = listOf(
                        MarketplacePricing(
                            rewardAsset = AssetId(assetLocation),
                            baseFeePerExecution = UInt128(BigInteger.ONE),
                            feePerMillisecond = UInt128(BigInteger.ONE),
                            feePerStorageByte = UInt128(BigInteger.ONE),
                            schedulingWindow = SchedulingWindow(SchedulingWindow.Kind.End, UInt64(System.currentTimeMillis() + monthInMillis))
                        )
                    ),
                    maxMemory = 20_000,
                    storageCapacity = 100_000,
                    networkRequestQuota = 10,
                    allowedConsumers = Option.none()
                )
                val callIndex = byteArrayOf(0x2b, 0x00)
                val call = AdvertiseCall(callIndex, advertisement)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val accountId = CryptoLegacy.getPublicKey().blake2b(256)
                        val multiAddress = MultiAddress(AccountIdentifier.AccountID, accountId)

                        val rpc = RPC(Constants.ACURAST_RPC)
                        val op = Utils.prepareOperation(rpc, accountId)
                        val extrinsic = Utils.prepareExtrinsic(call, op, multiAddress)

                        // Submit extrinsic
                        val txHash = rpc.author.submitExtrinsic(extrinsic)
                        Log.d("Extrinsic", "Transaction payload: ${extrinsic.toHex()}")
                        Log.d("Extrinsic", "Submitted: $txHash")

                        callback(true)
                    } catch (e: Throwable) {
                        Log.e("Resource Advertisement", Log.getStackTraceString(e))
                        Notification.notify("Error", "Failed to advertise resources.")
                        callback(false);
                    }
                }
            }

            /**
             * Acknowledge a job assignment.
             */
            fun acknowledgeJob(jobId: JobIdentifier) {
                val callIndex = byteArrayOf(0x2b, 0x03)
                val call = AcknowledgeMatchCall(callIndex, jobId)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val accountId = CryptoLegacy.getPublicKey().blake2b(256)
                        val multiAddress = MultiAddress(AccountIdentifier.AccountID, accountId)

                        val rpc = RPC(Constants.ACURAST_RPC)
                        val op = Utils.prepareOperation(rpc, accountId)

                        val extrinsic = Utils.prepareExtrinsic(call, op, multiAddress)

                        // Submit extrinsic
                        val txHash = rpc.author.submitExtrinsic(extrinsic)
                        Log.d("Substrate", "Transaction payload: ${extrinsic.toHex()}");
                        Log.d("Substrate", "Submitted extrinsic: $txHash")

                        Notification.notify("Success", "Job acknowledged!")
                    } catch (e: Throwable) {
                        Log.e("Job acknowledgment", Log.getStackTraceString(e))
                        Notification.notify("Error", "Failed to acknowledge job.")
                    }
                }
            }
        }
    }
}