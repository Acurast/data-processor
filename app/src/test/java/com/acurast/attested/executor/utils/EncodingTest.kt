package com.acurast.attested.executor.utils

import com.acurast.attested.executor.crypto.CryptoLegacy
import acurast.codec.extensions.hexToBa
import acurast.codec.extensions.toHex
import com.acurast.attested.executor.utils.Encoding
import com.acurast.attested.executor.utils.Networking
import it.airgap.tezos.core.Tezos
import it.airgap.tezos.rpc.RpcModule
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class EncodingTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupAll() {
            Tezos {
                isDefault = true
                cryptoProvider = CryptoLegacy.Tezos()
                use(RpcModule) {
                    httpClientProvider = Networking.HttpClient()
                }
            }
        }
    }

    @Test
    fun encodeAndDecode() {
        val payload = "ff1234ff"
        val encoded = Encoding.base58CheckEncode(payload.hexToBa())
        val decoded = Encoding.base58CheckDecode(encoded)
        Assert.assertEquals(decoded.toHex(), payload)
    }
}