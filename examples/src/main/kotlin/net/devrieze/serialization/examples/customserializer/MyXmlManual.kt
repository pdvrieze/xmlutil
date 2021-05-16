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

package net.devrieze.serialization.examples.customserializer

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.writeAttribute
import javax.xml.namespace.QName

/**
 * Class based upon query in issue #62. It shows how a custom serializer
 * could be used to add a pseudo attribute to the xml. This implementation uses a manually written serializer.
 */
@XmlSerialName("MyXml", "urn:OECD:MyXmlFile", "")
@Serializable(MyXmlManual.Companion::class)
data class MyXmlManual(val attribute: String) {

    @Serializable
    @XmlSerialName("MyXml", "urn:OECD:MyXmlFile", "")
    private open class BaseSerialDelegate(val attribute: String) {
        constructor(origin: MyXmlManual): this(origin.attribute)

        fun toMyXml(): MyXmlManual = MyXmlManual(attribute)
    }

    @Serializable
    @XmlSerialName("MyXml", "urn:OECD:MyXmlFile", "")
    private class DescriptorDelegate(
        val attribute: String,
        @XmlSerialName("schemalocation", "http://www.w3.org/2001/XMLSchema-instance", "xsi")
        @Required val schemalocation: String = "urn:OECD:MyXmlFile.xsd")

    @Serializer(MyXmlManual::class)
    companion object : KSerializer<MyXmlManual> {
        override val descriptor: SerialDescriptor =
            DescriptorDelegate.serializer().descriptor

        private val NS_XSI = XmlEvent.NamespaceImpl(
            "xsi",
            "http://www.w3.org/2001/XMLSchema-instance"
                                                   )


        override fun serialize(encoder: Encoder, value: MyXmlManual) {
            encoder.encodeStructure(descriptor) {
                (encoder as? XML.XmlOutput)?.target?.run {
                    namespaceAttr(NS_XSI)
                    writeAttribute(
                        QName(
                            NS_XSI.namespaceURI,
                            "schemalocation",
                            "xsi"
                             ), "urn:OECD:MyXmlFile.xsd"
                                  )
                }
                encodeStringElement(descriptor, 0, value.attribute)
            }
        }

        override fun deserialize(decoder: Decoder): MyXmlManual {
            return decoder.decodeStructure(descriptor) {
                lateinit var attribute: String
                do {
                    val idx = decodeElementIndex(descriptor)
                    when (idx) {
                        1, // just ignore the schema attribute
                        CompositeDecoder.DECODE_DONE -> continue
                        0                            -> attribute =
                            decodeStringElement(descriptor, 0)
                        else                         -> throw SerializationException(
                            "Not found"
                                                                                    )
                    }
                } while (idx != CompositeDecoder.DECODE_DONE)
                MyXmlManual(attribute)
            }
        }
    }
}
