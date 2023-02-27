package com.acurast.attested.executor.v8.prelude

import android.os.Bundle
import android.util.Log
import com.acurast.attested.executor.App
import com.eclipsesource.v8.JavaVoidCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Object

class Tezos {
    companion object {
        fun prelude(v8: V8, rootNamespace: V8Object, ipfsHash: ByteArray): V8Object {
            val obj = V8Object(v8)
            obj.registerJavaMethod(fulfill(ipfsHash), "fulfill")
            rootNamespace.add("tezos", obj)
            return obj
        }

        private fun fulfill(ipfsHash: ByteArray): JavaVoidCallback {
            return JavaVoidCallback { _, parameters ->
                Log.d("Tezos", "fulfill called for ${String(ipfsHash)}")

                val context = Bundle()
                context.putByteArray("jobIdentifier", ipfsHash)

                // TODO: rpc currently is ignored for Tezos
                when (val url = parameters[0]) {
                    is String -> context.putString("rpc", url)
                }

                when (val destination = parameters[1]) {
                    is String -> context.putString("destination", destination)
                }

                val payload = parameters[2]

                when (val extra = parameters[3]) {
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
                    Log.d("Tezos", "fulfill result: $it")
                }
                val onError: (Throwable) -> Unit = {
                    Log.e("Tezos", "fulfill failed: $it")
                }
                App.Protocol.tezos.fulfill(context, payload, onSuccess, onError)
            }
        }
    }
}