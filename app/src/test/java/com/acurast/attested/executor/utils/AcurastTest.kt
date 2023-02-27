package com.acurast.attested.executor.utils

import acurast.codec.extensions.hexToBa
import acurast.codec.type.AccountId32
import acurast.codec.type.acurast.JobIdentifier
import acurast.codec.type.acurast.JobRegistration
import acurast.codec.type.marketplace.ExecutionResult
import acurast.codec.type.marketplace.JobAssignment
import acurast.rpc.RPC
import acurast.rpc.http.KtorHttpClientProvider
import acurast.rpc.pallet.RuntimeVersion
import acurast.rpc.type.FrameSystemAccountInfo
import acurast.rpc.type.Header
import com.acurast.attested.executor.App
import com.acurast.attested.executor.crypto.Attestation
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.protocol.acurast.AcurastRPC
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Thread.sleep
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AcurastTest {
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkConstructor(KtorHttpClientProvider::class)
        mockkConstructor(RPC::class)

        mockkObject(Notification)
        every { Notification.notify(any(), any()) } returns

        mockkObject(CryptoLegacy)
        every { CryptoLegacy.getPublicKey() } returns Encoding.base58CheckDecode("p2pk67U3tiwZBEb7cv7YcuY5v286VJSBXESVV4FVJqS2mFejtYv86Br").drop(4).toByteArray()
        every { CryptoLegacy.acurastSign(any()) } returns "696e710fc4516d0a2ba91162777b5f0a4d0e9849a6121a4bae00a0d2df70b5d2ef6e26b0191024872aa22530ed3bef47cd8b0c635e659c79a4cc4a1533013b9c01".hexToBa()

        mockkObject(Scheduler)

        val accountInfo = FrameSystemAccountInfo(
            nonce = 0U,
            consumers = 1U,
            providers = 1U,
            sufficients = 0U,
            data = FrameSystemAccountInfo.FrameSystemAccountInfoData(
                free = BigInteger("1152921504606846976"),
                reserved = BigInteger.ZERO,
                miscFrozen = BigInteger.ZERO,
                feeFrozen = BigInteger.ZERO,
            )
        )
        coEvery {
            anyConstructed<RPC>().getAccountInfo(any(), any(), any(), any(), any())
        } returns accountInfo

        val blockHeader = Header(
            parentHash = "0x07476e5b6a6551a2395fd20176fb44128337e3f257133c1bdc0ca70152fa388d",
            number = BigInteger.ONE,
            stateRoot = "0xbd8b28d00f3936c7756f715d20d85d8233818add176a94df948f8223e9f7ff54",
            extrinsicsRoot = "0x21452e5cb48a7467bc74a8d2202a264e0c6b75c2278b278504b96dd1c7864d98",
        )
        coEvery {
            anyConstructed<RPC>().chain.getHeader(any(), any(), any(), any())
        } returns blockHeader

        val blockHash = "0x07476e5b6a6551a2395fd20176fb44128337e3f257133c1bdc0ca70152fa388d"
        coEvery {
            anyConstructed<RPC>().chain.getBlockHash(any(), any(), any(), any())
        } returns blockHash

        val runtimeVersion = RuntimeVersion(
            specVersion = 1_900,
            transactionVersion = 2
        )
        coEvery {
            anyConstructed<RPC>().state.getRuntimeVersion(any(), any(), any(), any())
        } returns runtimeVersion

        val opHash = "0x08d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d8eaf04151687736326c9fea17e25fc5287613693c912909cb226aa4794f26a48"
        coEvery {
            anyConstructed<RPC>().author.submitExtrinsic(any(), any(), any(), any())
        } returns opHash
    }

    @After
    fun clean() {
        unmockkAll()
    }

    @Test
    fun `Verify if device is attested`() {
        coEvery {
            anyConstructed<RPC>().isAttested(any(), any(), any(), any(), any())
        } returns true
        with(CountDownLatch(1)) {
            var isAttested = false
            AcurastRPC.queries.isAttested {
                countDown()
                isAttested = it
            }
            await()
            Assert.assertTrue(isAttested)
        }

        coEvery {
            anyConstructed<RPC>().isAttested(any(), any(), any(), any(), any())
        } returns false
        with(CountDownLatch(1)) {
            var isAttested = false
            AcurastRPC.queries.isAttested {
                countDown()
                isAttested = it
            }
            await()
            Assert.assertFalse(isAttested)
        }
    }

    @Test
    fun `Report job fulfillment`() {
        with(CountDownLatch(1)) {
            val jobId = JobIdentifier(
                requester = AccountId32("0xd43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d".hexToBa()),
                script = "697066733a2f2f516d5378377a44706b76627975674c33553339467454617357784d6d6b6647363773783977614752564837415145".hexToBa()
            )
            val result = ExecutionResult.success("0xffff".hexToBa())
            AcurastRPC.extrinsic.report(jobId, false, result)
            await(1L, TimeUnit.SECONDS)
        }
        verify { Notification.notify("Success", any()) }
    }

    @Test
    fun `Job fulfillment`() {
        with(CountDownLatch(1)) {
            val rpcURL = "http://localhost"
            val callIndex = byteArrayOf(0x20, 0x2)
            AcurastRPC.extrinsic.fulfill(
                rpcURL,
                callIndex,
                "697066733a2f2f516d5378377a44706b76627975674c33553339467454617357784d6d6b6647363773783977614752564837415145".hexToBa(),
                "0xd43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d".hexToBa()
            )
            await(1L, TimeUnit.SECONDS)
        }
        verify { Notification.notify("Success", any()) }
    }

    @Test
    fun `Submit attestation`() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AcurastRPC.extrinsic.submitAttestation()
            } catch(e: Throwable) {
                Assert.fail("Failed to submit attestation")
            }
        }
    }

    @Test
    fun `Advertise resources`() {
        with(CountDownLatch(1)) {
            var success = false
            AcurastRPC.extrinsic.advertise(
                0
            ) {
                success = it
            }
            await(1L, TimeUnit.SECONDS)
            Assert.assertTrue(success)
        }
    }

    @Test
    fun `Acknowledge job assignment`() {
        with(CountDownLatch(1)) {
            val jobId = JobIdentifier(
                requester = AccountId32("0xd43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d".hexToBa()),
                script = "697066733a2f2f516d5378377a44706b76627975674c33553339467454617357784d6d6b6647363773783977614752564837415145".hexToBa()
            )
            com.acurast.attested.executor.protocol.acurast.AcurastRPC.extrinsic.acknowledgeJob(jobId)
            await(1L, TimeUnit.SECONDS)
        }
        verify { Notification.notify("Success", any()) }
    }

    @Test
    fun `Fetch assigned jobs`() {
        val context = App()
        val jobAssignment = JobAssignment.read(
            listOf("0x1aee6710ac79060b1e13291ba85112af2b949d1a72012eeaa1f6b481830d0d734a803acc398e9201cfa3e321aa42c21fc213b320e36dae97c125f9f459d32c8d34c356e8294ec3b01cec015cc1b37ca4ee7fbc6e211d0b9d5197bfe177d80c251cbd2d43530a44705ad088af313e18f80b53ef16b36177cd4b77b846f2a5f07cd4697066733a2f2f516d5151656a454856664d4e743774574b716b54614b705533697453425235646e425833476e46487242356f3676","0x00000000000000000000010300a10f0432055800a68601000114000000000000000000000000000000")
        )
        val jobRegistration = JobRegistration.read(
            ByteBuffer.wrap("0xd4697066733a2f2f516d644a4e764d4c66766a7a4a6e48514a6d73454243384b554431667954757346726b5841463559615a6f7554320000881300000000000052525aa5850100003a565aa58501000020bf02000000000088130000000000008813000005000000204e00000000000100010300a10f0432055800821a06000000000000000000000000000000000000".hexToBa())
        )

        coEvery {
            anyConstructed<RPC>().getAssignedJobs(any(), any(), any(), any())
        } returns listOf(jobAssignment)

        coEvery {
            anyConstructed<RPC>().getJobRegistration(any(), any(), any(), any())
        } returns jobRegistration

        AcurastRPC.fetchJobs(context)

        sleep(1_000)

        verify {
            Scheduler.scheduleV8Execution(
                context,
                "ipfs://QmdJNvMLfvjzJnHQJmsEBC8KUD1fyTusFrkXAF5YaZouT2",
                "1cbd2d43530a44705ad088af313e18f80b53ef16b36177cd4b77b846f2a5f07c".hexToBa(),
                1673516438098,
                1673516439098,
                180_000,
                0,
                0,
                0
            )
        }
    }
}