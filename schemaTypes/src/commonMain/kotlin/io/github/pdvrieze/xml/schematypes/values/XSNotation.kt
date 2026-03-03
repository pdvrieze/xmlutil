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

import io.github.pdvrieze.xml.schematypes.types.NotationType
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
@Serializable(XSNotation.Companion::class)
interface XSNotation: XSAtomic {

    override val type: NotationType<*> get() = NotationType.Instance

    /**
     * Retrieve the prefix for this QName.
     */
    public fun getPrefix(): String

    /**
     * Retrieve the local part of this QName.
     */
    public fun getLocalPart(): String

    /**
     * Retrieve the namespace URI for this QName.
     */
    public fun getNamespaceURI(): String

    override val xmlString: CharSequence
        get() {
            return when {
                getNamespaceURI().isEmpty() -> {
                    when {
                        getPrefix().isEmpty() -> getLocalPart()
                        else -> "${getPrefix()}:${getLocalPart()}"
                    }
                }

                getPrefix().isEmpty() -> "Q{${getNamespaceURI()}}${getLocalPart()}"

                else -> "${getNamespaceURI()}:${getLocalPart()}"
            }
        }

    companion object : XmlSerializer<XSNotation> {
        operator fun invoke(namespaceUri: String, localPart: String, prefix: String = ""): XSNotation =
            XSNotationImpl(namespaceUri, localPart, prefix)

        operator fun invoke(localPart: String): XSNotation = XSNotationImpl(localPart)

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("xsd.QName") {
            element("namespaceUri", String.serializer().descriptor, isOptional = true)
            element("localName", String.serializer().descriptor)
            element("prefix", String.serializer().descriptor, isOptional = true)
        }.xml(PrimitiveSerialDescriptor("xsd.QName", PrimitiveKind.STRING))

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: XSNotation) {
            val qName = (value as? QName) ?: QName(value.getNamespaceURI(), value.getLocalPart(), value.getPrefix())
            QNameSerializer.serialize(encoder, qName)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun deserialize(decoder: Decoder): XSNotation {
            val qName = QNameSerializer.deserialize(decoder)
            return XSNotationImpl(qName.namespaceURI, qName.localPart, qName.prefix)
        }

        override fun serializeXML(
            encoder: Encoder,
            output: XmlWriter,
            value: XSNotation,
            isValueChild: Boolean
        ) {
            val qName = (value as? QName) ?: QName(value.getNamespaceURI(), value.getLocalPart(), value.getPrefix())
            QNameSerializer.serializeXML(encoder, output, qName, isValueChild)
        }

        override fun deserializeXML(
            decoder: Decoder,
            input: XmlReader,
            previousValue: XSNotation?,
            isValueChild: Boolean
        ): XSNotation {
            val previousQName = previousValue?.let {
                it as? QName
                    ?: QName(it.getNamespaceURI(), it.getLocalPart(), it.getPrefix())
            }

            val qName = QNameSerializer.deserializeXML(decoder, input, previousQName, isValueChild)
            return XSNotationImpl(qName.namespaceURI, qName.localPart, qName.prefix)
        }
    }
}

class XSNotationImpl(
    namespaceUri: String,
    localPart: String,
    prefix: String = "",
) : QName(namespaceUri, localPart, prefix), XSNotation {

    constructor(localPart: String) : this("", localPart)
}
