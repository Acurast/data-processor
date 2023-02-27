package com.acurast.attested.executor.crypto

/**
 * Crypto abstraction class used for TEE invocation.
 *
 * This abstract class gives the processor the crypto functionality needed. It abstracts the underlying TEE invocation.
 * This class is under the hood expected to do the entire secret creation/management/handling (preferably in the TEE).
 */
interface ICrypto {
    /**
     * Decrypts an [encryptedPayload], previously encrypted with AES.
     *
     * [senderPublicKeyBytes] and [sharedSecretSalt] are used to perform a ECDH key exchange.
     * @return decrypted OutputStream
     */
    fun rawStreamDecrypt(
        senderPublicKeyBytes: ByteArray,
        sharedSecretSalt: ByteArray,
        encryptedPayload: ByteArray
    ): ByteArray

    /**
     * Encrypts a [payload], directing it to a recipient using [recipientPublicKeyBytes].
     *
     * [recipientPublicKeyBytes] and [sharedSecretSalt] are used to perform a ECDH key exchange.
     * @return encrypted OutputStream
     */
    fun rawStreamEncrypt(
        recipientPublicKeyBytes: ByteArray,
        sharedSecretSalt: ByteArray,
        payload: ByteArray
    ): ByteArray

    /**
     * Signs a [payload] without prefix/postfix (hence "raw").
     * @return the signature ByteArray
     */
    fun rawSign(payload: ByteArray): ByteArray

    /**
     * @return the public key of the instance.
     */
    fun getPublicKey(compressed: Boolean = true): ByteArray
}
