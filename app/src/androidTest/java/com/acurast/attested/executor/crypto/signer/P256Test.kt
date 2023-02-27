package com.acurast.attested.executor.crypto.signer

import acurast.codec.extensions.hexToBa
import acurast.codec.extensions.toHex
import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.acurast.attested.executor.crypto.curves.P256
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class P256Test {
    private lateinit var signer: P256

    @Before
    fun setup() {
        this.signer = P256()
    }

    @Test
    fun getPublicKey() {
        val key = this.signer.getPublicKey()
        assertEquals(33, key.size)
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