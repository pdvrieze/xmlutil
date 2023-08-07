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

    abstract operator fun times(other: VAllNNI): VAllNNI

    object UNBOUNDED : VAllNNI() {
        override fun compareTo(other: VAllNNI): Int = when(other){
            UNBOUNDED -> 0
            else -> 1
        }

        operator fun plus(other: VNonNegativeInteger): VAllNNI = UNBOUNDED
        override operator fun plus(other: VAllNNI): VAllNNI = UNBOUNDED
        override operator fun times(other: VAllNNI): VAllNNI = UNBOUNDED

        override fun toString(): String = "unbounded"
    }

    @Serializable(Value.Serializer::class)
    class Value(val value: VNonNegativeInteger): VAllNNI(), VNonNegativeInteger by value {
        constructor(value: ULong): this(VNonNegativeInteger(value))
        constructor(value: UInt): this(VNonNegativeInteger(value))

        override fun compareTo(other: VAllNNI): Int {
            return when (other) {
                is UNBOUNDED -> -1
                is Value -> toULong().compareTo(other.toULong())
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

        override operator fun times(other: VAllNNI): VAllNNI = when (other) {
            is Value -> Value(value.toULong() * other.value.toULong())
            is UNBOUNDED -> UNBOUNDED
        }

        operator fun times(other: Value): Value = Value(value.toULong() * other.value.toULong())

        override fun toString(): String = value.toString()

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
    constructor(startNNI: VNonNegativeInteger, endInclusive: VAllNNI) : this(VAllNNI.Value(startNNI), endInclusive)
    constructor(
        startNNI: VNonNegativeInteger,
        endInclusive: VNonNegativeInteger
    ) : this(start = VAllNNI.Value(startNNI), VAllNNI.Value(endInclusive))

    override fun contains(value: VAllNNI): Boolean = when (value) {
        is VAllNNI.UNBOUNDED -> false
        else -> start <= value && value <= endInclusive
    }

    override fun isEmpty(): Boolean {
        return endInclusive !is VAllNNI.UNBOUNDED && start < endInclusive
    }

    operator fun times(other: AllNNIRange): AllNNIRange {
        return AllNNIRange(start * other.start, endInclusive * other.endInclusive)
    }
}
