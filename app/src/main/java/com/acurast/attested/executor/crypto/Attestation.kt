package com.acurast.attested.executor.crypto

import com.acurast.attested.executor.Constants
import java.security.cert.Certificate

class Attestation {
    companion object {
        /**
         * Get the chain of X.509 certificates associated with the hardware-backed keystore.
         *
         * @see <a href="https://developer.android.com/training/articles/security-key-attestation">Key Attestation</a>
         */
        fun getCertificateChain(): Array<Certificate> {
            // Get the android keystore provider
            val keyStore = CryptoLegacy.getKeyStore()

            return keyStore.getCertificateChain(Constants.PROCESSOR_KEY_ALIAS)
        }
    }
}