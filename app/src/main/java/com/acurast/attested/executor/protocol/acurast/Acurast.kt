package com.acurast.attested.executor.protocol.acurast

import acurast.codec.extensions.blake2b
import acurast.codec.extensions.toSS58
import android.os.Bundle
import com.acurast.attested.executor.crypto.ICrypto
import com.acurast.attested.executor.protocol.IProtocol

class Acurast(private val cryptoModule: ICrypto) : IProtocol {
    var initialized = false

    override fun init(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        initialized = true
        onSuccess()
    }

    override fun initialized(): Boolean = initialized

    override fun getSigner(): ICrypto = cryptoModule

    override fun getAddress(): String {
        return cryptoModule.getPublicKey(compressed = true).blake2b(256).toSS58()
    }

    override fun fulfill(
        context: Bundle,
        unserializedPayload: Any,
        onSuccess: (operationHash: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        TODO("NOT_USED_YET")
    }
}