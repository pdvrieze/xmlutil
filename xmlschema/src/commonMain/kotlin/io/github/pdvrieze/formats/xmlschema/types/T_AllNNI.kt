/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
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

@Serializable(T_AllNNI.Serializer::class)
sealed class T_AllNNI: Comparable<T_AllNNI> { //TODO make interface


    abstract operator fun plus(other: T_AllNNI): T_AllNNI

    abstract operator fun times(other: T_AllNNI): T_AllNNI


    object UNBOUNDED : T_AllNNI() {
        override fun compareTo(other: T_AllNNI): Int = when(other){
            UNBOUNDED -> 0
            else -> 1
        }

        operator fun plus(other: VNonNegativeInteger): T_AllNNI = UNBOUNDED
        override operator fun plus(other: T_AllNNI): T_AllNNI = UNBOUNDED
        override operator fun times(other: T_AllNNI): T_AllNNI = UNBOUNDED

        override fun toString(): String = "unbounded"
    }

    @Serializable(Value.Serializer::class)
    class Value(val value: VNonNegativeInteger): T_AllNNI(), VNonNegativeInteger by value {
        constructor(value: ULong): this(VNonNegativeInteger(value))
        constructor(value: UInt): this(VNonNegativeInteger(value))

        override fun compareTo(other: T_AllNNI): Int {
            return when (other) {
                is UNBOUNDED -> -1
                is Value -> toULong().compareTo(other.toULong())
            }
        }

        override operator fun plus(other: VNonNegativeInteger): VNonNegativeInteger = when (other) {
            is Value -> Value(value + other.value)
            else -> Value(value + other)
        }

        override operator fun plus(other: T_AllNNI): T_AllNNI = when (other) {
            is Value -> Value(value + other.value)
            is UNBOUNDED -> UNBOUNDED
        }

        override operator fun times(other: T_AllNNI): T_AllNNI = when (other) {
            is Value -> Value(value.toULong() * other.value.toULong())
            is UNBOUNDED -> UNBOUNDED
        }

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

    companion object Serializer: KSerializer<T_AllNNI> {

        operator fun invoke(v: Int): Value = Value(v.toULong())

        operator fun invoke(v: UInt): Value = Value(v.toULong())

        operator fun invoke(v: Long): Value = Value(v.toULong())

        operator fun invoke(v: ULong): Value = Value(v)

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AllNNI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): T_AllNNI = when (val v= decoder.decodeString()) {
            "unbounded" -> UNBOUNDED
            else -> Value(VNonNegativeInteger(v))
        }

        override fun serialize(encoder: Encoder, value: T_AllNNI) {
            encoder.encodeString(value.toString())
        }
    }
}

class AllNNIRange(override val start: T_AllNNI.Value, override val endInclusive: T_AllNNI): ClosedRange<T_AllNNI> {
    constructor(startNNI: VNonNegativeInteger, endInclusive: T_AllNNI) : this(T_AllNNI.Value(startNNI), endInclusive)
    constructor(startNNI: VNonNegativeInteger, endInclusive: VNonNegativeInteger) : this(start = T_AllNNI.Value(startNNI), T_AllNNI.Value(endInclusive))

    override fun contains(value: T_AllNNI): Boolean = when (value) {
        is T_AllNNI.UNBOUNDED -> false
        is T_AllNNI.Value -> start <= value && value <= endInclusive
    }

    override fun isEmpty(): Boolean {
        return endInclusive !is T_AllNNI.UNBOUNDED && start < endInclusive
    }
}
