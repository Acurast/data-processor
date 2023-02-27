package com.acurast.attested.executor.crypto.signer

import acurast.codec.extensions.hexToBa
import acurast.codec.extensions.toHex
import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.acurast.attested.executor.App
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.crypto.curves.GenericEcdsa
import com.acurast.attested.executor.native.SB_CURVE
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@SmallTest
class GenericEcdsaTest {
    private lateinit var signer: GenericEcdsa

    @Before
    fun setup() {
        this.signer = GenericEcdsa(SB_CURVE.SB_SW_CURVE_SECP256K1)

        // Clear previous key
        File(App.context.filesDir, Constants.KEY_FILE).delete()
    }

    @Test
    fun getOrCreatePrivateKey() {
        val key = this.signer.getOrCreatePrivateKey(returnKey = true)
        assertEquals(32, key.size)
    }

    @Test
    fun rawSign() {
        val payload = "1234".hexToBa()
        val signature = this.signer.rawSign(payload)

        assertEquals(signature.size, 64)
    }

    @Test
    fun encryptAndDecrypt() {
        val publicKey = this.signer.getPublicKey(compressed = false)

        val salt = "ff".hexToBa()
        val original = "1234".hexToBa()

        val encryptedBA = this.signer.rawStreamEncrypt(
            publicKey,
            salt,
            original
        )

        val decrypted = this.signer.rawStreamDecrypt(
            publicKey,
            salt,
            encryptedBA
        )

        assertEquals(decrypted.toHex(), original.toHex())
    }
}