/*
 * Copyright (c) 2023-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.xml.schematypes.values

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.*

// This implementation inherits QName to keep it easy.
@Serializable(XSQName.Companion::class)
class XSQName(
    namespaceUri: String,
    localPart: String,
    prefix: String = "",
) : QName(namespaceUri, localPart, prefix), XSAtomic {

    constructor(localPart: String) : this("", localPart)

    override val xmlString: CharSequence
        get() {
            return when {
                namespaceURI.isEmpty() -> {
                    when {
                        prefix.isEmpty() -> this@XSQName.localPart
                        else -> "$prefix:${this@XSQName.localPart}"
                    }
                }

                prefix.isEmpty() -> "Q{$namespaceURI}${this@XSQName.localPart}"
                else -> {
                    "$namespaceURI:${this@XSQName.localPart}"
                }
            }
        }

    companion object: XmlSerializer<XSQName> {

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("xsd.QName") {
            element("namespaceUri", String.serializer().descriptor, isOptional = true)
            element("localName", String.serializer().descriptor)
            element("prefix", String.serializer().descriptor, isOptional = true)
        }.xml(PrimitiveSerialDescriptor("xsd.QName", PrimitiveKind.STRING))

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: XSQName) {
            QNameSerializer.serialize(encoder, value)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun deserialize(decoder: Decoder): XSQName {
            val qName = QNameSerializer.deserialize(decoder)
            return XSQName(qName.namespaceURI, qName.localPart, qName.prefix)
        }

        override fun serializeXML(
            encoder: Encoder,
            output: XmlWriter,
            value: XSQName,
            isValueChild: Boolean
        ) {
            QNameSerializer.serializeXML(encoder, output, value, isValueChild)
        }

        override fun deserializeXML(
            decoder: Decoder,
            input: XmlReader,
            previousValue: XSQName?,
            isValueChild: Boolean
        ): XSQName {
            val qName = QNameSerializer.deserializeXML(decoder, input, previousValue, isValueChild)
            return XSQName(qName.namespaceURI, qName.localPart, qName.prefix)
        }
    }
}
