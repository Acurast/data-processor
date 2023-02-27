package com.acurast.attested.executor.crypto.curves

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.acurast.attested.executor.*
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.crypto.ICrypto
import com.acurast.attested.executor.native.SB_CURVE
import com.acurast.attested.executor.native.SweetBSigner
import com.google.crypto.tink.aead.subtle.AesGcmSiv
import com.google.crypto.tink.subtle.Hkdf
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class GenericEcdsa(curve: SB_CURVE) : ICrypto {
    var sweetBSigner = SweetBSigner(curve)

    fun getOrCreatePrivateKey(returnKey: Boolean = true): ByteArray {
        val keyStore: KeyStore = KeyStore.getInstance(Constants.ANDROID_KEYSTORE_ALIAS)
        keyStore.load(null)
        val keyFile = File(App.context.filesDir, Constants.KEY_FILE)
        val cipher = Cipher.getInstance(Constants.ENCRYPTION_ALGORITHM)
        try {
            if (!keyFile.exists()) {
                val fileOutputStream = keyFile.outputStream()
                val keyGenerator: KeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, Constants.ANDROID_KEYSTORE_ALIAS)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    Constants.ENCRYPTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(Constants.ENCRYPTION_KEY_SIZE)
                    .setIsStrongBoxBacked(true)

                val encryptionKey: SecretKey = try {
                    keyGenerator.init(
                        keyGenParameterSpec.build()
                    )
                    keyGenerator.generateKey()
                } catch (e: StrongBoxUnavailableException) {
                    keyGenParameterSpec.setIsStrongBoxBacked(false)
                    keyGenerator.init(
                        keyGenParameterSpec.build()
                    )
                    keyGenerator.generateKey()
                }
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
                val privateKey = CryptoLegacy.secureRandomBytes()
                val publicKey = this.sweetBSigner.computePublicKey(privateKey)
                fileOutputStream.write(publicKey)
                fileOutputStream.write(cipher.iv)
                fileOutputStream.write(cipher.doFinal(privateKey))
                fileOutputStream.close()

                return privateKey
            } else if(returnKey) {
                val encryptedPayload = keyFile.inputStream().readBytes()
                val entry = keyStore.getEntry(Constants.ENCRYPTER_KEY_ALIAS, null) as SecretKeyEntry
                val spec = GCMParameterSpec(
                    Constants.ENCRYPTION_BLOCK_SIZE,
                    encryptedPayload.sliceArray(64..75)
                )
                cipher.init(Cipher.DECRYPT_MODE, entry.secretKey, spec)
                return cipher.doFinal(encryptedPayload.sliceArray(76..123))
            }
        } catch (e: Exception) {
            // TODO: I think the line below should be removed
            // We should not delete an already generated key is something goes wrong unexpectedly.
            // Only when we are creating it, but never when it was already created.
            keyFile.delete() // making this atomic
        }
        return ByteArray(0)
    }

    override fun getPublicKey(compressed: Boolean): ByteArray {
        getOrCreatePrivateKey(returnKey = false)
        val publicKey = File(App.context.filesDir, Constants.KEY_FILE).inputStream().readBytes().sliceArray(0..63)
        return if(compressed) this.sweetBSigner.compressPublicKey(publicKey) else publicKey
    }

    override fun rawSign(payload: ByteArray): ByteArray {
        val privateKey = this.getOrCreatePrivateKey()
        // TODO: uncomment
        // return this.sweetBSigner.signMessageDigest(privateKey, payload)
        return Ecdsa(Curve.SECP_256_K1).sign(privateKey, payload)
    }

    override fun rawStreamDecrypt(
        senderPublicKeyBytes: ByteArray,
        sharedSecretSalt: ByteArray,
        encryptedPayload: ByteArray
    ): ByteArray {
        val key = this.generateSharedKey(senderPublicKeyBytes, sharedSecretSalt)
        return key.decrypt(encryptedPayload, byteArrayOf())
    }

    override fun rawStreamEncrypt(
        recipientPublicKeyBytes: ByteArray,
        sharedSecretSalt: ByteArray,
        payload: ByteArray
    ): ByteArray {
        val key = this.generateSharedKey(recipientPublicKeyBytes, sharedSecretSalt)
        return key.encrypt(payload, byteArrayOf())
    }

    /**
     * Reference: https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec#example:ecdh
     */
    private fun generateSharedKey(
        ephemeralPublicKeyBytes: ByteArray,
        sharedSecretSalt: ByteArray,
    ): AesGcmSiv {
        val privateKey = this.getOrCreatePrivateKey(returnKey = true)
        val sharedSecret = this.sweetBSigner.generateSharedSecret(privateKey, ephemeralPublicKeyBytes)

        // sharedSecret cannot safely be used as a key yet. We must run it through a key derivation
        // function with some other data: "salt" and "info". Salt is an optional random value,
        // omitted in this example. It's good practice to include both public keys and any other
        // key negotiation data in info. Here we use the public keys and a label that indicates
        // messages encrypted with this key are coming from the server.
        val info = ByteArrayOutputStream()
        info.write("ECDH ${this.sweetBSigner.curve.name} AES-256-GCM-SIV".toByteArray(
            StandardCharsets.UTF_8))
        info.write(this.getPublicKey())
        info.write(ephemeralPublicKeyBytes)

        // Generate key by applying HKDF key derivation function.
        return AesGcmSiv(
            Hkdf.computeHkdf(
                "HMACSHA256", sharedSecret, sharedSecretSalt, info.toByteArray(), 32
            )
        )
    }
}