package com.acurast.attested.executor.crypto.curves

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.acurast.attested.executor.*
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.crypto.ICrypto
import com.acurast.attested.executor.utils.trim
import com.google.crypto.tink.aead.subtle.AesGcmSiv
import com.google.crypto.tink.subtle.Hkdf
import com.rfksystems.blake2b.Blake2b
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPublicKeySpec
import java.security.spec.InvalidKeySpecException
import javax.crypto.KeyAgreement


class P256 : ICrypto {

    private fun getOrCreateKeyPair(): KeyPair {
        val signerKeyAlias = Constants.SIGNER_KEY_ALIAS
        val keyStore: KeyStore = KeyStore.getInstance(Constants.ANDROID_KEYSTORE_ALIAS)
        keyStore.load(null)

        if (!keyStore.containsAlias(signerKeyAlias)) {
            val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, Constants.ANDROID_KEYSTORE_ALIAS
            )
            val keyGenParameterSpec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                KeyGenParameterSpec.Builder(
                    signerKeyAlias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_AGREE_KEY
                )
            } else {
                KeyGenParameterSpec.Builder(signerKeyAlias, KeyProperties.PURPOSE_SIGN)
            }.setAlgorithmParameterSpec(ECGenParameterSpec(Constants.SECP256R1)).setDigests(
                KeyProperties.DIGEST_NONE
            ).setAttestationChallenge(CryptoLegacy.secureRandomBytes())

            keyPairGenerator.initialize(
                keyGenParameterSpec.build()
            )

            val keyPair = try {
                keyPairGenerator.initialize(
                    keyGenParameterSpec.build()
                )
                keyPairGenerator.generateKeyPair()
            } catch (e: StrongBoxUnavailableException) {
                keyGenParameterSpec.setIsStrongBoxBacked(false)
                keyPairGenerator.initialize(
                    keyGenParameterSpec.build()
                )
                keyPairGenerator.generateKeyPair()
            }
            return keyPair
        } else {
            val entry = keyStore.getEntry(signerKeyAlias, null)
            val privateKeyEntry = (entry as KeyStore.PrivateKeyEntry)
            return KeyPair(privateKeyEntry.certificate.publicKey, privateKeyEntry.privateKey)
        }
    }

    private fun getPrivateKey(): PrivateKey {
        return getOrCreateKeyPair().private
    }

    override fun getPublicKey(compressed: Boolean): ByteArray {
        val publicKey = getOrCreateKeyPair().public as ECPublicKey
        val ecNamedCurveTable = ECNamedCurveTable.getParameterSpec(Constants.SECP256R1)
        val ecPoint = ecNamedCurveTable.curve.createPoint(publicKey.w.affineX, publicKey.w.affineY)
        return ecPoint.getEncoded(compressed)
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

    override fun rawSign(payload: ByteArray): ByteArray {
        val privateKey = this.getPrivateKey()

        val signature = Signature.getInstance(Constants.NONE_WITH_ECDSA)
        signature.initSign(privateKey)

        val messageDigest = Blake2b(256)
        messageDigest.update(payload, 0, payload.size)

        val messageBlake2bHash = ByteArray(32)
        messageDigest.digest(messageBlake2bHash, 0)
        signature.update(messageBlake2bHash)

        val signaturePayload = signature.sign()

        val asN1InputStream = ASN1InputStream(ByteArrayInputStream(signaturePayload))
        val asn1Sequence = asN1InputStream.readObject() as ASN1Sequence

        val r = asn1Sequence.getObjectAt(0) as ASN1Integer
        val s = asn1Sequence.getObjectAt(1) as ASN1Integer

        return r.value.toByteArray().trim() + s.value.toByteArray().trim()
    }

    /**
     * Reference: https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec#example:ecdh
     */
    private fun generateSharedKey(
        ephemeralPublicKeyBytes: ByteArray,
        sharedSecretSalt: ByteArray,
    ): AesGcmSiv {
        val keyPair = this.getOrCreateKeyPair()

        val ephemeralPublicKey = getPublicKeyFromBytes(ephemeralPublicKeyBytes)

        // Create a shared secret based on our private key and the other party's public key.
        val keyAgreement: KeyAgreement =
            KeyAgreement.getInstance("ECDH", Constants.ANDROID_KEYSTORE_ALIAS)
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(ephemeralPublicKey, true)
        val sharedSecret: ByteArray = keyAgreement.generateSecret()

        // sharedSecret cannot safely be used as a key yet. We must run it through a key derivation
        // function with some other data: "salt" and "info". Salt is an optional random value,
        // omitted in this example. It's good practice to include both public keys and any other
        // key negotiation data in info. Here we use the public keys and a label that indicates
        // messages encrypted with this key are coming from the server.
        val info = ByteArrayOutputStream()
        info.write(
            "ECDH ${Constants.SECP256R1} AES-256-GCM-SIV".toByteArray(
                StandardCharsets.UTF_8
            )
        )
        info.write(this.getPublicKey())
        info.write(ephemeralPublicKeyBytes)

        // Generate key by applying HKDF key derivation function.
        return AesGcmSiv(
            Hkdf.computeHkdf(
                "HMACSHA256", sharedSecret, sharedSecretSalt, info.toByteArray(), 32
            )
        )
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getPublicKeyFromBytes(pubKey: ByteArray): PublicKey {
        val spec = ECNamedCurveTable.getParameterSpec(Constants.SECP256R1)
        val factory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider())
        val params = ECNamedCurveSpec(Constants.SECP256R1, spec.curve, spec.g, spec.n)
        val point = ECPointUtil.decodePoint(params.curve, pubKey)
        val pubKeySpec = ECPublicKeySpec(point, params)
        return factory.generatePublic(pubKeySpec) as ECPublicKey
    }
}