/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// TODO Unify this with VNonNegativeInteger
@Serializable(VAllNNI.Serializer::class)
sealed class VAllNNI: Comparable<VAllNNI> { //TODO make interface

    abstract operator fun plus(other: VAllNNI): VAllNNI

    abstract operator fun minus(other: VAllNNI): VAllNNI

    abstract operator fun times(other: VAllNNI): VAllNNI

    abstract fun safeMinus(other: VAllNNI, min: Value = ZERO): VAllNNI

    object UNBOUNDED : VAllNNI() {
        override fun compareTo(other: VAllNNI): Int = when(other){
            UNBOUNDED -> 0
            else -> 1
        }

        operator fun plus(other: VNonNegativeInteger): VAllNNI = UNBOUNDED
        override operator fun plus(other: VAllNNI): VAllNNI = UNBOUNDED
        override operator fun minus(other: VAllNNI): VAllNNI = when (other) {
            UNBOUNDED -> ZERO
            else -> UNBOUNDED
        }
        override operator fun times(other: VAllNNI): VAllNNI = UNBOUNDED

        override fun safeMinus(other: VAllNNI, min: Value): VAllNNI = when(other) {
            UNBOUNDED -> min
            else -> UNBOUNDED
        }

        override fun toString(): String = "unbounded"
    }

    @Serializable(Value.Serializer::class)
    class Value(val value: VNonNegativeInteger): VAllNNI(), VNonNegativeInteger by value {
        constructor(value: ULong): this(VNonNegativeInteger(value))
        constructor(value: UInt): this(VNonNegativeInteger(value))

        override fun compareTo(other: VAllNNI): Int {
            return when (other) {
                is UNBOUNDED -> -1
                is Value -> value.compareTo(other.value)
            }
        }

        override operator fun plus(other: VNonNegativeInteger): VNonNegativeInteger = when (other) {
            is Value -> Value(value + other.value)
            else -> Value(value + other)
        }

        override operator fun plus(other: VAllNNI): VAllNNI = when (other) {
            is Value -> Value(value + other.value)
            is UNBOUNDED -> UNBOUNDED
        }

        operator fun plus(other: Value): Value = Value(value + other.value)

        override operator fun times(other: VAllNNI): VAllNNI = when (other) {
            is Value -> Value(value.toULong() * other.value.toULong())
            is UNBOUNDED -> UNBOUNDED
        }

        operator fun times(other: Value): Value = Value(value.toULong() * other.value.toULong())

        operator fun minus(other: Value): Value = Value(value.toULong() - other.value.toULong())

        override fun minus(other: VAllNNI): VAllNNI = when(other) {
            UNBOUNDED -> ZERO
            is Value -> Value((value.toULong() - other.value.toULong()))
        }

        override fun safeMinus(other: VAllNNI, min: Value): VAllNNI = when {
            other !is Value -> min // unbounded
            value > (other.value + min.value) -> this - other
            else -> min
        }

        fun safeMinus(other: Value, min: Value): Value = when {
            (value + min.value) > other.value -> this - other
            else -> min
        }

        fun safeMinus(other: Value): Value = when {
            value > other.value -> this - other
            else -> ZERO
        }

        override fun toString(): String = value.toString()

        override fun equals(other: Any?): Boolean {
            if (this===other) return true
            return when (other) {
                is VNonNegativeInteger -> compareTo(other) == 0
                is ULong -> toULong() == other
                is UInt -> toULong() == other.toULong()
                else -> false
            }
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        companion object Serializer : KSerializer<Value> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AllNNI.Value", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): Value {
                return Value(VNonNegativeInteger(decoder.decodeString()))
            }

            override fun serialize(encoder: Encoder, value: Value) {
                encoder.encodeString(value.toString())
            }

        }
    }

    companion object Serializer: KSerializer<VAllNNI> {

        val ONE = Value(VNonNegativeInteger.ONE)

        val ZERO = Value(VNonNegativeInteger.ZERO)

        operator fun invoke(v: Int): Value = Value(v.toULong())

        operator fun invoke(v: UInt): Value = Value(v.toULong())

        operator fun invoke(v: Long): Value = Value(v.toULong())

        operator fun invoke(v: ULong): Value = Value(v)

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AllNNI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VAllNNI = when (val v = decoder.decodeString()) {
            "unbounded" -> UNBOUNDED
            else -> Value(VNonNegativeInteger(v))
        }

        override fun serialize(encoder: Encoder, value: VAllNNI) {
            encoder.encodeString(value.toString())
        }
    }
}

class AllNNIRange(override val start: VAllNNI.Value, override val endInclusive: VAllNNI) : ClosedRange<VAllNNI> {
    val isSimple: Boolean get() = endInclusive == VAllNNI.ONE && start == VAllNNI.ONE

    constructor(startNNI: VNonNegativeInteger, endInclusive: VAllNNI) : this(VAllNNI.Value(startNNI), endInclusive)
    constructor(
        startNNI: VNonNegativeInteger,
        endInclusive: VNonNegativeInteger
    ) : this(start = VAllNNI.Value(startNNI), VAllNNI.Value(endInclusive))

    override fun contains(value: VAllNNI): Boolean = when (value) {
        is VAllNNI.UNBOUNDED -> false
        else -> start <= value && value <= endInclusive
    }

    operator fun contains(other: AllNNIRange): Boolean {
        return start <= other.start && endInclusive >= other.endInclusive
    }

    override fun isEmpty(): Boolean {
        return endInclusive !is VAllNNI.UNBOUNDED && start < endInclusive
    }

    operator fun times(other: AllNNIRange): AllNNIRange? = when {
        isSimple -> other
        other.isSimple -> this
        start > VAllNNI.ONE -> null
        else -> AllNNIRange(start * other.start, endInclusive * other.endInclusive)
    }

    override fun toString(): String = when(endInclusive) {
        VAllNNI.UNBOUNDED -> "[$start, →)"
        else -> "[$start, $endInclusive]"
    }

    operator fun plus(other: AllNNIRange): AllNNIRange {
        return AllNNIRange(start + other.start, endInclusive + other.endInclusive)
    }

    operator fun minus(otherRange: AllNNIRange): AllNNIRange? {
        if (otherRange.endInclusive > endInclusive) return null

        val newStart: VAllNNI.Value = when { // out of range start is allowed
            otherRange.start > start -> VAllNNI.ZERO
            else -> start - otherRange.start
        }
        val newEnd: VAllNNI = when {
            otherRange.endInclusive >= (endInclusive + newStart) -> newStart
            else -> endInclusive - otherRange.endInclusive
        }

        return AllNNIRange(newStart, newEnd)
    }

    operator fun minus(other: VAllNNI): AllNNIRange {

        val newStart: VAllNNI.Value = when {
            other !is VAllNNI.Value ||
            other >= start -> VAllNNI.ZERO
            else -> start - other
        }
        val newEnd: VAllNNI = when {
            other > endInclusive -> throw IllegalArgumentException("Int underflow ($endInclusive-$other)")
            else -> endInclusive - other
        }

        return AllNNIRange(newStart, newEnd)
    }
}
