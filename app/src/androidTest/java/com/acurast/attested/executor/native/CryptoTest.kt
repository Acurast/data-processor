package com.acurast.attested.executor.native

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.acurast.attested.executor.App
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.crypto.Crypto
import com.rfksystems.blake2b.Blake2b
import com.ubinetic.attested.executor.signers.GenericEcdsaSigner
import com.ubinetic.attested.executor.utils.Curve
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec


// @RunWith is required only if you use a mix of JUnit3 and JUnit4.
@RunWith(AndroidJUnit4::class)
@SmallTest
class CryptoTest {

    @Test
    fun verifyPublicKey() {
        val keyStore: KeyStore = KeyStore.getInstance(Constants.ANDROID_KEYSTORE_ALIAS)
        keyStore.load(null)

        val privateKey =  if (!keyStore.containsAlias("B")) {
            val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, Constants.ANDROID_KEYSTORE_ALIAS
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                "B",
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_ENCRYPT
            ).setAlgorithmParameterSpec(ECGenParameterSpec(Constants.SECP256R1)).setDigests(
                KeyProperties.DIGEST_NONE
            ).setAttestationChallenge(Crypto.secureRandomBytes())
                .setRandomizedEncryptionRequired(false)
                .setIsStrongBoxBacked(true)

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
            keyPair.private
        } else {
            val entry = keyStore.getEntry("B", null)
            (entry as KeyStore.PrivateKeyEntry).privateKey
        }

        val signature = Signature.getInstance(Constants.NONE_WITH_ECDSA)
        signature.initSign(privateKey)
        signature.update(ByteArray(32))
        val signaturePayload = signature.sign()
    }

    //TODO add creation of signature and ECDH
}
