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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class VUnsignedInt : VUnsignedLong {
    override fun toLong(): Long = toInt().toLong()

    override fun toULong(): ULong = toUInt().toULong()

    private class Inst(val value: UInt) : VUnsignedInt() {
        override val xmlString: String get() = value.toString()

        override fun toInt(): Int = value.toInt()

        override fun toUInt(): UInt = value
    }

    class Serializer : KSerializer<VUnsignedInt> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("unsignedLong", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VUnsignedInt {
            return Inst(decoder.decodeString().toUInt())
        }

        override fun serialize(encoder: Encoder, value: VUnsignedInt) {
            encoder.encodeString(value.xmlString)
        }
    }

    companion object {
        operator fun invoke(value: UInt): VUnsignedInt = Inst(value)
    }

}
