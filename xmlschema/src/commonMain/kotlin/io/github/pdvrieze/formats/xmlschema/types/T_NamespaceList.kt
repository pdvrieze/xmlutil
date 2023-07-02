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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(T_NamespaceList.Serializer::class)
sealed class T_NamespaceList {
    object ANY : T_NamespaceList()
    object OTHER : T_NamespaceList()
    class Values(val values: List<Elem>) : T_NamespaceList(), List<Elem> by values {
/*
        constructor(values: List<String>) : this(values.map { Elem.fromString(it) })
        constructor(values: List<AnyURI>) : this(values.map { Uri(it) })
*/
    }

    sealed class Elem {
        companion object {
            fun fromString(string: String) = when (string) {
                "##targetNamespace" -> TARGETNAMESPACE
                "##local" -> LOCAL
                else -> Uri(VAnyURI(string))
            }
        }
    }

    object TARGETNAMESPACE : Elem()
    object LOCAL : Elem()
    class Uri(val value: VAnyURI) : Elem()

    object Serializer : KSerializer<T_NamespaceList> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("namespaceList", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: T_NamespaceList) {
            val str = when (value) {
                ANY -> "##any"
                OTHER -> "##other"
                is Values -> value.values.joinToString(" ") { v ->
                    when (v) {
                        TARGETNAMESPACE -> "##targetNamespace"
                        LOCAL -> "##local"
                        is Uri -> v.value.value
                    }
                }
            }
            encoder.encodeString(str)
        }

        override fun deserialize(decoder: Decoder): T_NamespaceList {
            return when (val str = decoder.decodeString().trim()) {
                "##any" -> ANY
                "##other" -> OTHER
                else -> {
                    Values(str.splitToSequence(' ')
                               .filter { it.isNotEmpty() }
                               .map { Elem.fromString(it) }
                               .toList())
                }
            }
        }
    }

}