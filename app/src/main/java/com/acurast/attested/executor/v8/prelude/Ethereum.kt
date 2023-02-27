package com.acurast.attested.executor.v8.prelude

import android.os.Bundle
import android.util.Log
import com.acurast.attested.executor.App
import com.eclipsesource.v8.*

class Ethereum {
    companion object {
        fun prelude(v8: V8, rootNamespace: V8Object, ipfsHash: ByteArray): V8Object {
            val obj = V8Object(v8)
            obj.registerJavaMethod(fulfill(ipfsHash), "fulfill")
            rootNamespace.add("ethereum", obj)
            return obj
        }

        private fun fulfill(ipfsHash: ByteArray): JavaVoidCallback {
            return JavaVoidCallback { receiver, parameters ->
                val url = parameters[0]
                val destination = parameters[1]
                val payload = parameters[2]
                val extra = parameters[3]
                val successCallback = when(val callback = parameters[4]) {
                    is V8Function -> callback
                    else -> throw IllegalArgumentException()
                }
                val errorCallback = when(val callback = parameters[5]) {
                    is V8Function -> callback
                    else -> throw IllegalArgumentException()
                }
                Log.d("Ethereum", "fulfill called for ${String(ipfsHash)}")

                val context = Bundle()
                context.putByteArray("jobIdentifier", ipfsHash)

                when (url) {
                    is String -> context.putString("rpc", url)
                    else -> throw IllegalArgumentException()
                }

                when (destination) {
                    is String -> context.putString("destination", destination)
                    else -> throw IllegalArgumentException()
                }

                when (extra) {
                    is V8Object -> {
                        for (key in extra.keys) {
                            when (val value = extra.get(key)) {
                                is String -> context.putString(key, value)
                                is Int -> context.putInt(key, value)
                                is Boolean -> context.putBoolean(key, value)
                                else -> throw IllegalArgumentException()
                            }
                        }
                    }
                    else -> throw IllegalArgumentException()
                }

                val onSuccess: (String) -> Unit = {
                    Log.d("Ethereum", "fulfill result: $it")
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(it)
                    successCallback.call(receiver.runtime, resultV8Array)
                }
                val onError: (Throwable) -> Unit = {
                    Log.d("Ethereum", "fulfill failed: ${it.message}")
                    val resultV8Array = V8Array(receiver.runtime)
                    resultV8Array.push(it.message)
                    errorCallback.call(receiver.runtime, resultV8Array)
                }
                App.Protocol.ethereum.fulfill(context, payload, onSuccess, onError)
            }
        }
    }
}