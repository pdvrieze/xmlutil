/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(VNonNegativeInteger.Serializer::class)
interface VNonNegativeInteger : VInteger {
    fun toULong(): ULong
    fun toUInt(): UInt

    operator fun plus(other: VNonNegativeInteger): VNonNegativeInteger
    operator fun times(other: VNonNegativeInteger): VNonNegativeInteger

    fun coerceAtMost(maxMax: VAllNNI): VNonNegativeInteger = when {
        maxMax is VAllNNI.Value && maxMax.value < this -> maxMax
        else -> this
    }

    override fun compareTo(other: VInteger): Int = when (other) {
        is VNonNegativeInteger -> toULong().compareTo(other.toULong())
        else -> {
            val ol = other.toLong()
            if (ol < 0) 1 else toULong().compareTo(ol.toULong())
        }
    }

    operator fun compareTo(other: VNonNegativeInteger): Int =
        toULong().compareTo(other.toULong())

    operator fun compareTo(other: VAllNNI): Int = when {
        other !is VAllNNI.Value -> -1 // If not a value then unbounded
        else -> toULong().compareTo(other.toULong())
    }

    private class Inst(override val xmlString: String) : VNonNegativeInteger {
        override fun toLong(): Long = xmlString.toLong()

        override fun toInt(): Int = xmlString.toInt()

        override fun toULong(): ULong = xmlString.toULong()

        override fun toUInt(): UInt = xmlString.toUInt()

        override fun plus(other: VNonNegativeInteger): VNonNegativeInteger {
            return VUnsignedLong(toULong() + other.toULong())
        }

        override fun times(other: VNonNegativeInteger): VNonNegativeInteger {
            return VUnsignedLong(toULong() * other.toULong())
        }

        override fun toString(): String = "${xmlString}u"
    }

    class Serializer : KSerializer<VNonNegativeInteger> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("nonNegativeInteger", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VNonNegativeInteger {
            return invoke(decoder.decodeString().trim())
        }

        override fun serialize(encoder: Encoder, value: VNonNegativeInteger) {
            encoder.encodeString(value.xmlString)
        }

        companion object
    }

    companion object {
        val ONE = VUnsignedInt(1u)
        val ZERO = VUnsignedInt(0u)

        operator fun invoke(charSequence: CharSequence) =
            invoke(rawValue = charSequence.toString())

        operator fun invoke(rawValue: String): VNonNegativeInteger = when {
            rawValue.length > MAXLONG.length -> Inst(rawValue)

            rawValue == "0" -> ZERO
            rawValue == "1" -> ONE

            rawValue.length == MAXLONG.length && (rawValue[0] == '0' || rawValue[0] == '1')
                    && rawValue.substring(1).toLong() <= MAXNONSIGNDIGITS ->
                invoke(rawValue.toULong())

            rawValue.toLong() <= MAXUINT -> invoke(rawValue.toUInt())

            else -> invoke(rawValue.toULong())
        }

        operator fun invoke(value: ULong): VUnsignedLong = VUnsignedLong(value)
        operator fun invoke(value: UInt): VUnsignedInt = VUnsignedInt(value)
        operator fun invoke(value: Long): VUnsignedLong = run { require(value >= 0); VUnsignedLong(value.toULong()) }
        operator fun invoke(value: Int): VUnsignedInt = run { require(value >= 0); VUnsignedInt(value.toUInt()) }

        private val MAXLONG = ULong.MAX_VALUE.toString()
        private val MAXNONSIGNDIGITS = MAXLONG.substring(1).toLong()
        private val MAXUINT = UInt.MAX_VALUE.toLong()

    }
}


