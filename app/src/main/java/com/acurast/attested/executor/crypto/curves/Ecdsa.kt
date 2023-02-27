package com.acurast.attested.executor.crypto.curves

import com.acurast.attested.executor.crypto.CryptoLegacy
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.rlp.RlpType
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class Ecdsa(curve: Curve) {
    fun bytesToBigInt(bs: ByteArray): BigInteger {
        if (bs.isEmpty()) return BigInteger.ZERO
        var num: BigInteger = BigInteger.ZERO
        for (b in bs) {
            var n = b.toLong()
            if (n < 0) n += 256
            num = num.shiftLeft(8).add(BigInteger.valueOf(n))
        }
        return num
    }

    fun bigIntToBytes(n: BigInteger): ByteArray {
        val bs: ByteArray = n.toByteArray()
        return if (bs[0] == Byte.MIN_VALUE) Arrays.copyOfRange(bs, 1, bs.size) else bs
    }

    /**
     * Parameters for y^2 = x^3 + a*x + b (mod = P).
     * Base point G(x,y).
     */
    val P: BigInteger
    val N: BigInteger
    val A: BigInteger
    val B: BigInteger
    val Gx: BigInteger
    val Gy: BigInteger

    init {
        P = curve.P
        N = curve.N
        A = curve.A
        B = curve.B
        Gx = curve.Gx
        Gy = curve.Gy
    }

    /**
     * Initialize several numbers in range 0~9.
     */
    val NUM: Array<BigInteger> = arrayOf<BigInteger>(
            BigInteger.ZERO, BigInteger.ONE, BigInteger("2"), BigInteger("3"), BigInteger("4"),
            BigInteger("5"), BigInteger("6"), BigInteger("7"), BigInteger("8"), BigInteger("9")
    )


    /**
     * @param curve the Elliptic Curves.
     *
     * @return instance of algorithm.
     */
    fun from(curve: Curve): Ecdsa {
        return Ecdsa(curve)
    }

    /**
     * @param privateKeyBytes private key content bytes.
     * @param hashBytes hash content bytes.
     *
     * @return signature string.
     */
    fun sign(privateKeyBytes: ByteArray, hashBytes: ByteArray, randomBytes: ByteArray = CryptoLegacy.secureRandomBytes()): ByteArray {
        val priv: BigInteger = bytesToBigInt(privateKeyBytes)
        val hash: BigInteger = bytesToBigInt(hashBytes)
        val k = BigInteger(randomBytes)
        val p: Array<BigInteger> = fastMultiply(Gx, Gy, NUM[1], k)
        val z: BigInteger = inv(p[2], P)
        val r: BigInteger = z.pow(2).multiply(p[0]).mod(P)
        val sRaw: BigInteger = inv(k, N).multiply(hash.add(r.multiply(priv))).mod(N)
        val s: BigInteger =
                if (sRaw.multiply(NUM[2]).compareTo(N) < 0) sRaw else N.subtract(sRaw)

        // Skip v for now
        // val y: BigInteger = z.pow(3).multiply(p[1]).mod(P)
        // val v: BigInteger = y.mod(NUM[2]).xor(if (sRaw.multiply(NUM[2]).compareTo(N) < 0) y.mod(NUM[2]) else NUM[1])

        return Numeric.toBytesPadded(r, 32) + Numeric.toBytesPadded(s, 32)
    }


    /**
     * @param pubKeyBytes public key content bytes.
     * @param hashBytes hash content bytes.
     * @param sig signature content.
     *
     * @return if the hash content has been falsified, return false.
     */
    fun verify(pubKeyBytes: ByteArray, hashBytes: ByteArray, sig: String): Boolean {
        if (sig.length != 130) throw RuntimeException("Invalid signature, $sig")
        if (pubKeyBytes.size != 64) throw RuntimeException("Invalid publickKey.")
        val r = BigInteger(sig.substring(0, 64), 16)
        val s = BigInteger(sig.substring(64, 128), 16)
        if (r.mod(N).equals(NUM[0]) || s.mod(N).equals(NUM[0])) return false
        val xBytes = ByteArray(32)
        val yBytes = ByteArray(32)
        System.arraycopy(pubKeyBytes, 0, xBytes, 0, 32)
        System.arraycopy(pubKeyBytes, 32, yBytes, 0, 32)
        val x: BigInteger = bytesToBigInt(xBytes)
        val y: BigInteger = bytesToBigInt(yBytes)
        val w: BigInteger = inv(s, N)
        val z: BigInteger = bytesToBigInt(hashBytes)
        val u1: BigInteger = z.multiply(w).mod(N)
        val u2: BigInteger = r.multiply(w).mod(N)
        val p: Array<BigInteger> = fastMultiply(Gx, Gy, NUM[1], u1)
        val pz: BigInteger = inv(p[2], P)
        val px: BigInteger = pz.pow(2).multiply(p[0]).mod(P)
        val py: BigInteger = pz.pow(3).multiply(p[1]).mod(P)
        val q: Array<BigInteger> = fastMultiply(x, y, NUM[1], u2)
        val qz: BigInteger = inv(q[2], P)
        val qx: BigInteger = qz.pow(2).multiply(q[0]).mod(P)
        val qy: BigInteger = qz.pow(3).multiply(q[1]).mod(P)
        val g: Array<BigInteger> = fastAdd(px, py, NUM[1], qx, qy, NUM[1])
        val gz: BigInteger = inv(g[2], P)
        val gx: BigInteger = gz.pow(2).multiply(g[0]).mod(P)
        return r.equals(gx)
    }


    /**
     * @param privKeyBytes private key content bytes.
     *
     * @return bytes calculate public key bytes from private key bytes.
     */
    fun privateKeyToPublicKey(privKeyBytes: ByteArray): ByteArray {
        val priv: BigInteger = bytesToBigInt(privKeyBytes)
        if (priv.compareTo(N) > 0) throw RuntimeException("Invalid private key.")
        val p: Array<BigInteger> = fastMultiply(Gx, Gy, NUM[1], priv)
        val z: BigInteger = inv(p[2], P)
        val x: BigInteger = z.pow(2).multiply(p[0]).mod(P)
        val y: BigInteger = z.pow(3).multiply(p[1]).mod(P)
        val xBs: ByteArray = bigIntToBytes(x)
        val yBs: ByteArray = bigIntToBytes(y)
        val pubKeyBytes = ByteArray(64)
        System.arraycopy(xBs, 0, pubKeyBytes, 0, xBs.size)
        System.arraycopy(yBs, 0, pubKeyBytes, 32, yBs.size)
        return pubKeyBytes
    }

    /**
     * @param hashBytes hash content bytes.
     * @param sig signature content.
     *
     * @return bytes calculate public key bytes from signature and hash.
     */
    fun recoverToPublicKey(hashBytes: ByteArray, sig: String): ByteArray {
        if (sig.length != 130) throw RuntimeException("Invalid signature, $sig")
        val _27 = BigInteger("27")
        val _34 = BigInteger("34")
        val r = BigInteger(sig.substring(0, 64), 16)
        val s = BigInteger(sig.substring(64, 128), 16)
        if (r.mod(N).equals(NUM[0]) || s.mod(N).equals(NUM[0])) throw RuntimeException(
                "Invalid signature, $sig"
        )
        val v: BigInteger = BigInteger(sig.substring(128, 130), 16).add(_27)
        if (v.compareTo(_27) < 0 || v.compareTo(_34) > 0) throw RuntimeException("Invalid signature, $sig")
        val x: BigInteger = r
        val num: BigInteger = x.pow(3).add(x.multiply(A).add(B)).mod(P)
        var y: BigInteger = num.modPow(P.add(NUM[1]).divide(NUM[4]), P)
        if (y.mod(NUM[2]).xor(v.mod(NUM[2])).equals(NUM[0])) y = P.subtract(y)
        if (!y.pow(2).subtract(num).mod(P)
                        .equals(NUM[0])
        ) throw RuntimeException("Invalid signature, $sig")
        val z: BigInteger = bytesToBigInt(hashBytes)
        val GZ: Array<BigInteger> = fastMultiply(Gx, Gy, NUM[1], N.subtract(z).mod(N))
        val XY: Array<BigInteger> = fastMultiply(x, y, NUM[1], s)
        val QR: Array<BigInteger> = fastAdd(GZ[0], GZ[1], GZ[2], XY[0], XY[1], XY[2])
        val Q: Array<BigInteger> = fastMultiply(QR[0], QR[1], QR[2], inv(r, N))
        val pubZ: BigInteger = inv(Q[2], P)
        val left: BigInteger = pubZ.pow(2).multiply(Q[0]).mod(P)
        val right: BigInteger = pubZ.pow(3).multiply(Q[1]).mod(P)
        val leftBs: ByteArray = bigIntToBytes(left)
        val rightBs: ByteArray = bigIntToBytes(right)
        val pubBytes = ByteArray(leftBs.size + rightBs.size)
        System.arraycopy(leftBs, 0, pubBytes, 0, leftBs.size)
        System.arraycopy(rightBs, 0, pubBytes, leftBs.size, rightBs.size)
        return pubBytes
    }

    fun quickPow(n: BigInteger, m: BigInteger, mod: BigInteger): BigInteger {
        if (m.equals(NUM[1])) return n.mod(mod)
        val a: Array<BigInteger> = m.divideAndRemainder(NUM[2])
        val r: BigInteger = quickPow(n, a[0], mod).pow(2).mod(mod)
        return if (a[1].equals(NUM[0])) r else n.multiply(r).mod(mod)
    }

    fun fastMultiply(
            a0: BigInteger,
            a1: BigInteger,
            a2: BigInteger,
            n: BigInteger
    ): Array<BigInteger> {
        if (a1.equals(NUM[0]) || n.equals(NUM[0])) return arrayOf<BigInteger>(
                NUM[0],
                NUM[0],
                NUM[1]
        )
        if (n.equals(NUM[1])) return arrayOf<BigInteger>(a0, a1, a2)
        if (n.signum() < 0 || n.compareTo(N) >= 0) return fastMultiply(a0, a1, a2, n.mod(N))
        val a: Array<BigInteger> = fastMultiply(a0, a1, a2, n.shiftRight(1))
        val p: Array<BigInteger> = fastDouble(a[0], a[1], a[2])
        return if (n.mod(NUM[2]).equals(NUM[0])) {
            p
        } else if (n.mod(NUM[2]).equals(NUM[1])) {
            fastAdd(p[0], p[1], p[2], a0, a1, a2)
        } else throw RuntimeException("Invalid BigInteger. " + n.toString(16))
    }

    fun fastDouble(a0: BigInteger, a1: BigInteger, a2: BigInteger): Array<BigInteger> {
        val ysq: BigInteger = a1.pow(2).mod(P)
        val s: BigInteger = ysq.multiply(a0).multiply(NUM[4]).mod(P)
        val m: BigInteger = a0.pow(2).multiply(NUM[3]).add(a2.pow(4).multiply(A)).mod(P)
        val nx: BigInteger = m.pow(2).subtract(s.multiply(NUM[2])).mod(P)
        val ny: BigInteger = m.multiply(s.subtract(nx)).subtract(ysq.pow(2).multiply(NUM[8])).mod(P)
        val nz: BigInteger = a1.multiply(a2).multiply(NUM[2]).mod(P)
        return arrayOf<BigInteger>(nx, ny, nz)
    }

    fun fastAdd(
            p0: BigInteger, p1: BigInteger, p2: BigInteger,
            q0: BigInteger, q1: BigInteger, q2: BigInteger
    ): Array<BigInteger> {
        val u1: BigInteger = q2.pow(2).multiply(p0).mod(P)
        val u2: BigInteger = p2.pow(2).multiply(q0).mod(P)
        val s1: BigInteger = q2.pow(3).multiply(p1).mod(P)
        val s2: BigInteger = p2.pow(3).multiply(q1).mod(P)
        if (u1.equals(u2)) {
            return if (s1.equals(s2)) fastDouble(p0, p1, p2) else arrayOf<BigInteger>(
                    NUM[0],
                    NUM[0], NUM[1]
            )
        }
        val h: BigInteger = u2.subtract(u1)
        val r: BigInteger = s2.subtract(s1)
        val h2: BigInteger = h.pow(2).mod(P)
        val h3: BigInteger = h2.multiply(h).mod(P)
        val u1h2: BigInteger = u1.multiply(h2).mod(P)
        val nx: BigInteger = r.pow(2).subtract(h3).subtract(u1h2.multiply(NUM[2])).mod(P)
        val ny: BigInteger = r.multiply(u1h2.subtract(nx)).subtract(s1.multiply(h3)).mod(P)
        val nz: BigInteger = h.multiply(p2).multiply(q2).mod(P)
        return arrayOf<BigInteger>(nx, ny, nz)
    }

    fun inv(a: BigInteger, b: BigInteger): BigInteger {
        if (a.equals(NUM[0])) return NUM[0]
        var lm: BigInteger = NUM[1]
        var hm: BigInteger = NUM[0]
        var low: BigInteger = a.mod(b)
        var high: BigInteger = b
        while (low.compareTo(NUM[1]) > 0) {
            val r: BigInteger = high.divide(low)
            val nm: BigInteger = hm.subtract(lm.multiply(r))
            val ne: BigInteger = high.subtract(low.multiply(r))
            hm = lm
            high = low
            lm = nm
            low = ne
        }
        return lm.mod(b)
    }

    fun concatBytes(vararg bytes: ByteArray): ByteArray {
        var l = 0
        for (bs in bytes) l += bs.size
        val out = ByteArray(l)
        var s = 0
        for (bs in bytes) {
            System.arraycopy(bs, 0, out, s, bs.size)
            s += bs.size
        }
        return out
    }
}