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

package nl.adaptivity.xmlutil.serialization.impl

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix
import nl.adaptivity.xmlutil.serialization.XML

@Serializer(forClass = QName::class)
internal object XmlQNameSerializer : KSerializer<QName> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("javax.xml.namespace.QName", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): QName {
        if(decoder !is  XML.XmlInput) throw SerializationException("QNameXmlSerializer only makes sense in an XML context")

        val prefixedName = decoder.decodeString()
        val cIndex = prefixedName.indexOf(':')

        val prefix:String
        val namespace:String
        val localPart: String

        when {
            cIndex < 0 -> {
                prefix = ""
                localPart = prefixedName
                namespace = decoder.input.namespaceContext.getNamespaceURI("") ?: ""
            }
            else       -> {
                prefix = prefixedName.substring(0, cIndex)
                localPart = prefixedName.substring(cIndex + 1)
                namespace = decoder.input.namespaceContext.getNamespaceURI(prefix)
                    ?: throw SerializationException("Missing namespace for prefix $prefix in QName value")
            }
        }

        return QName(namespace, localPart, prefix)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: QName) {
        if(encoder !is XML.XmlOutput) throw SerializationException("QNameXmlSerializer only makes sense in an XML context")
//        val registeredNs = encoder.target.namespaceContext.getNamespaceURI(value.prefix)
//        if (registeredNs!=value.namespaceURI) throw SerializationException("No namespace registered for prefix ${value.prefix}")

        encoder.encodeString("${value.prefix}:${value.localPart}")
    }
}
