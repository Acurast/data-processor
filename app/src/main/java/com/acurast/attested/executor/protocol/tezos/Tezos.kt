package com.acurast.attested.executor.protocol.tezos

import android.os.Bundle
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.crypto.ICrypto
import com.acurast.attested.executor.protocol.IProtocol
import it.airgap.tezos.michelson.micheline.dsl.builder.expression.Pair
import it.airgap.tezos.michelson.micheline.dsl.micheline
import it.airgap.tezos.michelson.packer.packToBytes
import java.math.BigInteger

class Tezos(private val cryptoModule: ICrypto) : IProtocol {
    var initialized = false

    override fun init(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        ensureRevealed(
            {
                initialized = true
                onSuccess()
            },
            onError
        )
    }

    override fun initialized(): Boolean = initialized

    override fun getSigner(): ICrypto = cryptoModule

    override fun getAddress(): String {
        return Utils.getPublicKeyHash(cryptoModule).base58
    }

    override fun fulfill(
        context: Bundle,
        unserializedPayload: Any,
        onSuccess: (operationHash: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            val fee = context.getLong("fee")
            val gasLimit = context.getLong("gasLimit")
            val storageLimit = context.getLong("storageLimit")
            val entrypoint = context.getString("entrypoint", "fulfill")
            val destination = context.getString("destination", Constants.FULFILL_CONTRACT_ADDRESS)
            val jobIdentifier = when (val jobID = context.get("jobIdentifier")) {
                is ByteArray -> jobID
                else -> throw IllegalArgumentException()
            }

            val payload = Micheline.fromValue(unserializedPayload)

            TezosRPC.fetchBranch(
                onSuccess = { branch ->
                    TezosRPC.fetchCounter(
                        address = this.getAddress(),
                        onSuccess = { counter ->
                            TezosRPC.injectOperation(
                                operation = Operation.buildOperation(
                                    branch,
                                    Operation.buildTransaction(
                                        this.cryptoModule,
                                        BigInteger.valueOf(fee),
                                        counter.add(BigInteger.ONE),
                                        BigInteger.valueOf(gasLimit),
                                        BigInteger.valueOf(storageLimit),
                                        BigInteger.ZERO,
                                        destination,
                                        entrypoint,
                                        micheline {
                                            Pair {
                                                arg { bytes(jobIdentifier) }
                                                arg { bytes(payload.packToBytes()) }
                                            }
                                        }
                                    )
                                ),
                                onSuccess = onSuccess,
                                onError = onError
                            )
                        },
                        onError = onError
                    )
                },
                onError = onError
            )
        } catch (e: Throwable) {
            onError(e)
        }
    }

    /**
     * This method ensures that the account public key gets revealed.
     */
    private fun ensureRevealed(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        TezosRPC.isRevealed(
            address = Utils.getPublicKeyHash(this.cryptoModule),
            onSuccess = { isRevealed ->
                if (!isRevealed) {
                    TezosRPC.fetchBranch(
                        onSuccess = { branch ->
                            TezosRPC.fetchCounter(
                                address = this.getAddress(),
                                onSuccess = { counter ->
                                    TezosRPC.injectOperation(
                                        operation = Operation.buildOperation(
                                            branch,
                                            Operation.buildReveal(
                                                this.cryptoModule,
                                                BigInteger.valueOf(3590L),
                                                counter.add(BigInteger.ONE),
                                                BigInteger.valueOf(1000L),
                                                BigInteger.valueOf(1000L)
                                            )
                                        ),
                                        onSuccess = { onSuccess() },
                                        onError = onError
                                    )
                                },
                                onError = onError
                            )
                        },
                        onError = onError
                    )
                } else {
                    onSuccess()
                }
            },
            onError = onError
        )
    }
}