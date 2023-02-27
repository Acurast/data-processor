package com.acurast.attested.executor.protocol.tezos

import com.acurast.attested.executor.utils.hexStringToByteArray
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import it.airgap.tezos.core.coder.encoded.encodeToBytes
import it.airgap.tezos.core.converter.encoded.Signature
import it.airgap.tezos.michelson.micheline.Micheline as TezosMicheline
import it.airgap.tezos.michelson.micheline.dsl.builder.expression.*
import it.airgap.tezos.michelson.micheline.dsl.micheline
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.math.BigInteger


fun V8Array.toMicheline() = Micheline.fromV8Array(this)
fun V8Object.toMicheline() = Micheline.fromV8Object(this)
fun Boolean.toMicheline() = Micheline.fromBool(this)
fun String.toMicheline() = Micheline.fromString(this)
fun Int.toMicheline() = Micheline.fromNat(BigInteger.valueOf(this.toLong()))
fun ByteArray.toMicheline() = Micheline.fromBytes(this)

/**
 * This class translate kotlin values to their Micheline representation.
 */
class Micheline {
    companion object {
        fun fromNat(value: BigInteger) = micheline { int(value.toString()) }
        fun fromBytes(value: ByteArray) = micheline { bytes(value) }
        fun fromString(value: String) = micheline { string(value) }

        fun fromList(values: List<TezosMicheline>) = micheline { sequence(values) }
        fun fromMap(map: Map<TezosMicheline, TezosMicheline>) = micheline {
            sequence(map.map {
                micheline {
                    Elt {
                        key(it.key)
                        value(it.value)
                    }
                }
            })
        }

        fun fromSignature(value: String) =
            micheline { bytes(Signature(value).encodeToBytes()) }

        fun fromPair(first: TezosMicheline, second: TezosMicheline) = micheline {
            Pair {
                arg(first)
                arg(second)
            }
        }

        fun fromBool(value: Boolean) = micheline { if (value) True else False }

        private fun fromEmptyValue() = micheline { Unit }
        fun fromValue(objectValue: Any): TezosMicheline {
            return when (objectValue) {
                is String -> if (objectValue.startsWith("sig")) fromSignature(objectValue) else if (objectValue.startsWith(
                        "0x"
                    )
                ) fromBytes(
                    objectValue.replace("0x", "").hexStringToByteArray()
                ) else objectValue.toMicheline()
                is Int -> objectValue.toMicheline()
                is Boolean -> objectValue.toMicheline()
                is Double -> fromNat(BigDecimal.valueOf(objectValue).toBigInteger())
                is V8Array -> objectValue.toMicheline()
                is V8Object -> if (objectValue == V8.getUndefined()) fromEmptyValue() else objectValue.toMicheline()
                else -> throw UnsupportedEncodingException()
            }
        }

        fun fromV8Array(value: V8Array): TezosMicheline {
            val result = ArrayList<TezosMicheline>()
            for (index in 0 until value.length()) {
                result.add(fromValue(value[index]))
            }
            return fromList(result)
        }

        fun fromV8Object(value: V8Object, start: Int = 0): TezosMicheline {
            return if (value.keys.isEmpty()) {
                fromEmptyValue()
            } else if (value.keys.size - start == 1) {
                val key = value.keys[start]
                fromValue(value[key])
            } else {
                val key = value.keys[start]
                fromPair(fromValue(value[key]), fromV8Object(value, start + 1))
            }
        }
    }
}