package com.acurast.attested.executor.utils

import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash

/**
 * This class implements various helper methods for encoding and decoding data.
 */
class Encoding {
    companion object {

        /**
         * Implementation details: https://en.bitcoin.it/wiki/Base58Check_encoding
         */
        fun base58CheckEncode(payload: ByteArray): String {
            val checksum = Sha256Hash.hashTwice(payload, 0, payload.size)
            return Base58.encode(payload + checksum.sliceArray(0..3))
        }

        /**
         * Implementation details: https://en.bitcoin.it/wiki/Base58Check_encoding
         */
        fun base58CheckDecode(payload: String): ByteArray {
            val checkedPayload = Base58.decode(payload)
            val checksum =
                checkedPayload.sliceArray(checkedPayload.size - 4 until checkedPayload.size)
            val rawPayload = checkedPayload.sliceArray(0..checkedPayload.size - 5)
            val calculatedChecksum = Sha256Hash.hashTwice(rawPayload, 0, rawPayload.size)

            return if (checksum.contentEquals(calculatedChecksum.sliceArray(0..3))) {
                rawPayload
            } else {
                ByteArray(0)
            }
        }

    }
}