package com.acurast.attested.executor.protocol.tezos

import acurast.codec.extensions.toHex
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.crypto.curves.P256
import com.acurast.attested.executor.utils.Encoding
import io.mockk.*
import io.mockk.impl.annotations.MockK
import it.airgap.tezos.core.Tezos as TezosSDK
import it.airgap.tezos.michelson.micheline.dsl.micheline
import it.airgap.tezos.operation.coder.forgeToBytes
import it.airgap.tezos.rpc.RpcModule
import it.airgap.tezos.rpc.http.HttpClientProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TezosTest {

    @MockK
    private lateinit var httpClient : HttpClientProvider

    private lateinit var tezosProtocol: Tezos

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkConstructor(P256::class)

        TezosSDK {
            isDefault = true
            cryptoProvider = CryptoLegacy.Tezos()
            use(RpcModule) {
                httpClientProvider = httpClient
            }
        }

        coEvery {
            anyConstructed<P256>().getPublicKey()
        } returns Encoding.base58CheckDecode("p2pk67U3tiwZBEb7cv7YcuY5v286VJSBXESVV4FVJqS2mFejtYv86Br").drop(4).toByteArray()

        tezosProtocol = Tezos(P256())
    }

    @After
    fun clean() {
        unmockkAll()
    }

    @Test
    fun fetchBranch() {
        val countDownLatch = CountDownLatch(1)

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val currentDate = sdf.format(Date())

        val jsonResponse = """                	
{"protocol":"PtKathmankSpLLDALzWw7CGD2j2MtyveTwboEYokqUCP4a1LxMg","chain_id":"NetXnHfVqm9iesp","hash":"BLASKAk1eH1whBQ443vJKmi18asJz5ZdTLiY7cWHvNE62dyUY2N","level":1652473,"proto":4,"predecessor":"BMUo7xGf2gCsRWgmLKTd3f3QDQxJe94Ccvw1NpBQ1atjXkdCYmg","timestamp":"$currentDate","validation_pass":4,"operations_hash":"LLoaTpEyPD7hiDhACQ6aa4HH6byXUMshfVurMSqpUV2mp2hKV13oh","fitness":["02","001936f9","","ffffffff","00000000"],"context":"CoVQgGeiSbpDqiMjhfy9s1BgsUmXyB7nbqaoQpJ1RqToD4MBMFjX","payload_hash":"vh3AJ1xB1tivVRa4D4uCMjjfpVRrKZ6RMURR4GQ5PTSRRy2xBkB5","payload_round":0,"proof_of_work_nonce":"763259c58b4f0000","liquidity_baking_toggle_vote":"on","signature":"sighoginksLo5M3f3EbBcHy6SCPrhuSLY6iZWAcmEndVvECUnG7s7a3hHpkP5KFymRzwQHLh3ZcF5VmFemvnNwgp3iUrYtJW"}
        """.trimIndent()

        coEvery { httpClient.get(any(), any(), any(), any(), any(), any()) } returns jsonResponse

        var result = ""
        TezosRPC.fetchBranch(
            onSuccess = {
                result = it
                countDownLatch.countDown()
            },
            onError = {
                countDownLatch.countDown()
            }
        )
        countDownLatch.await()
        assertEquals(result, "BLASKAk1eH1whBQ443vJKmi18asJz5ZdTLiY7cWHvNE62dyUY2N")
    }

    @Test
    fun publicKeyHash() {
        assertEquals(
            "tz3bqWfPDPCHaQspeNj2RA3Xtt1jewJfxmts",
            tezosProtocol.getAddress()
        )
    }

    @Test
    fun buildOperation() {
        val transaction = Operation.buildOperation(
            "BMHBtAaUv59LipV1czwZ5iQkxEktPJDE7A9sYXPkPeRzbBasNY8",
            Operation.buildTransaction(
                tezosProtocol.getSigner(),
                BigInteger.valueOf(2355),
                BigInteger.valueOf(20503891),
                BigInteger.valueOf(19856),
                BigInteger.valueOf(2535),
                BigInteger.valueOf(0),
                "KT1Mgy95DVzqVBNYhsW93cyHuB57Q94UFhrh",
                "makeOven",
                micheline { Unit },
            )
        )

        assertEquals(transaction.forgeToBytes().toHex(), "ce69c5713dac3537254e7be59759cf59c15abd530d10501ccf9028a5786314cf6c02aa1c86fc255fd6caf272eeed63821f3c5d78140cb312d3bae309909b01e71300018fc5caed1547213e2227eedae324337031afe37700ffff086d616b654f76656e000000050200000000")
    }
}