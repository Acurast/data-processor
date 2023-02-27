package com.acurast.attested.executor.native

import acurast.codec.extensions.hexToBa
import acurast.codec.extensions.toHex
import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.crypto.curves.GenericEcdsa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// @RunWith is required only if you use a mix of JUnit3 and JUnit4.
@RunWith(AndroidJUnit4::class)
@SmallTest
class SweetBICryptoTest {
    private lateinit var sweetB: SweetBSigner
    private lateinit var genericEcdsaSigner: GenericEcdsa

    @Before
    fun setup() {
        this.genericEcdsaSigner = GenericEcdsa(SB_CURVE.SB_SW_CURVE_SECP256K1)
        this.sweetB = SweetBSigner(SB_CURVE.SB_SW_CURVE_SECP256K1)
    }

    @Test
    fun computePublicKey() {
        val privateKey =
            "995d78eada34f8fdae356d0c4b8e1ee25f104b094cb0689888dfdb6144f3271a".hexToBa()
        val expectedPublicKey =
            "02e639863bbc4dd0db450bb0029b1a1a05689b7cb178ef13c3d343138705d3e848d454141846ea165524e5d418fccb107207374f861f1c13f9b46d5785d0e878"
        val result = this.sweetB.computePublicKey(privateKey).toHex()

        assertEquals(expectedPublicKey, result)
    }

    @Test
    fun compressPublicKey() {
        val publicKey1 =
            "02e639863bbc4dd0db450bb0029b1a1a05689b7cb178ef13c3d343138705d3e848d454141846ea165524e5d418fccb107207374f861f1c13f9b46d5785d0e878".hexToBa()
        val compressed1 = this.sweetB.compressPublicKey(publicKey1).toHex()
        val expected1 = "0202e639863bbc4dd0db450bb0029b1a1a05689b7cb178ef13c3d343138705d3e8"
        assertEquals(expected1, compressed1)

        val publicKey2 =
            "3ba44319e14583c7c01957d8997d13399bbb66f6b88774701a67f2e7ea9c32fc39eb4651dafef5c94b360beff76813d74d94890a0aa6a25989382ee67fa230a1".hexToBa()
        val expected2 = "033ba44319e14583c7c01957d8997d13399bbb66f6b88774701a67f2e7ea9c32fc"
        val compressed2 = this.sweetB.compressPublicKey(publicKey2).toHex()
        assertEquals(expected2, compressed2)
    }

    @Test
    fun verifyPublicKey() {
        assertTrue(
            this.sweetB.verifyPublicKey(this.genericEcdsaSigner.getPublicKey(compressed = false))
        )
    }

    @Test
    fun signMessageDigest() {
        val privateKey =
            "995d78eada34f8fdae356d0c4b8e1ee25f104b094cb0689888dfdb6144f3271a".hexToBa()
        val messageDigest = "ff".repeat(32).hexToBa()
        val expected =
            "e68720fce041c86b88a123e2d19b42d1266f328e717ccb738f334760411a8c4f0688f85d1d8b5230690b2870928418e9630462a2b9c6e443f18770581b3043e0"
        val result = this.sweetB.signMessageDigest(
            privateKey,
            messageDigest
        ).toHex()
        assertEquals(expected, result)
    }

    @Test
    fun generateSharedSecret() {
        val privateKeyA =
            "995d78eada34f8fdae356d0c4b8e1ee25f104b094cb0689888dfdb6144f3271a".hexToBa()
        val publicKeyA = this.sweetB.computePublicKey(
            privateKeyA
        )
        val privateKeyB =
            "995d78eada34f8fdae356d0c4b8e1ee25f104b094cb0689888dfdb6144f3271a".hexToBa()
        val publicKeyB = this.sweetB.computePublicKey(
            privateKeyB
        )

        val secretAB = this.sweetB.generateSharedSecret(
            privateKeyA,
            publicKeyB
        ).toHex()

        val secretBA = this.sweetB.generateSharedSecret(
            privateKeyB,
            publicKeyA
        ).toHex()

        assertEquals(secretAB, secretBA)
    }
}
