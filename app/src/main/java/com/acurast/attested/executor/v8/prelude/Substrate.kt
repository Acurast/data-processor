package com.acurast.attested.executor.v8.prelude

import acurast.codec.extensions.hexToBa
import android.util.Log
import com.acurast.attested.executor.protocol.acurast.AcurastRPC
import com.eclipsesource.v8.JavaVoidCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Object

class Substrate {
    companion object {
        fun prelude(v8: V8, rootNamespace: V8Object, ipfsHash: ByteArray): V8Object {
            val obj = V8Object(v8)
            obj.registerJavaMethod(fulfill(ipfsHash), "fulfill")
            rootNamespace.add("substrate", obj)
            return obj
        }

        private fun fulfill(ipfsHash: ByteArray): JavaVoidCallback {
            return JavaVoidCallback { _, parameters ->
                Log.d("v8", "fulfill called for ${String(ipfsHash)}")

                val rpcURL = when (val url = parameters[0]) {
                    is String -> url
                    else -> throw IllegalArgumentException()
                }

                val callIndex = when (val callIndex = parameters[1]) {
                    is String -> callIndex.hexToBa()
                    else -> throw IllegalArgumentException()
                }

                val payload = when (val payload = parameters[2]) {
                    is String -> payload.hexToBa()
                    else -> throw IllegalArgumentException()
                }

                Log.d("v8", "fulfill called for ${parameters[0]}")
                AcurastRPC.extrinsic.fulfill(rpcURL, callIndex, ipfsHash, payload)
            }
        }
    }
}