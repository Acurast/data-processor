package com.acurast.attested.executor.protocol.tezos

import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.utils.Networking
import com.acurast.attested.executor.utils.toHex
import it.airgap.tezos.core.Tezos as TezosSdk
import it.airgap.tezos.michelson.packer.packToBytes
import it.airgap.tezos.rpc.RpcModule
import org.junit.Test

import org.junit.Assert.*
import org.junit.BeforeClass
import java.math.BigInteger

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MichelineTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupAll() {
            TezosSdk {
                isDefault = true
                cryptoProvider = CryptoLegacy.Tezos()
                use(RpcModule) {
                    httpClientProvider = Networking.HttpClient()
                }
            }
        }
    }

    @Test
    fun michelineFromNat() {
        val packedData = Micheline.fromNat(BigInteger.valueOf(0)).packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "0000")
    }

    @Test
    fun michelineFromBool() {
        var packedData = Micheline.fromBool(true).packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "030a")
        packedData = Micheline.fromBool(false).packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "0303")
    }

    @Test
    fun michelineFromMap() {
        val map = HashMap<it.airgap.tezos.michelson.micheline.Micheline, it.airgap.tezos.michelson.micheline.Micheline>()
        map.put(Micheline.fromString("0"), Micheline.fromNat(BigInteger.valueOf(0)))
        map.put(Micheline.fromString("1"), Micheline.fromNat(BigInteger.valueOf(0)))
        val packedData = Micheline.fromMap(map).packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "02000000140704010000000130000007040100000001310000")
    }

    @Test
    fun michelineFromString() {
        val packedData = Micheline.fromString("asdf").packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "010000000461736466")
    }

    @Test
    fun michelineFromList() {
        val packedData = Micheline.fromList(
            listOf(
                Micheline.fromNat(BigInteger.valueOf(0)),
                Micheline.fromNat(BigInteger.valueOf(0))
            )
        ).packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "020000000400000000")
    }

    @Test
    fun michelineFromPair() {
        val packedData = Micheline.fromPair(
            Micheline.fromNat(BigInteger.valueOf(0)),
            Micheline.fromNat(BigInteger.valueOf(0))
        ).packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "070700000000")
    }

    @Test
    fun michelineFromBytes() {
        val packedData = Micheline.fromBytes(ByteArray(3) { 1 }).packToBytes(dropTag = true)
        assertEquals(packedData.toHex(), "0a00000003010101")
    }

    @Test
    fun michelineFromSignature() {
        val packedData =
            Micheline.fromSignature("p2sigoocoSbrF9aqyf4dCPuL5W8Q1CijkpyfbLpYkExmrdG83a9E2dQ6QBeEzG35bug1HtMCJwzpsoHPYaHLtZs8aH7gW8KDMb").packToBytes(dropTag = true)
        assertEquals(
            packedData.toHex(),
            "0a00000040ca91963f58660bff08a2db1b29b90e03adf4ed4748edb43b3856c5c7040e8d41f468c2756a8c9324a7317c052833afd3797083e0c009b3c083ea177afbafe5ad"
        )
    }
}