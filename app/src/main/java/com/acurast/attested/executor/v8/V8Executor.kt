package com.acurast.attested.executor.v8

import acurast.codec.extensions.toHex
import acurast.codec.type.AccountId32
import acurast.codec.type.acurast.JobIdentifier
import acurast.codec.type.marketplace.ExecutionResult
import android.os.Bundle
import android.util.Log
import com.acurast.attested.executor.App
import com.acurast.attested.executor.Constants
import com.eclipsesource.v8.*
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.protocol.acurast.AcurastRPC
import com.acurast.attested.executor.protocol.tezos.toMicheline
import com.acurast.attested.executor.utils.*
import com.acurast.attested.executor.v8.prelude.Ethereum
import com.acurast.attested.executor.v8.prelude.Signers
import com.acurast.attested.executor.v8.prelude.Substrate
import com.acurast.attested.executor.v8.prelude.Tezos
import it.airgap.tezos.michelson.packer.packToBytes
import java.io.UnsupportedEncodingException
import java.net.URL

class V8Executor(
    private val ipfsHash: ByteArray,
    private val script: String,
    private val extras: Bundle
) {
    /**
     * V8 method to send HTTP (GET) requests from javascript
     */
    private val httpGETJavaVoidCallback = JavaVoidCallback { receiver, parameters ->
        val rawUrl = parameters[0]
        val rawHeaders = parameters[1]
        val successCallback = parameters[2]
        val errorCallback = parameters[3]

        if (rawUrl is String && rawHeaders is V8Object && successCallback is V8Function && errorCallback is V8Function) {
            val url = URL(rawUrl)
            val headers = HashMap<String, String>()
            for (key in rawHeaders.keys) {
                val objectValue = rawHeaders[key]
                when (objectValue) {
                    is String -> headers[key] = objectValue
                    else -> throw UnsupportedEncodingException()
                }
            }
            Networking.httpsGetString(url, headers, { payload, certificatePin ->
                V8Utils.withV8Lock(receiver) {
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(payload)
                    resultV8Array.push(certificatePin.toHex())
                    Log.d("hex", "${url}: ${certificatePin.toHex()}")
                    successCallback.call(receiver.runtime, resultV8Array)
                }
            }, {
                V8Utils.withV8Lock(receiver) {
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(it.message)
                    errorCallback.call(receiver.runtime, resultV8Array)
                }
            })
        } else {
            throw IllegalArgumentException()
        }
    }

    /**
     * V8 method to send HTTP (POST) requests from javascript
     */
    private val httpPOSTJavaVoidCallback = JavaVoidCallback { receiver, parameters ->
        val rawUrl = parameters[0]
        val body = parameters[1]
        val rawHeaders = parameters[2]
        val successCallback = parameters[3]
        val errorCallback = parameters[4]

        if (rawUrl is String && body is String && rawHeaders is V8Object && successCallback is V8Function && errorCallback is V8Function) {
            val url = URL(rawUrl)

            val headers = HashMap<String, String>()
            for (key in rawHeaders.keys) {
                val objectValue = rawHeaders[key]
                when (objectValue) {
                    is String -> headers[key] = objectValue
                    else -> throw UnsupportedEncodingException()
                }
            }

            Networking.httpsPostString(url, body, headers, { payload, certificatePin ->
                V8Utils.withV8Lock(receiver) {
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(payload)
                    resultV8Array.push(certificatePin.toHex())
                    successCallback.call(receiver, resultV8Array)
                }
            }, {
                V8Utils.withV8Lock(receiver) {
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(it.message)
                    errorCallback.call(receiver.runtime, resultV8Array)
                }
            })
        } else {
            throw IllegalArgumentException()
        }
    }

    /**
     * V8 method to attest the device from javascript
     */
    private val attestJavaVoidCallback = JavaCallback { receiver, parameters ->
        val nonce = parameters[0]
        val successCallback = parameters[1]
        val errorCallback = parameters[2]

        if (nonce is String && successCallback is V8Function && errorCallback is V8Function) {
            Scheduler.sendSafetyNetRequest(App.context, nonce.toByteArray(), {
                V8Utils.withV8Lock(receiver) {
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(it)
                    successCallback.call(receiver.runtime, resultV8Array)
                }
            }, {
                V8Utils.withV8Lock(receiver) {
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(it.message)
                    errorCallback.call(receiver.runtime, resultV8Array)
                }
            })
        } else {
            throw IllegalArgumentException()
        }
    }

    /**
     * V8 method to pack Micheline values from javascript
     */
    @Deprecated("This callback is deprecated. `tezos` object should be used instead")
    private val packJavaCallback = JavaCallback { _, parameters ->
        val payload = parameters[0]
        if (payload is V8Object) {
            return@JavaCallback payload.toMicheline().packToBytes().toHex()
        } else {
            throw IllegalArgumentException()
        }
    }

    /**
     * V8 method to get the Tezos address of the processor from javascript
     */
    @Deprecated("This callback is deprecated. `tezos` object should be used instead")
    private val ownAddressCallback = JavaCallback { _, _ ->
        return@JavaCallback App.Protocol.tezos.getAddress()
    }

    /**
     * V8 method for logging from javascript
     */
    private val printJavaVoidCallback = JavaVoidCallback { _, parameters ->
        if (parameters.length() > 0) {
            val arg1 = parameters[0]
            Log.d("v8", "${String(ipfsHash)} ${arg1.toString()}")

            if (arg1 is Releasable) {
                arg1.release()
            }
        }
    }

    /**
     * V8 method for generating random bytes from javascript
     */
    private val randomJavaVoidCallback = JavaCallback { _, _ ->
        return@JavaCallback CryptoLegacy.secureRandomBytes().toHex()
    }

    /**
     * V8 method for submitting a Job fulfillment from javascript
     */
    @Deprecated("This callback is deprecated. `tezos` object should be used instead")
    private val fulfillJavaVoidCallback = JavaVoidCallback { _, parameters ->
        Log.d("v8", "fulfill called for ${String(ipfsHash)}")

        val onSuccess: (String) -> Unit = {
            Log.d("v8", "Fulfill operation hash: $it")
        }
        val onError: (Throwable) -> Unit = {
            Log.d("Error", "$it")
        }

        val context = Bundle(extras.deepCopy())
        context.putByteArray("jobIdentifier", ipfsHash)

        App.Protocol.tezos.fulfill(context, parameters[0], onSuccess, onError)
    }

    private val environmentCallback = JavaCallback { _, parameters ->
        val environmentKey = parameters[0]
        return@JavaCallback App.readSharedPreferencesString(
            "${Constants.ENVIRONMENT_KEY_PREFIX}${environmentKey}",
            ""
        )
    }

    /**
     * Execute Job script
     */
    fun execute(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        Thread {
            var failed = false
            try {
                val v8: V8 = V8.createV8Runtime()
                val v8RootNamespace = V8Object(v8)

                // APP information
                val v8AppInfo = V8Object(v8)
                v8AppInfo.add("version", Constants.PROCESSOR_VERSION)
                v8RootNamespace.add("app_info", v8AppInfo)

                val v8JobInfo = V8Object(v8)
                v8RootNamespace.add("job_info", v8JobInfo)

                // Random primitives
                val v8Random = V8Object(v8)
                v8Random.registerJavaMethod(randomJavaVoidCallback, "generateSecureRandomHex")
                v8RootNamespace.add("random", v8Random)

                val chainsObject = V8Object(v8)
                val ethereumObject = Ethereum.prelude(v8, chainsObject, ipfsHash)
                val substrateObject = Substrate.prelude(v8, chainsObject, ipfsHash)
                val tezosObject = Tezos.prelude(v8, chainsObject, ipfsHash)
                v8RootNamespace.add("chains", chainsObject)

                val signersObject = Signers.prelude(v8, v8RootNamespace)
                v8.add("_STD_", v8RootNamespace)

                v8.registerJavaMethod(printJavaVoidCallback, "print")
                v8.registerJavaMethod(attestJavaVoidCallback, "attest")
                v8.registerJavaMethod(ownAddressCallback, "ownAddress")
                v8.registerJavaMethod(packJavaCallback, "pack")
                v8.registerJavaMethod(httpGETJavaVoidCallback, "httpGET")
                v8.registerJavaMethod(httpPOSTJavaVoidCallback, "httpPOST")
                v8.registerJavaMethod(fulfillJavaVoidCallback, "fulfill")
                v8.registerJavaMethod(randomJavaVoidCallback, "generateSecureRandomHex")
                v8.registerJavaMethod(environmentCallback, "environment")

                try {
                    val startTime = System.currentTimeMillis()

                    v8.executeVoidScript(script)

                    Log.d("v8", "${String(ipfsHash)} used millis: ${System.currentTimeMillis() - startTime}")
                } catch (e: Exception) {
                    Log.d("v8", "${String(ipfsHash)} got inner exception: $e")
                    failed = true
                    onError(e)
                } finally {
                    v8.locker.release()
                }

                Thread.sleep(Constants.EXECUTION_TIME_LIMIT)
                v8.terminateExecution()
                while (!v8.locker.tryAcquire()) {
                    Thread.sleep(Constants.ACQUIRE_SLEEP)
                }

                v8AppInfo.close()
                v8JobInfo.close()
                v8Random.close()
                ethereumObject.close()
                substrateObject.close()
                tezosObject.close()
                signersObject.close()
                v8RootNamespace.close()
                v8.release(false)
            } catch (e: Exception) {
                Log.d("v8", "got outer exception: ${e.message}")
                failed = true
                onError(e)
            } finally {
                val requester = extras.getByteArray("requester")
                val script = extras.getString("ipfsUri", "")
                val isLastReport = extras.getBoolean("isLastReport", false)
                if (requester != null && requester.isNotEmpty()) {
                    // TODO : Include diagnostics on fulfillment failures.
                    val jobId = JobIdentifier(
                        requester = AccountId32(requester),
                        script = script.toByteArray(charset = Charsets.UTF_8)
                    )
                    AcurastRPC.extrinsic.report(jobId, isLastReport, ExecutionResult.success(byteArrayOf()))
                }

                if (!failed) {
                    onSuccess()
                }
            }
        }.start()
    }
}
