/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2014-2016 the libsecp256k1 contributors
 * Copyright 2022 Papers AG
 *
 * https://github.com/rsksmart/bitcoinj-thin/blob/master/src/main/java/co/rsk/bitcoinj/core/BtcECKey.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acurast.attested.executor.crypto.curves

import com.acurast.attested.executor.utils.toHex
import com.acurast.attested.executor.utils.trim
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.LinuxSecureRandom
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.asn1.x9.X9IntegerConverter
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.math.ec.ECAlgorithms
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.FixedPointUtil
import org.bouncycastle.math.ec.custom.sec.SecP256K1FieldElement
import org.bouncycastle.math.ec.custom.sec.SecP256R1FieldElement
import java.math.BigInteger
import java.security.InvalidParameterException
import java.security.SecureRandom

val Secp256r1 = RecoverableEcdsa("secp256r1")
val Secp256k1 = RecoverableEcdsa("secp256k1")

class RecoverableEcdsa(ecName: String) {
    /**
     * The parameters of secp256r1 or secp256k1 curves.
     */
    private var CURVE_PARAMS: X9ECParameters
    private var CURVE: ECDomainParameters
    private var secureRandom: SecureRandom
    private var Q: BigInteger // Prime field

    init {

        // Init proper random number generator, as some old Android installations have bugs that make it unsecure.
        if (Utils.isAndroidRuntime())
            LinuxSecureRandom()

        // Tell Bouncy Castle to precompute data that's needed during calculations.
        CURVE_PARAMS = CustomNamedCurves.getByName(ecName)
        Q = when (ecName) {
            "secp256r1" -> SecP256R1FieldElement.Q
            "secp256k1" -> SecP256K1FieldElement.Q
            else -> throw InvalidParameterException("An unknown curve $ecName was provided.")
        }
        FixedPointUtil.precompute(CURVE_PARAMS.g)
        CURVE = ECDomainParameters(
            CURVE_PARAMS.curve,
            CURVE_PARAMS.getG(),
            CURVE_PARAMS.getN(),
            CURVE_PARAMS.h
        )
        secureRandom = SecureRandom()
    }

    fun findRecoverId(
        r: ByteArray,
        s: ByteArray,
        publicKey: ByteArray,
        messageHash: ByteArray
    ): Byte {
        val R = BigInteger(r.toHex(), 16)
        val S = BigInteger(s.toHex(), 16)
        val PK = BigInteger(publicKey.toHex(), 16)
        // Now we have to work backwards to figure out the recId needed to recover the signature.
        var recId = -1
        for (i in 0..3) {
            val k: BigInteger? = recoverFromSignature(R, S, i, messageHash)
            if (k != null && k.equals(PK)) {
                recId = i
                break
            }
        }
        if (recId == -1) {
            throw RuntimeException(
                "Could not construct a recoverable key. Are your credentials valid?"
            )
        }

        // recover identifier
        return recId.toByte()
    }

    /**
     * Decompress a compressed public key (x-coord and y-coord).
     */
    private fun decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint? {
        val x9 = X9IntegerConverter()
        val compEnc: ByteArray = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.curve))
        compEnc[0] = (if (yBit) 0x03 else 0x02).toByte()
        return CURVE.curve.decodePoint(compEnc)
    }

    /**
     * Given the components of a signature and a selector value, recover and return the public key
     * that generated the signature according to the algorithm in SEC1v2 section 4.1.6.
     *
     *
     * The recId is an index from 0 to 3 which indicates which of the 4 possible keys is the
     * correct one. Because the key recovery operation yields multiple potential keys, the correct
     * key must either be stored alongside the signature, or you must be willing to try each recId
     * in turn until you find one that outputs the key you are expecting.
     *
     *
     * If this method returns null it means recovery was not possible and recId should be
     * iterated.
     *
     *
     * Given the above two points, a correct usage of this method is inside a for loop from 0 to
     * 3, and if the output is null OR a key that is not the one you expect, you try again with the
     * next recId.
     *
     * @param recId Which possible key to recover.
     * @param r the R component of the signature, wrapped.
     * @param s the S component of the signature, wrapped.
     * @param message Hash of the data that was signed.
     * @return An ECKey containing only the public part, or null if recovery wasn't possible.
     */
    fun recoverFromSignature(
        r: BigInteger,
        s: BigInteger,
        recId: Int,
        message: ByteArray
    ): BigInteger? {
        assert(recId in 0..3) { "recId must be in the range of [0, 3]" }
        assert(r.signum() >= 0) { "r must be positive" }
        assert(s.signum() >= 0) { "s must be positive" }

        // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
        //   1.1 Let x = r + jn
        val n = CURVE.n; // Curve order.
        val i = BigInteger.valueOf(recId.toLong() / 2)
        val x = r.add(i.multiply(n))
        //   1.2. Convert the integer x to an octet string X of length mlen using the conversion
        //        routine specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
        //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R
        //        using the conversion routine specified in Section 2.3.4. If this conversion
        //        routine outputs "invalid", then do another iteration of Step 1.
        //
        // More concisely, what these points mean is to use X as a compressed public key.
        if (x.compareTo(Q) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
            return null
        }
        // Compressed keys require you to know an extra bit of data about the y-coord as there are
        // two possibilities. So it's encoded in the recId.
        val R = decompressKey(x, recId and 1 == 1)
        //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers
        //        responsibility).
        if (R == null || !R.multiply(n).isInfinity()) {
            return null
        }
        //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
        val e = BigInteger(1, message)
        //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via
        //        iterating recId)
        //   1.6.1. Compute a candidate public key as:
        //               Q = mi(r) * (sR - eG)
        //
        // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
        //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n).
        // In the above equation ** is point multiplication and + is point addition (the EC group
        // operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the mod. For
        // example the additive inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and
        // -3 mod 11 = 8.
        val eInv = BigInteger.ZERO.subtract(e).mod(n)
        val rInv = r.modInverse(n)
        val srInv = rInv.multiply(s).mod(n)
        val eInvrInv = rInv.multiply(eInv).mod(n)
        val q: ECPoint = ECAlgorithms.sumOfTwoMultiplies(CURVE.g, eInvrInv, R, srInv)
        val qBytes: ByteArray = q.getEncoded(true)
        // We remove the prefix
        return BigInteger(qBytes)
    }
}