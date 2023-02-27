package com.acurast.attested.executor.utils

import android.app.Application
import android.content.SharedPreferences
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class IPFSTest {
    @MockK
    private lateinit var context : Application
    @MockK
    private lateinit var sharedPreferences: SharedPreferences
    @MockK
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @After
    fun clean() {
        unmockkAll()
    }

    @Test
    fun resolveUri() {
        val ipfsUri = "ipfs://QmdJNvMLfvjzJnHQJmsEBC8KUD1fyTusFrkXAF5YaZouT2"
        every { sharedPreferences.contains(ipfsUri) } returns false
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit
        every { sharedPreferences.edit() } returns editor
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences

        val scriptContent = "fulfill(generateSecureRandomHex())\n"
        mockkObject(Networking)
        every { Networking.httpsRequest(any(), any(), any(), any(), any(), any()) } answers {
            arg<(String, ByteArray) -> Unit>(4).invoke(scriptContent, byteArrayOf())
        }

        with(CountDownLatch(1)) {
            var content = ""
            IPFS.resolveUri(context, "ipfs://QmdJNvMLfvjzJnHQJmsEBC8KUD1fyTusFrkXAF5YaZouT2", {
                content = it
                countDown()
            }, {
                countDown()
            })
            await()
            Assert.assertEquals(content, scriptContent)
        }
    }

    @Test
    fun uriToHash() {
        val cid = IPFS.uriToHash("ipfs://QmasuFVFEEcSqcSNHqxecqftt2wPPKLURSqFaYKtCSJDRE")
        Assert.assertEquals(cid, "QmasuFVFEEcSqcSNHqxecqftt2wPPKLURSqFaYKtCSJDRE")
    }
}