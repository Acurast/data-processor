package com.acurast.attested.executor.protocol.tezos

import com.acurast.attested.executor.crypto.ICrypto
import it.airgap.tezos.core.converter.encoded.Address
import it.airgap.tezos.core.type.encoded.BlockHash
import it.airgap.tezos.core.type.number.TezosNatural
import it.airgap.tezos.core.type.tez.Mutez
import it.airgap.tezos.michelson.micheline.Micheline
import it.airgap.tezos.operation.Operation
import it.airgap.tezos.operation.OperationContent
import it.airgap.tezos.operation.contract.Entrypoint
import it.airgap.tezos.operation.contract.Parameters
import java.math.BigInteger

class Operation {
    companion object {
        /**
         * Build an operation.
         */
        fun buildOperation(branch: String, content: OperationContent): Operation {
            return Operation(content, branch = BlockHash(branch))
        }

        /**
         * Build an operation of kind `reveal`
         */
        fun buildReveal(
            ICrypto: ICrypto,
            fee: BigInteger,
            counter: BigInteger,
            gasLimit: BigInteger,
            storageLimit: BigInteger
        ): OperationContent.Reveal {
            return OperationContent.Reveal(
                source = Utils.getPublicKeyHash(ICrypto),
                fee = Mutez(fee.toString()),
                counter = TezosNatural(counter.toString()),
                gasLimit = TezosNatural(gasLimit.toString()),
                storageLimit = TezosNatural(storageLimit.toString()),
                publicKey = Utils.getPublicKey(ICrypto)
            )
        }

        /**
         * Build an operation of kind `transaction`
         */
        fun buildTransaction(
            ICrypto: ICrypto,
            fee: BigInteger,
            counter: BigInteger,
            gasLimit: BigInteger,
            storageLimit: BigInteger,
            amount: BigInteger,
            contractAddress: String,
            entrypoint: String,
            script: Micheline
        ): OperationContent.Transaction {
            return OperationContent.Transaction(
                source = Utils.getPublicKeyHash(ICrypto),
                fee = Mutez(fee.toString()),
                counter = TezosNatural(counter.toString()),
                gasLimit = TezosNatural(gasLimit.toString()),
                storageLimit = TezosNatural(storageLimit.toString()),
                amount = Mutez(amount.toString()),
                destination = Address(contractAddress),
                parameters = Parameters(Entrypoint(entrypoint), script),
            )
        }
    }
}