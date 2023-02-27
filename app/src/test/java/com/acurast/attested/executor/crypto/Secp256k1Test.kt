package com.acurast.attested.executor.crypto

import acurast.codec.extensions.hexToBa
import com.acurast.attested.executor.crypto.curves.Secp256k1
import org.junit.Assert
import org.junit.Test

class Secp256k1Test {
    @Test
    fun createRSVSignature1() {
        val expected: Byte  = 0x00
        val signature = Secp256k1.findRecoverId(
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798".hexToBa(),
            "7fb9347dec59aeea6f698a55a5a01aefbba6862357a6cfca299555c4648c2fee".hexToBa(),
            "0202e639863bbc4dd0db450bb0029b1a1a05689b7cb178ef13c3d343138705d3e8".hexToBa(),
            "ff".repeat(32).hexToBa()
        )
        Assert.assertEquals(expected, signature)
    }

    @Test
    fun createRSVSignature2() {
        val expected: Byte  = 0x01
        val signature = Secp256k1.findRecoverId(
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798".hexToBa(),
            "3c556157c2012356db01fe15423fc170179fc1698111606c47463e027098d54a".hexToBa(),
            "02a80db7e510496aebf8e119e18611b28d1361164e70854eaca54c1a6b6a600888".hexToBa(),
            "f0".repeat(32).hexToBa()
        )
        Assert.assertEquals(expected, signature)
    }

    @Test
    fun createRSVSignature3() {
        val expected: Byte  = 0x01
        val recId = Secp256k1.findRecoverId(
            "f501bda76b0efad267775269e96de1d6dc17a826a6bc8f52e2ae456ee070ae53".hexToBa(),
            "5537f37163b848ef7f9578f90315db29e7b86c81502d36397a92cb0a67b21dea".hexToBa(),
            "0202e639863bbc4dd0db450bb0029b1a1a05689b7cb178ef13c3d343138705d3e8".hexToBa(),
            "f0".repeat(32).hexToBa()
        )
        Assert.assertEquals(expected, recId)
    }
}