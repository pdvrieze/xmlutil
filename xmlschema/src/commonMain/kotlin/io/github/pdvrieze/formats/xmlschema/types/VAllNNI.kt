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
    abstract operator fun times(other: Value): VAllNNI
    abstract operator fun times(mult: VNonNegativeInteger): VAllNNI

    abstract fun safeMinus(other: VAllNNI, min: Value = ZERO): VAllNNI
    abstract operator fun plus(other: ULong): VAllNNI

    object UNBOUNDED : VAllNNI() {
        override fun compareTo(other: VAllNNI): Int = when(other){
            UNBOUNDED -> 0
            else -> 1
        }

        override fun times(mult: VNonNegativeInteger): UNBOUNDED = this

        operator fun plus(other: VNonNegativeInteger): VAllNNI = UNBOUNDED
        override operator fun plus(other: VAllNNI): VAllNNI = UNBOUNDED
        override fun plus(other: ULong): VAllNNI = UNBOUNDED

        override operator fun minus(other: VAllNNI): VAllNNI = when (other) {
            UNBOUNDED -> ZERO
            else -> UNBOUNDED
        }

        override fun times(other: Value): VAllNNI = UNBOUNDED

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
            is Value -> value.toULong().safeTimes(other.value.toULong())?.let { Value(it) } ?: UNBOUNDED
            is UNBOUNDED -> UNBOUNDED
        }

        override operator fun times(other: Value): Value = Value(value.toULong() * other.value.toULong())

        override operator fun times(other: VNonNegativeInteger): Value = Value(value.toULong() * other.toULong())

        operator fun minus(other: Value): Value = Value(value.toULong() - other.value.toULong())

        override fun minus(other: VAllNNI): Value = when(other) {
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

        fun ULong.safeTimes(other: ULong): ULong? {
            val result = this * other
            if (this != 0uL && result / this != other) return null
            return result
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

        override operator fun plus(other: ULong): Value {
            return Value(value+other)
        }

        companion object Serializer : KSerializer<Value> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AllNNI.Value", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): Value {
                return Value(VNonNegativeInteger(decoder.decodeString()))
            }

            override fun serialize(encoder: Encoder, value: Value) {
                encoder.encodeString(value.xmlString)
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
            when (value) {
                is UNBOUNDED -> encoder.encodeString("unbounded")
                is Value -> encoder.encodeString(value.xmlString)
            }
        }
    }
}

class AllNNIRange(override val start: VAllNNI.Value, override val endInclusive: VAllNNI) : ClosedRange<VAllNNI> {
    val isSimple: Boolean get() = endInclusive == VAllNNI.ONE && startsWithOne
    val isSingletonRange: Boolean get() = start == endInclusive
    val startsWithOne: Boolean get() = start == VAllNNI.ONE

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

    private operator fun times(mult: VNonNegativeInteger): AllNNIRange {
        return AllNNIRange(this.start*mult, endInclusive*mult)
    }

    /**
     * This function returns a contiguous range if ranges can be merged, if not returns `null`.
     * Note that this is **not** commutative. The outer and inner ranges differ, and multiplier values matter too.
     *
     * For an inner range: (a..b).mergeRanges(c..d) where (c..d) is not a single value, it is required
     * that c*a..c*b is consecutive with (c+1)*a..(c+1)*b. That is the case if (c*b)+1>=(c+1)*a.
     */
    fun mergeRanges(outer: AllNNIRange): AllNNIRange? {
        if (isSimple) return outer
        if (outer.isSimple) return this
        if (startsWithOne && isSingletonRange) return AllNNIRange(start*outer.start, endInclusive*outer.endInclusive)

        val startEnd = outer.start*endInclusive
        val secondStart = (outer.start + VAllNNI.ONE)* start
        if (startEnd+VAllNNI.ONE>=secondStart) return AllNNIRange(start*outer.start, endInclusive*outer.endInclusive)
        return null
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
            (otherRange.endInclusive + newStart) >= endInclusive -> newStart
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AllNNIRange

        if (start != other.start) return false
        if (endInclusive != other.endInclusive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + endInclusive.hashCode()
        return result
    }

    companion object {
        val INFRANGE: AllNNIRange = VAllNNI.ZERO..VAllNNI.UNBOUNDED
        val SINGLERANGE: AllNNIRange = VAllNNI.ONE..VAllNNI.ONE
        val OPTRANGE: AllNNIRange = VAllNNI.ZERO..VAllNNI.ONE
    }
}
