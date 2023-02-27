package com.acurast.attested.executor.crypto

import acurast.codec.extensions.hexToBa
import com.acurast.attested.executor.crypto.curves.Secp256r1
import org.junit.Assert
import org.junit.Test

class Secp256r1Test {
    @Test
    fun createRSVSignature() {
        val expected: Byte = 0x00
        val signature = Secp256r1.findRecoverId(
            "155e93be2101e2dc4f74517ddc67eb1730ecf13df982597bcc7b758f071a036b".hexToBa(),
            "3923bd98d0b59ae051264a9781200cc8dd3799af32858b125922f01ab5d2168a".hexToBa(),
            "032221f88ab3843cdf8ee8f0a410237b712278b99f9982504644a0018d94c179c5".hexToBa(),
            "af9613760f72635fbdb44a5a0a63c39f12af30f950a6ee5c971be188e89c4051".hexToBa()
        )
        Assert.assertEquals(expected, signature)
    }
}