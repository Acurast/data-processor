package com.acurast.attested.executor.protocol.ethereum

import acurast.codec.extensions.toHex
import android.os.Bundle
import com.acurast.attested.executor.crypto.ICrypto
import com.acurast.attested.executor.crypto.curves.Secp256k1
import com.acurast.attested.executor.protocol.IProtocol
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.web3j.crypto.Sign
import org.web3j.crypto.SignatureDataOperations
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.rlp.*
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.ByteBuffer

class Ethereum(private val cryptoModule: ICrypto) : IProtocol {
    var initialized = false

    override fun init(onSuccess: () -> Unit, onError: (e: Throwable) -> Unit) {
        initialized = true
        onSuccess()
    }

    override fun initialized(): Boolean = initialized

    override fun getSigner(): ICrypto = cryptoModule

    /**
     * Get the account address.
     *
     * @return the last 20 bytes of keccak(public_key)
     */
    override fun getAddress(): String {
        val publicKey = this.cryptoModule.getPublicKey(false)
        return Keccak.Digest256().digest(publicKey).sliceArray(12..31).toHex()
    }

    override fun fulfill(
        context: Bundle,
        unserializedPayload: Any,
        onSuccess: (operationHash: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            // Prepare inputs
            val rpc = context.getString("rpc")
            val methodSignature = context.getString("methodSignature", "fulfill(bytes)")
            val destination = context.getString("destination").orEmpty()
            val gasLimit =
                BigInteger(context.getString("gasLimit", DefaultGasProvider.GAS_LIMIT.toString(10)))
            val maxPriorityFeePerGas = BigInteger(context.getString("maxPriorityFeePerGas", "0"))
            val maxFeePerGas = BigInteger(context.getString("maxFeePerGas", "0"))

            // Prepare RPC client
            val web3j = Web3j.build(HttpService(rpc))

            val rawTransaction = Utils.buildContractCall(
                web3j,
                methodSignature,
                unserializedPayload,
                "0x" + this.getAddress(),
                destination,
                gasLimit,
                maxPriorityFeePerGas,
                maxFeePerGas,
            )
            val transactionBytes = TransactionEncoder.encode(rawTransaction)
            val transactionDigest = Keccak.Digest256().digest(transactionBytes)
            val signature = this.cryptoModule.rawSign(transactionDigest)
            val r = signature.sliceArray(0..31)
            val s = signature.sliceArray(32..63)
            val v =
                Secp256k1.findRecoverId(r, s, this.cryptoModule.getPublicKey(), transactionDigest)
            val signatureData = Sign.SignatureData(
                (v + SignatureDataOperations.LOWER_REAL_V).toByte(), r, s
            )

            val values = TransactionEncoder.asRlpValues(rawTransaction, signatureData)
            var encodedSignedTransaction = RlpEncoder.encode(RlpList(values))
            encodedSignedTransaction = ByteBuffer.allocate(encodedSignedTransaction.size + 1)
                .put(rawTransaction.type.rlpType)
                .put(encodedSignedTransaction)
                .array()

            // Submit transaction and wait for it to be included in a block
            val result =
                web3j.ethSendRawTransaction(Numeric.toHexString(encodedSignedTransaction)).send()

            if (result.error == null) {
                onSuccess(result.transactionHash)
            } else {
                onError(Exception(result.error.message))
            }
        } catch (e: Throwable) {
            onError(e)
        }
    }
}