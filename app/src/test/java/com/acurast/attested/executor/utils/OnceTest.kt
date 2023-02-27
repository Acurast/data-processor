package com.acurast.attested.executor.utils

import acurast.codec.extensions.hexToBa
import acurast.codec.extensions.toHex
import com.acurast.attested.executor.utils.Encoding
import com.acurast.attested.executor.utils.Once
import org.junit.Assert
import org.junit.Test

class OnceTest {
    @Test
    fun executeOnlyOnce() {
        var counter = 0;
        val clousure = Once<Int> { counter = it }

        clousure(1)
        Assert.assertEquals(counter, 1)
        clousure(2)
        Assert.assertEquals(counter, 1)
    }
}