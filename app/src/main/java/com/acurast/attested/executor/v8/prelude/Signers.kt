package com.acurast.attested.executor.v8.prelude

import acurast.codec.extensions.hexToBa
import com.acurast.attested.executor.App
import com.acurast.attested.executor.crypto.ICrypto
import com.acurast.attested.executor.utils.toHex
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.*

class Signers {
    companion object {
        fun prelude(v8: V8, rootNamespace: V8Object): V8Object {
            val obj = V8Object(v8)

            val secp256r1Object = V8Object(v8)
            secp256r1Object.registerJavaMethod(encrypt(App.Signer.SECP_256_R1), "encrypt")
            secp256r1Object.registerJavaMethod(decrypt(App.Signer.SECP_256_R1), "decrypt")
            obj.add("secp256r1", secp256r1Object)

            val secp256k1Object = V8Object(v8)
            secp256k1Object.registerJavaMethod(encrypt(App.Signer.SECP_256_K1), "encrypt")
            secp256k1Object.registerJavaMethod(decrypt(App.Signer.SECP_256_K1), "decrypt")
            obj.add("secp256k1", secp256k1Object)

            rootNamespace.add("signers", obj)
            return obj
        }

        fun encrypt(ICrypto: ICrypto): JavaCallback {
            return JavaCallback { _, parameters ->
                val publicKey = when (val publicKey = parameters[0]) {
                    is String -> publicKey.hexToBa()
                    else -> throw IllegalArgumentException()
                }
                val sharedSecretSalt = when (val sharedSecretSalt = parameters[1]) {
                    is String -> sharedSecretSalt.hexToBa()
                    else -> throw IllegalArgumentException()
                }
                val payload = when (val payload = parameters[2]) {
                    is String -> payload.hexToBa()
                    else -> throw IllegalArgumentException()
                }

                val encrypted = ICrypto.rawStreamEncrypt(publicKey, sharedSecretSalt, payload)

                return@JavaCallback encrypted.toHex()
            }
        }

        fun decrypt(ICrypto: ICrypto): JavaCallback {
            return JavaCallback { _, parameters ->
                val publicKey = when (val publicKey = parameters[0]) {
                    is String -> publicKey.hexToBa()
                    else -> throw IllegalArgumentException()
                }
                val sharedSecretSalt = when (val sharedSecretSalt = parameters[1]) {
                    is String -> sharedSecretSalt.hexToBa()
                    else -> throw IllegalArgumentException()
                }
                val encrypted = when (val encrypted = parameters[2]) {
                    is String -> encrypted.hexToBa()
                    else -> throw IllegalArgumentException()
                }

                val decrypted = ICrypto.rawStreamEncrypt(publicKey, sharedSecretSalt, encrypted)

                return@JavaCallback decrypted.toHex()
            }
        }
    }
}