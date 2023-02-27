package com.acurast.attested.executor.native

import android.util.Log

enum class SB_CURVE(val code: Int) {
    /** NIST P-256 */
    SB_SW_CURVE_P256(0),
    /** SECG secp256k1 */
    SB_SW_CURVE_SECP256K1(1),
}

enum class SB_ERROR(val code: Int) {
    /** No error has occurred and the output parameters are valid. */
    SB_SUCCESS(0),

    /** The entropy input used to seed the DRBG is too small */
    SB_ERROR_INSUFFICIENT_ENTROPY(1 shl 0),

    /** The input to the DRBG is too large */
    SB_ERROR_INPUT_TOO_LARGE(1 shl 1),

    /** The DRBG generate request is too large */
    SB_ERROR_REQUEST_TOO_LARGE(1 shl 2),

    /** The DRBG must be reseeded and the operation can be retried */
    SB_ERROR_RESEED_REQUIRED(1 shl 3),

    /** The DRBG has produced an extremely low-probability output (p < 2^-64) */
    SB_ERROR_DRBG_FAILURE(1 shl 4),

    /** The curve supplied is invalid */
    SB_ERROR_CURVE_INVALID(1 shl 5),

    /** The supplied private key is invalid */
    SB_ERROR_PRIVATE_KEY_INVALID(1 shl 6),

    /** The supplied public key is invalid */
    SB_ERROR_PUBLIC_KEY_INVALID(1 shl 7),

    /** The signature is invalid */
    SB_ERROR_SIGNATURE_INVALID(1 shl 8),

    /** The DRBG has not been nullified but not initialized */
    SB_ERROR_DRBG_UNINITIALIZED(1 shl 9),

    /** The context was initialized by a \c _start routine that does not match
     *  the \c _continue or \c _finish routine being called. */
    SB_ERROR_INCORRECT_OPERATION(1 shl 10),

    /** The \c _finish routine was called, but the operation was not yet
     *  finished. */
    SB_ERROR_NOT_FINISHED(1 shl 11),

    /** Additional input was required by the DRBG, but not provided. */
    SB_ERROR_ADDITIONAL_INPUT_REQUIRED(1 shl 12);

    companion object {
        fun valueOf(code: Int): SB_ERROR {
            return when (code) {
                SB_SUCCESS.code -> SB_SUCCESS
                SB_ERROR_INSUFFICIENT_ENTROPY.code -> SB_ERROR_INSUFFICIENT_ENTROPY
                SB_ERROR_INPUT_TOO_LARGE.code -> SB_ERROR_INPUT_TOO_LARGE
                SB_ERROR_REQUEST_TOO_LARGE.code -> SB_ERROR_REQUEST_TOO_LARGE
                SB_ERROR_RESEED_REQUIRED.code -> SB_ERROR_RESEED_REQUIRED
                SB_ERROR_DRBG_FAILURE.code -> SB_ERROR_DRBG_FAILURE
                SB_ERROR_CURVE_INVALID.code -> SB_ERROR_CURVE_INVALID
                SB_ERROR_PRIVATE_KEY_INVALID.code -> SB_ERROR_PRIVATE_KEY_INVALID
                SB_ERROR_PUBLIC_KEY_INVALID.code -> SB_ERROR_PUBLIC_KEY_INVALID
                SB_ERROR_SIGNATURE_INVALID.code -> SB_ERROR_SIGNATURE_INVALID
                SB_ERROR_DRBG_UNINITIALIZED.code -> SB_ERROR_DRBG_UNINITIALIZED
                SB_ERROR_INCORRECT_OPERATION.code -> SB_ERROR_INCORRECT_OPERATION
                SB_ERROR_NOT_FINISHED.code -> SB_ERROR_NOT_FINISHED
                SB_ERROR_ADDITIONAL_INPUT_REQUIRED.code -> SB_ERROR_ADDITIONAL_INPUT_REQUIRED
                else -> throw Exception("Cannot convert ${this::class.simpleName} error code: $code")
            }
        }
    }
}

class SweetBSigner(val curve: SB_CURVE) {
    external fun jniComputePublicKey(key: ByteArray, curve: Int): ByteArray?
    external fun jniCompressPublicKey(key: ByteArray, curve: Int): ByteArray?
    external fun jniVerifyPublicKey(key: ByteArray, curve: Int): Int
    external fun jniSignMessageDigest(key: ByteArray, message: ByteArray, curve: Int): ByteArray?
    external fun jniGenerateSharedSecret(privateKey: ByteArray, publicKey: ByteArray, curve: Int): ByteArray?

    companion object {
        init {
            System.loadLibrary("sweet_b")
        }
    }

    /**
     * Generate an ECDH shared secret using the given private key and public key.
     */
    fun generateSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        return this.jniGenerateSharedSecret(privateKey, publicKey, curve.code)
            ?: throw Exception("Could not generate shared secret.")
    }

    /**
     * Signs the 32-byte message digest using the provided private key.
     */
    fun signMessageDigest(key: ByteArray, digest: ByteArray): ByteArray {
        return this.jniSignMessageDigest(key, digest, curve.code)
            ?: throw Exception("Could not sign digest message.")
    }

    /**
     * Returns the public key for the supplied private key.
     */
    fun computePublicKey(key: ByteArray): ByteArray {
        return this.jniComputePublicKey(key, curve.code)
            ?: throw Exception("Could not compute public key.")
    }

    /**
     * Compress the supplied public key into a single 256-bit value and an extra "sign" bit.
     */
    fun compressPublicKey(publicKey: ByteArray): ByteArray {
        return this.jniCompressPublicKey(publicKey, curve.code)
            ?: throw Exception("Could not compress public key.")
    }


    /**
     * Validate the supplied public key.
     */
    fun verifyPublicKey(key: ByteArray): Boolean {
        return when (val result = this.jniVerifyPublicKey(key, curve.code)) {
            SB_ERROR.SB_SUCCESS.code -> true
            else -> {
                Log.d("${this::class.simpleName}#verifyPublicKey", SB_ERROR.valueOf(result).name)
                return false
            }
        }
    }
}