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
