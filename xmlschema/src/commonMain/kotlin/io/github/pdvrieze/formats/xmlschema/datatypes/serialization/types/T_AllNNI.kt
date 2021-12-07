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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(T_AllNNI.Serializer::class)
sealed class T_AllNNI {
    object UNBOUNDED: T_AllNNI() {
        override fun toString(): String = "unbounded"
    }

    class Value(val value: VNonNegativeInteger): T_AllNNI(), VNonNegativeInteger by value {
        constructor(value: ULong): this(VNonNegativeInteger(value))
        constructor(value: UInt): this(VNonNegativeInteger(value))

        override fun toString(): String = value.toString()
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
