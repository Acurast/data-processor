package com.acurast.attested.executor.protocol.acurast

import acurast.codec.extensions.hexToBa
import acurast.codec.type.*
import com.acurast.attested.executor.crypto.CryptoLegacy
import java.math.BigInteger

class Utils {
    companion object {
        public fun prepareExtrinsic(
            call: ExtrinsicCall,
            data: AcurastRPC.OperationData,
            multiAddress: MultiAddress
        ): ByteArray {
            val extrinsicPayload = ExtrinsicPayload(
                call,
                data.era,
                data.nonce,
                data.tip,
                data.specVersion,
                data.transactionVersion,
                data.genesisHash,
                data.blockHash
            )

            // Sign extrinsic payload
            val signature = CryptoLegacy.acurastSign(extrinsicPayload.toU8a());

            return Extrinsic(
                ExtrinsicSignature(
                    multiAddress,
                    MultiSignature(CurveKind.Secp256r1, signature),
                    data.era,
                    data.nonce,
                    data.tip
                ),
                call
            ).toU8a()
        }

        public suspend fun prepareOperation(
            rpc: acurast.rpc.RPC,
            accountId: ByteArray
        ): AcurastRPC.OperationData {
            val accountInfo = rpc.getAccountInfo(accountId)
            val blockHeader = rpc.chain.getHeader()
            val genesisBlockHash = rpc.chain.getBlockHash(BigInteger.ZERO)
            val latestBlockHash = rpc.chain.getBlockHash(blockHeader.number)
            val runtimeVersion = rpc.state.getRuntimeVersion()

            val era = MortalEra.from(
                blockHeader.number.toInt().toUInt()
            )

            return AcurastRPC.OperationData(
                era,
                accountInfo.nonce.toLong(),
                BigInteger.ZERO,
                runtimeVersion.specVersion.toLong(),
                runtimeVersion.transactionVersion.toLong(),
                genesisBlockHash.hexToBa(),
                latestBlockHash.hexToBa(),
            )
        }
    }
}