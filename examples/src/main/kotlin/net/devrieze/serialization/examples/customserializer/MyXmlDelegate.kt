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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * Class based upon query in issue #62. It shows how a custom serializer
 * could be used to add a pseudo attribute to the xml. This implementation uses a delegate object to serialize.
 */
@XmlSerialName("MyXml", "urn:OECD:MyXmlFile", "")
@Serializable(MyXmlDelegate.Companion::class)
data class MyXmlDelegate(val attribute: String) {

    @Serializable
    @XmlSerialName("MyXml", "urn:OECD:MyXmlFile", "")
    private open class BaseSerialDelegate(val attribute: String) {
        constructor(origin: MyXmlDelegate) : this(origin.attribute)

        fun toMyXml(): MyXmlDelegate = MyXmlDelegate(attribute)
    }

    @Serializable
    @XmlSerialName("MyXml", "urn:OECD:MyXmlFile", "")
    private class XmlSerialDelegate : BaseSerialDelegate {
        @XmlBefore("attribute")
        @XmlSerialName("schemalocation", "http://www.w3.org/2001/XMLSchema-instance", "xsi")
        val schemalocation: String

        constructor(attribute: String, schemalocation: String) :
                super(attribute) {
            this.schemalocation = schemalocation
        }

        constructor(origin: MyXmlDelegate) : this(origin.attribute, "urn:OECD:MyXmlFile.xsd")
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    @Serializer(MyXmlDelegate::class)
    companion object : KSerializer<MyXmlDelegate> {
        override val descriptor: SerialDescriptor =
            XmlSerialDelegate.serializer().descriptor

        private val NS_XSI = XmlEvent.NamespaceImpl(
            "xsi",
            "http://www.w3.org/2001/XMLSchema-instance"
                                                   )


        override fun serialize(encoder: Encoder, value: MyXmlDelegate) {
            if (encoder is XML.XmlOutput) {
                XmlSerialDelegate.serializer().serialize(encoder, XmlSerialDelegate(value))
            } else {
                BaseSerialDelegate.serializer().serialize(encoder, BaseSerialDelegate(value))
            }

/*
            encoder.encodeStructure(descriptor) {
                (encoder as? XML.XmlOutput)?.target?.run {
                    namespaceAttr(NS_XSI)
                    writeAttribute(
                        QName( NS_XSI.namespaceURI, "schemalocation", "xsi" ),
                        "urn:OECD:MyXmlFile.xsd"
                                  )
                }
                encodeStringElement(descriptor, 0, value.attribute)
            }
*/
        }

        override fun deserialize(decoder: Decoder): MyXmlDelegate {
            return when (decoder) {
                is XML.XmlInput -> XmlSerialDelegate.serializer().deserialize(decoder).toMyXml()
                else            -> BaseSerialDelegate.serializer().deserialize(decoder).toMyXml()
            }

/*
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
                MyXml(attribute)
            }
*/
        }
    }
}
