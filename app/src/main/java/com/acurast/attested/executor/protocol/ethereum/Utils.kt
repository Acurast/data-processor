package com.acurast.attested.executor.protocol.ethereum

import acurast.codec.extensions.hexToBa
import com.acurast.attested.executor.utils.toHex
import okhttp3.internal.toHexString
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

class Utils {
    companion object {
        val SLOT_LENGTH = 32;

        /**
         * Ensure the result has 32 bytes.
         */
        private fun adjustToSlotLength(b: String, littleEndian: Boolean) =
            if(littleEndian) {
                b.padStart(SLOT_LENGTH * 2, '0')
            } else {
                b.padEnd(SLOT_LENGTH * 2, '0')
            }

        /**
         * Hashes the method signature of an EVM contract
         */
        fun encodeMethodSignature(methodSignature: String): String =
            Keccak.Digest256().digest(methodSignature.encodeToByteArray())
                .sliceArray(0..3)
                .toHex()

        /**
         * Encode integer in a 32 bytes slot (little endian)
         */
        fun encodeIntAsEvmSlot(n: Int): String = adjustToSlotLength(n.toHexString(), littleEndian = true)

        /**
         * Encode argument of type bytes
         *
         * [offset] is the current length of all previous arguments
         * [buffer] the bytes to be encoded
         */
        fun encodeBytesArgument(offset: Int, buffer: ByteArray): String {
            val startingPosition = encodeIntAsEvmSlot(offset + SLOT_LENGTH)
            val bytesLength = encodeIntAsEvmSlot(buffer.size)
            val bytesAdjusted = adjustToSlotLength(buffer.toHex(), littleEndian = false)
            return startingPosition + bytesLength + bytesAdjusted
        }

        /**
         * Build raw transaction EIP-155 (https://eips.ethereum.org/EIPS/eip-155)
         */
        fun buildContractCall(
            web3j: Web3j,
            methodSignature: String,
            unserializedPayload: Any,
            from: String,
            to: String,
            gasLimit: BigInteger,
            maxPriorityFeePerGas: BigInteger,
            maxFeePerGas: BigInteger,
        ): RawTransaction {
            val chainId = web3j.ethChainId().send()
            val transactionCount = web3j.ethGetTransactionCount(
                from,
                DefaultBlockParameterName.LATEST
            ).send()
            val nonce = if (transactionCount.hasError()) {
                BigInteger.ONE
            } else {
                transactionCount.transactionCount
            }
            val encodedMethodSig = encodeMethodSignature(methodSignature)

            val payload = when (unserializedPayload) {
                is String -> unserializedPayload.hexToBa()
                else -> throw IllegalArgumentException()
            }

            val data = encodedMethodSig + encodeBytesArgument(0, payload)
            return RawTransaction.createTransaction(
                chainId.chainId.toLong(),
                nonce,
                gasLimit,
                to,
                BigInteger.ZERO,
                "0x$data",
                // Gas information: https://docs.alchemy.com/docs/maxpriorityfeepergas-vs-maxfeepergas
                maxPriorityFeePerGas,
                maxFeePerGas,
            )
        }
    }
}