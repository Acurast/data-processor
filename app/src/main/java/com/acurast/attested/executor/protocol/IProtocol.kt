package com.acurast.attested.executor.protocol

import android.os.Bundle
import com.acurast.attested.executor.crypto.ICrypto

/**
 * Protocol abstraction class.
 *
 * This abstract class implements the bootstrapping of an account on a specific protocol (i.e. Tezos) and
 * allows to invoke the fulfill method on-chain.
 */
interface IProtocol {

    /**
     * Asynchronous function to initialise the account on-chain (i.e. reveal).
     */
    fun init(onSuccess: () -> Unit, onError: (Throwable) -> Unit)

    /**
     * Informs about the initialization state of the protocol component
     */
    fun initialized(): Boolean

    /**
     * Get signer used in the protocol.
     */
    fun getSigner(): ICrypto

    /**
     * Returns the address of the account.
     */
    fun getAddress(): String

    /**
     * Fulfills a job.
     *
     * The [context] argument can contain arbitrary data specific to the target protocol.
     * [unserializedPayload] is an arbitrary payload to be sent to the target chain.
     */
    fun fulfill(
        context: Bundle,
        unserializedPayload: Any,
        onSuccess: (operationHash: String) -> Unit,
        onError: (Throwable) -> Unit
    )
}
