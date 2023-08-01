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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.parseQName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix
import nl.adaptivity.xmlutil.serialization.XML

@Serializable(T_QNameListA.Serializer::class)
class T_QNameListA(val values: List<T_QNameListA.Elem>): List<T_QNameListA.Elem> by values {
    sealed class Elem
    object DEFINED: Elem()
    class Name(val qName: QName): Elem()

    object Serializer: KSerializer<T_QNameListA> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("List<QName>", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: T_QNameListA) {
            val e = encoder as? XML.XmlOutput
            val str = value.joinToString(" ") {
                when (it) {
                    DEFINED -> "##defined"
                    is Name -> when (e) {
                        null -> "{${it.qName.namespaceURI}}${it.qName.prefix}:${it.qName.localPart}"
                        else -> e.ensureNamespace(it.qName)
                            .let { q -> if (q.prefix == "") q.localPart else "${q.prefix}:${q.localPart}" }
                    }
                }
            }
            encoder.encodeString(str)
        }

        override fun deserialize(decoder: Decoder): T_QNameListA {
            val str = decoder.decodeString()
            val d = decoder as? XML.XmlInput
            val items = str.splitToSequence(' ')
                .filter { it.isNotEmpty() }
                .mapTo(mutableListOf()) { elem ->
                    when (elem) {
                        "##defined" -> DEFINED
                        else -> Name(parseQName(d, elem))
                    }
                }
            return T_QNameListA(items)
        }

    }
}
