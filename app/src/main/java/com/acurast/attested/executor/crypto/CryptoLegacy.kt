package com.acurast.attested.executor.crypto

import acurast.codec.extensions.blake2b
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.crypto.curves.Secp256r1
import com.acurast.attested.executor.utils.Encoding
import com.acurast.attested.executor.utils.hexStringToByteArray
import com.acurast.attested.executor.utils.trim
import com.rfksystems.blake2b.Blake2b
import it.airgap.tezos.core.crypto.CryptoProvider
import it.airgap.tezos.michelson.micheline.dsl.builder.expression.Pair
import it.airgap.tezos.michelson.micheline.dsl.micheline
import it.airgap.tezos.michelson.packer.packToBytes
import it.airgap.tezos.operation.Operation
import it.airgap.tezos.operation.coder.forgeToBytes
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.jce.ECNamedCurveTable
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class CryptoLegacy {
    companion object {
        val SPSIG_PREFIX = "36f02c34".hexStringToByteArray()

        /**
         * Get android keystore provider.
         *
         * @see <a href="https://developer.android.com/training/articles/keystore">Use Android keystore provider</a>
         */
        fun getKeyStore(): KeyStore {
            val keyStore: KeyStore = KeyStore.getInstance(Constants.KEYSTORE_PROVIDER)
            keyStore.load(null)
            return keyStore
        }

        /**
         * Generate key pair for the processor if it does not exist.
         *
         * @see <a href="https://developer.android.com/reference/java/security/KeyPairGenerator">KeyPairGenerator</a>
         */
        fun generateProcessorKeyIfNecessary() {
            // Get the android keystore provider
            val keyStore = getKeyStore()

            if (!keyStore.containsAlias(Constants.SIGNER_KEY_ALIAS)) {
                val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, Constants.ANDROID_KEYSTORE_ALIAS
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    Constants.SIGNER_KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_AGREE_KEY
                ).setAlgorithmParameterSpec(ECGenParameterSpec(Constants.SECP256R1)).setDigests(
                    KeyProperties.DIGEST_NONE
                ).setAttestationChallenge(this.secureRandomBytes())

                keyPairGenerator.initialize(
                    keyGenParameterSpec.build()
                )

                try {
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
            }
        }

        fun secureRandomBytes(): ByteArray{
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                Constants.RANDOM_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT
            )
                .setIsStrongBoxBacked(true)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

            val generator: KeyGenerator =
                KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    Constants.ANDROID_KEYSTORE_ALIAS
                )

            val key: SecretKey = try {
                generator.init(
                    keyGenParameterSpec.build()
                )
                generator.generateKey()
            } catch (e: StrongBoxUnavailableException) {
                keyGenParameterSpec.setIsStrongBoxBacked(false)
                generator.init(
                    keyGenParameterSpec.build()
                )
                generator.generateKey()
            }

            val cipher = Cipher.getInstance(Constants.ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return MessageDigest.getInstance(Constants.ENTROPY_EXTRACTOR_HASH)
                .digest(cipher.doFinal(ByteArray(32)))
        }

        fun signWithPrefix(chainId: ByteArray, script: String, payload: ByteArray): String {
            val scriptBytes = script.toByteArray(charset = Charsets.UTF_8)
            val scriptDigest = Blake2b(256)
            scriptDigest.update(scriptBytes, 0, scriptBytes.size)
            val scriptBlake2bHash = ByteArray(32)
            scriptDigest.digest(scriptBlake2bHash, 0)

            return sign(
                micheline {
                    Pair {
                        arg { bytes(chainId) }
                        arg {
                            Pair {
                                arg { bytes(scriptBlake2bHash) }
                                arg { bytes(payload) }
                            }
                        }
                    }
                }.packToBytes()
            )
        }

        fun sign(payload: ByteArray): String {
            return Encoding.base58CheckEncode(
                SPSIG_PREFIX + rawSign(payload)
            )
        }

        fun rawSign(payload: ByteArray): ByteArray {
            val keyStore = getKeyStore()

            val entry = keyStore.getEntry(Constants.PROCESSOR_KEY_ALIAS, null)
            val privateKey = (entry as KeyStore.PrivateKeyEntry).privateKey

            val messageDigest = Blake2b(256)
            messageDigest.update(payload, 0, payload.size)

            val messageBlake2bHash = ByteArray(32)
            messageDigest.digest(messageBlake2bHash, 0)

            return ecSign(messageBlake2bHash, privateKey)
        }

        fun ecSign(payload: ByteArray, privateKey: PrivateKey): ByteArray {
            val signaturePayload = Signature.getInstance(Constants.NONE_WITH_ECDSA).apply {
                initSign(privateKey)
                update(payload)
            }.sign()

            val asN1InputStream = ASN1InputStream(ByteArrayInputStream(signaturePayload))
            val asn1Sequence = asN1InputStream.readObject() as ASN1Sequence

            val r = asn1Sequence.getObjectAt(0) as ASN1Integer
            val s = asn1Sequence.getObjectAt(1) as ASN1Integer

            return r.value.toByteArray().trim() + s.value.toByteArray().trim()
        }

        fun acurastSign(payload: ByteArray): ByteArray {
            val keyStore = getKeyStore()

            val entry = keyStore.getEntry(Constants.PROCESSOR_KEY_ALIAS, null)
            val privateKey = (entry as KeyStore.PrivateKeyEntry).privateKey
            val publicKey = getPublicKey();

            val signature = Signature.getInstance(Constants.NONE_WITH_ECDSA)
            signature.initSign(privateKey)

            // If the payload has more than 256 bytes, it needs to be hashed with blake2b
            val messageHash = if (payload.size > 256) {
                DigestUtils.sha256(payload.blake2b(256))
            } else {
                DigestUtils.sha256(payload);
            }

            signature.update(messageHash)

            val signaturePayload = signature.sign();

            val asN1InputStream = ASN1InputStream(ByteArrayInputStream(signaturePayload))
            val asn1Sequence = asN1InputStream.readObject() as ASN1Sequence

            val r = (asn1Sequence.getObjectAt(0) as ASN1Integer).value.toByteArray()
            val s = (asn1Sequence.getObjectAt(1) as ASN1Integer).value.toByteArray()
            val v = Secp256r1.findRecoverId(
                r,
                s,
                publicKey,
                messageHash
            )

            return r.trim() + s.trim() + byteArrayOf(v)
        }

        fun signOperation(operation: Operation): ByteArray = rawSign(byteArrayOf(3) + operation.forgeToBytes())

        fun getPublicKey(): ByteArray {
            val keyStore = getKeyStore()
            val publicKey =
                keyStore.getCertificate(Constants.PROCESSOR_KEY_ALIAS).publicKey as ECPublicKey
            val ecNamedCurveTable = ECNamedCurveTable.getParameterSpec(Constants.EC_CURVE)
            val ecPoint =
                ecNamedCurveTable.curve.createPoint(publicKey.w.affineX, publicKey.w.affineY)
            return ecPoint.getEncoded(true)
        }

        fun hashInputstream(digest: MessageDigest, data: InputStream): ByteArray {
            val buffer = ByteArray(Constants.STREAM_BUFFER_LENGTH)
            var read = data.read(buffer, 0, Constants.STREAM_BUFFER_LENGTH)
            while (read > -1) {
                digest.update(buffer, 0, read)
                read = data.read(buffer, 0, Constants.STREAM_BUFFER_LENGTH)
            }
            return digest.digest()
        }
    }

    class Tezos : CryptoProvider {
        override fun blake2b(message: ByteArray, size: Int): ByteArray {
            if (size !in setOf(20, 32, 48, 64)) throw IllegalArgumentException("Invalid BLAKE2b hash size (supported: 20, 32, 48 or 64).")

            val messageDigest = Blake2b(size * 8)
            messageDigest.update(message, 0, message.size)

            return ByteArray(size).also { messageDigest.digest(it, 0) }
        }

        override fun sha256(message: ByteArray): ByteArray {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            return messageDigest.digest(message)
        }

        override fun signP256(message: ByteArray, secretKey: ByteArray): ByteArray =
            signEC(message, secretKey, ECCurve.Secp256R1)

        override fun signSecp256K1(message: ByteArray, secretKey: ByteArray): ByteArray =
            signEC(message, secretKey, ECCurve.Secp256K1)

        override fun verifyP256(
            message: ByteArray,
            signature: ByteArray,
            publicKey: ByteArray
        ): Boolean = verifyEC(message, signature, publicKey, ECCurve.Secp256R1)

        override fun verifySecp256K1(
            message: ByteArray,
            signature: ByteArray,
            publicKey: ByteArray
        ): Boolean = verifyEC(message, signature, publicKey, ECCurve.Secp256K1)

        override fun signEd25519(message: ByteArray, secretKey: ByteArray): ByteArray {
            throw IllegalStateException("Ed25519 signature not supported")
        }

        override fun verifyEd25519(
            message: ByteArray,
            signature: ByteArray,
            publicKey: ByteArray
        ): Boolean {
            throw IllegalStateException("Ed25519 signature not supported")
        }

        private fun signEC(message: ByteArray, secretKey: ByteArray, curve: ECCurve): ByteArray =
            ecSign(message, secretKey.toECPrivateKey(curve))

        private fun verifyEC(message: ByteArray, signature: ByteArray, publicKey: ByteArray, curve: ECCurve): Boolean =
            Signature.getInstance(Constants.NONE_WITH_ECDSA).apply {
                initVerify(publicKey.toECPublicKey(curve))
                update(message)
            }.verify(signature)

        private fun ecParams(curve: ECCurve): ECParameterSpec =
            AlgorithmParameters.getInstance("EC").apply {
                init(ECGenParameterSpec(curve.stdName))
            }.getParameterSpec(ECParameterSpec::class.java)

        private fun ByteArray.toECPrivateKey(curve: ECCurve): PrivateKey =
            KeyFactory
                .getInstance("EC")
                .generatePrivate(ECPrivateKeySpec(BigInteger(1, this), ecParams(curve)))

        private fun ByteArray.toECPublicKey(curve: ECCurve): PublicKey {
            val params = ecParams(curve)

            return KeyFactory
                .getInstance("EC")
                .generatePublic(ECPublicKeySpec(toECPoint(params), params))
        }

        private fun ByteArray.toECPoint(params: ECParameterSpec): ECPoint {
            val orderLength = params.order.bitLength() / Byte.SIZE_BITS
            val x = BigInteger(1, sliceArray(0 until orderLength))
            val y = BigInteger(1, sliceArray(orderLength until size))

            return ECPoint(x, y)
        }

        private enum class ECCurve(val stdName: String) {
            Secp256K1("secp256k1"),
            Secp256R1("secp256r1"),
        }

    }
}