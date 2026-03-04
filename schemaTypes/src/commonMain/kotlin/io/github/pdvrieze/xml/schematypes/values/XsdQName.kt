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

import io.github.pdvrieze.xml.schematypes.types.QNameType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdQNameImpl
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
@ExperimentalXmlUtilApi
@Serializable(XsdQName.Companion::class)
interface XsdQName: XsdAtomic {

    override val schemaType: QNameType<XsdQName>

    /**
     * Retrieve the prefix for this QName.
     */
    fun getPrefix(): String

    /**
     * Retrieve the local part of this QName.
     */
    fun getLocalPart(): String

    /**
     * Retrieve the namespace URI for this QName.
     */
    fun getNamespaceURI(): String

    fun toQName(): QName

    override val xmlString: String
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

    infix fun isEquivalent(other: QName): Boolean {
        return getLocalPart() == other.getLocalPart() &&
                getNamespaceURI() == other.getNamespaceURI()
    }

    infix fun isEquivalent(other: XsdQName): Boolean {
        return getLocalPart() == other.getLocalPart() &&
                getNamespaceURI() == other.getNamespaceURI()
    }


    companion object: XmlSerializer<XsdQName> {
        operator fun invoke(namespaceUri: String, localPart: String, prefix: String = ""): XsdQName =
            XsdQNameImpl(namespaceUri, localPart, prefix)

        operator fun invoke(localPart: String): XsdQName = XsdQNameImpl(localPart)
        operator fun invoke(qName: QName): XsdQName = XsdQNameImpl(qName.namespaceURI, qName.localPart, qName.prefix)
        operator fun invoke(notation: XsdNotation): XsdQName = XsdQNameImpl(notation.getNamespaceURI(), notation.getLocalPart(), notation.getPrefix())

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("xsd.QName") {
            element("namespaceUri", String.serializer().descriptor, isOptional = true)
            element("localName", String.serializer().descriptor)
            element("prefix", String.serializer().descriptor, isOptional = true)
        }.xml(PrimitiveSerialDescriptor("xsd.QName", PrimitiveKind.STRING))

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: XsdQName) {
            QNameSerializer.serialize(encoder, value.toQName())
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun deserialize(decoder: Decoder): XsdQName {
            val qName = QNameSerializer.deserialize(decoder)
            return XsdQNameImpl(qName.namespaceURI, qName.localPart, qName.prefix)
        }

        override fun serializeXML(
            encoder: Encoder,
            output: XmlWriter,
            value: XsdQName,
            isValueChild: Boolean
        ) {
            QNameSerializer.serializeXML(encoder, output, value.toQName(), isValueChild)
        }

        override fun deserializeXML(
            decoder: Decoder,
            input: XmlReader,
            previousValue: XsdQName?,
            isValueChild: Boolean
        ): XsdQName {
            val previousQName = previousValue?.let {
                it as? QName
                    ?: QName(it.getNamespaceURI(), it.getLocalPart(), it.getPrefix())
            }

            val qName = QNameSerializer.deserializeXML(decoder, input, previousQName, isValueChild)
            return XsdQNameImpl(qName.namespaceURI, qName.localPart, qName.prefix)
        }
    }

}

// XXX remove
val XsdQName.prefix get() = getPrefix()
val XsdQName.localPart get() = getLocalPart()
val XsdQName.namespaceURI get() = getNamespaceURI()
