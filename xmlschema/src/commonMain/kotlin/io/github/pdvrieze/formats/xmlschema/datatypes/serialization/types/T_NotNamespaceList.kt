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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyURI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(T_NotNamespaceList.Serializer::class)
class T_NotNamespaceList(private val values: List<T_NotNamespaceList.Elem>) : List<T_NotNamespaceList.Elem> by values {

    sealed class Elem {
        companion object {
            fun fromString(string: String) = when (string) {
                "##targetNamespace" -> TARGETNAMESPACE
                "##local" -> LOCAL
                else -> Uri(AnyURI(string))
            }
        }
    }

    object TARGETNAMESPACE : Elem()
    object LOCAL : Elem()
    class Uri(val value: AnyURI) : Elem()

    object Serializer : KSerializer<T_NotNamespaceList> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("namespaceList", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: T_NotNamespaceList) {
            val str = value.values.joinToString(" ") { v ->
                when (v) {
                    TARGETNAMESPACE -> "##targetNamespace"
                    LOCAL -> "##local"
                    is Uri -> v.value.value
                }
            }

            encoder.encodeString(str)
        }

        override fun deserialize(decoder: Decoder): T_NotNamespaceList {
            val values = decoder.decodeString()
                .trim()
                .splitToSequence(' ')
                .filter { it.isNotEmpty() }
                .map { Elem.fromString(it) }
                .toList()

            return T_NotNamespaceList(values)
        }
    }

}
