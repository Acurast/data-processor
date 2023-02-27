package com.acurast.attested.executor

import acurast.codec.extensions.hexToBa
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.acurast.attested.executor.v8.V8Executor
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

val javascriptExamples = arrayOf(
    """
        generateSecureRandomHex()
    """,
    """
        const text = "HELLO WORLD"
        print(text)
    """,
    """
        const encrypted = _STD_.signers.secp256k1.encrypt(
            "0xfc5ca2476ea685b6217309a32abc70b43d7899a3460dd3406df981c4db00f155e71ba8c71da17d6e7d50ec81f24bcaed9bdd9ffc0885021dec67ec023643c0d3",
            "0xff",
            "0x1234"
        )
        print(encrypted)
    """,
/*    """
        const payload = "0xff";
        const result = _STD_.chains.tezos.fulfill(
            "https://tezos-ghostnet-node.prod.gke.papers.tech",
            undefined,
            payload,
            {
                fee: 1000,
                gasLimit: 1000,
                storageLimit: 1000,
            }
        )
        print(result)
    """,*/
)

@RunWith(AndroidJUnit4::class)
@SmallTest
class V8ExecutorTest {
    @Test
    fun executeScriptExamples() {
        for (script in javascriptExamples) {
            var executed = false
            with(CountDownLatch(1)) {
                V8Executor(
                    "0xff".hexToBa(),
                    script,
                    Bundle(),
                ).execute(
                    onSuccess = { executed = true; countDown() },
                    onError = { executed = false; countDown() }
                )
                await()
            }
            Assert.assertTrue(executed)
        }
    }

    @Test
    fun executeScriptWithEthereumCall() {
        val script = """
            const payload = "0xff";
            const destination = "0x320c4a2053bC59A9F40644E383e30d6aDe55677B";
            _STD_.chains.ethereum.fulfill(
                "https://goerli.infura.io/v3/75829a5785c844bc9c9e6e891130ee6f",
                destination,
                payload,
                {
                    methodSignature: "fulfill",
                    gasLimit: "9000000",
                    maxFeePerGas: "6500000000",
                    maxPriorityFeePerGas: "2500000000",
                },
                (opHash) => {
                    print("Succeeded: " + opHash)
                },
                (err) => {
                    print("Failed: " + err)
                },
            )
        """
        var executed = false
        with(CountDownLatch(1)) {
            V8Executor(
                "0xff".hexToBa(),
                script,
                Bundle(),
            ).execute(
                onSuccess = { executed = true; countDown() },
                onError = { executed = false; countDown() }
            )
            await()
        }
        Assert.assertTrue(executed)
    }
}