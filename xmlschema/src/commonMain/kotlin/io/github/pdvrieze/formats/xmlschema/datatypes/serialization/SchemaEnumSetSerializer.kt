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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SchemaEnumSetSerializer<T: Enum<T>>(val elementSerializer: KSerializer<T>): KSerializer<Set<T>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Set<${elementSerializer.descriptor.serialName}>", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Set<T>) {
        if (value.size == elementSerializer.descriptor.elementsCount) {
            encoder.encodeString("#all")
        } else {
            val stringListEncoder = SimpleStringEncoder(encoder.serializersModule)
            SetSerializer(elementSerializer).serialize(stringListEncoder, value)

            encoder.encodeString(stringListEncoder.joinToString(" "))
        }
    }

    override fun deserialize(decoder: Decoder): Set<T> {
        val str = decoder.decodeString()
        val names = when (str) {
            "#all" -> {
                (0 until elementSerializer.descriptor.elementsCount).map {
                    elementSerializer.descriptor.getElementName(it)
                }
            }
            else -> str.split(' ')
        }
        return SetSerializer(elementSerializer).deserialize(SimpleStringListDecoder(names, decoder.serializersModule))
    }
}
