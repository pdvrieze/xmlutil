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

import io.github.pdvrieze.formats.xmlschema.types.T_DerivationControl
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationControl.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
class SchemaEnumSetSerializer<T : Enum<T>>(val elementSerializer: KSerializer<T>) : KSerializer<Set<T>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Set<${elementSerializer.descriptor.serialName}>", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Set<T>) {
        if (value.size > 0) {
            if (value.size == elementSerializer.descriptor.elementsCount) {
                encoder.encodeString("#all")
            } else {
                val stringListEncoder = SimpleStringEncoder(encoder.serializersModule)
                SetSerializer(elementSerializer).serialize(stringListEncoder, value)

                encoder.encodeString(stringListEncoder.joinToString(" "))
            }
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
        }.filter { it.isNotEmpty() }
        return SetSerializer(elementSerializer).deserialize(SimpleStringListDecoder(names, decoder.serializersModule))
    }
}

class AllDerivationSerializer : KSerializer<Set<T_DerivationControl>> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Set<T_DeriviationControl>", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Set<T_DerivationControl> {
        return when (val s = decoder.decodeString().trim()) {
            "#all" -> setOf(RESTRICTION, EXTENSION, LIST, UNION, SUBSTITUTION)
            "" -> emptySet()
            else -> s.split(' ').asSequence().mapTo(HashSet()) {
                when (it) {
                    RESTRICTION.name -> RESTRICTION
                    EXTENSION.name -> EXTENSION
                    LIST.name -> LIST
                    UNION.name -> UNION
                    SUBSTITUTION.name -> SUBSTITUTION
                    else -> error("Unsupported substitution name: ${s}")
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Set<T_DerivationControl>) {
        val s = when (value.size) {
            4 -> "#all"
            else -> value.joinToString(" ") { it.name }
        }
        encoder.encodeString(s)
    }
}

class ComplexDerivationSerializer : KSerializer<Set<ComplexBase>> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Set<T_TypeDerviationControl.ComplexBase>", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Set<ComplexBase> {
        return when (val s = decoder.decodeString().trim()) {
            "#all" -> setOf(RESTRICTION, EXTENSION)
            "" -> emptySet()
            else -> s.split(' ').asSequence().mapTo(HashSet()) {
                when (it) {
                    RESTRICTION.name -> RESTRICTION
                    EXTENSION.name -> EXTENSION
                    else -> error("Unsupported substitution name: ${s}")
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Set<ComplexBase>) {
        val s = when (value.size) {
            2 -> "#all"
            else -> value.joinToString(" ") { it.name }
        }
        encoder.encodeString(s)
    }
}
