package com.acurast.attested.executor.protocol.ethereum

import acurast.codec.extensions.hexToBa
import org.junit.Assert
import org.junit.Test

class UtilsTest {
    @Test
    fun encodeMethodSignature() {
        Assert.assertEquals("144f725e", Utils.encodeMethodSignature("fulfill(bytes)"))
    }

    @Test
    fun encodeIntAsEvmSlot() {
        Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020", Utils.encodeIntAsEvmSlot(32))
    }

    @Test
    fun encodeBytesArgument() {
        Assert.assertEquals(
            "00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000001ff00000000000000000000000000000000000000000000000000000000000000",
            Utils.encodeBytesArgument(0, "ff".hexToBa())
        )
    }
}