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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(VNotNamespaceList.Serializer::class)
class VNotNamespaceList(private val values: List<Elem>) : List<VNotNamespaceList.Elem> by values {

    constructor() : this(emptyList())

    sealed class Elem {
        companion object {
            fun fromString(string: String): Elem = when (string) {
                "##targetNamespace" -> TARGETNAMESPACE
                "##local" -> LOCAL
                else -> Uri(VAnyURI(string))
            }
        }
    }

    object TARGETNAMESPACE : Elem()
    object LOCAL : Elem()
    class Uri(val value: VAnyURI) : Elem()

    object Serializer : KSerializer<VNotNamespaceList> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("namespaceList", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: VNotNamespaceList) {
            val str = value.values.joinToString(" ") { v ->
                when (v) {
                    TARGETNAMESPACE -> "##targetNamespace"
                    LOCAL -> "##local"
                    is Uri -> v.value.value
                }
            }

            encoder.encodeString(str)
        }

        override fun deserialize(decoder: Decoder): VNotNamespaceList {
            val values = decoder.decodeString()
                .trim()
                .splitToSequence(' ')
                .filter { it.isNotEmpty() }
                .map { Elem.fromString(it) }
                .toList()

            return VNotNamespaceList(values)
        }
    }

}
