/*
 * Copyright (c) 2018.
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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.09.24 at 08:12:58 PM CEST
//


package nl.adaptivity.xml.serialization.regressions.soap

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import nl.adaptivity.serialutil.decodeElements
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment


/**
 *
 *
 * Java class for Body complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="Body">
 * <complexContent>
 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * <sequence>
 * <any processContents='lax' maxOccurs="unbounded" minOccurs="0"/>
 * </sequence>
 * <anyAttribute processContents='lax' namespace='##other'/>
 * </restriction>
 * </complexContent>
 * </complexType>
 * ```
 *
 */
class Body<out T: Any>(
    @XmlValue(true)
    val child: T,
    val encodingStyle: String? = "http://www.w3.org/2003/05/soap-encoding",
    val otherAttributes: Map<QName, String> = emptyMap(),
) {
    fun copy(
        encodingStyle: String? = this.encodingStyle,
        otherAttributes: Map<QName, String> = this.otherAttributes,
    ): Body<T> = Body(child, encodingStyle, otherAttributes)

    fun <U: Any> copy(
        child: U,
        encodingStyle: String? = this.encodingStyle,
        otherAttributes: Map<QName, String> = this.otherAttributes,
    ): Body<U> = Body(child, encodingStyle, otherAttributes)

    class Serializer<T: Any>(private val contentSerializer: KSerializer<T>): XmlSerializer<Body<T>> {

        @OptIn(ExperimentalSerializationApi::class, XmlUtilInternal::class)
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.w3c.dom.Body") {
            annotations = SoapSerialObjects.bodyAnnotations
            element<String>("encodingStyle", SoapSerialObjects.encodingStyleAnnotations, true)
            element("otherAttributes", SoapSerialObjects.attrsSerializer.descriptor, isOptional = true)
            element("child", contentSerializer.descriptor, SoapSerialObjects.valueAnnotations)
        }.xml(
            buildClassSerialDescriptor("org.w3c.dom.Body") {
                annotations = SoapSerialObjects.bodyAnnotations
                element<String>("encodingStyle", SoapSerialObjects.encodingStyleAnnotations, true)
                element("otherAttributes", SoapSerialObjects.attrsSerializer.descriptor, listOf(XmlElement(false)), isOptional = true)
                element("child", contentSerializer.descriptor, SoapSerialObjects.valueAnnotations)
            }
        )

        override fun deserialize(decoder: Decoder): Body<T> {
            var encodingStyle: String? = null
            var otherAttributes: Map<QName, String> = emptyMap()
            lateinit var child: T
            decoder.decodeStructure(descriptor) {
                decodeElements(this) { idx ->
                    when (idx) {
                        0 -> encodingStyle = decodeStringElement(descriptor, idx)
                        1 -> otherAttributes = decodeSerializableElement(
                            descriptor, idx,
                            SoapSerialObjects.attrsSerializer, otherAttributes
                        )

                        2 -> child = decodeSerializableElement(descriptor, idx, contentSerializer)
                    }
                }
            }
            return Body(child)
        }

        override fun deserializeXML(
            decoder: Decoder,
            input: XmlReader,
            previousValue: Body<T>?,
            isValueChild: Boolean
        ): Body<T> {
            val descriptor = descriptor.getElementDescriptor(-1)
            var encodingStyle: String? = null
            var otherAttributes: Map<QName, String> = emptyMap()
            lateinit var child: T
            decoder.decodeStructure(descriptor) {
                otherAttributes = input.attributes.filter {
                    when {
                        it.prefix == XMLConstants.XMLNS_ATTRIBUTE ||
                                (it.prefix == "" && it.localName == XMLConstants.XMLNS_ATTRIBUTE) -> false

                        it.namespaceUri != Envelope.NAMESPACE -> true
                        it.localName == "encodingStyle" -> {
                            encodingStyle = it.value; false
                        }

                        else -> true
                    }
                }.associate { QName(it.namespaceUri, it.localName, it.prefix) to it.value }

                child = decodeSerializableElement(descriptor, 2, contentSerializer, null)
                if (input.nextTag() != EventType.END_ELEMENT) throw SerializationException("Extra content in body")
            }
            return Body(child)
        }

        override fun serialize(encoder: Encoder, value: Body<T>) {
            encoder.encodeStructure(descriptor) {
                value.encodingStyle?.also { style ->
                    encodeStringElement(descriptor, 0, style)
                }
                if (value.otherAttributes.isNotEmpty() || shouldEncodeElementDefault(descriptor, 1)) {
                    encodeSerializableElement(
                        descriptor,
                        1,
                        SoapSerialObjects.attrsSerializer,
                        value.otherAttributes
                    )
                }
                encodeSerializableElement(descriptor, 2, contentSerializer, value.child)
            }
        }

        override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Body<T>, isValueChild: Boolean) {
            output.smartStartTag(ELEMENTNAME) {
                value.encodingStyle?.also { style ->
                    output.attribute(Envelope.NAMESPACE, "encodingStyle", Envelope.PREFIX, style.toString())
                }
                for ((aName, aValue) in value.otherAttributes) {
                    output.writeAttribute(aName, aValue)
                }
                val child = value.child
                when (child) {
                    is CompactFragment -> {
                        for (ns in child.namespaces) {
                            if (output.getNamespaceUri(ns.prefix) != ns.namespaceURI) {
                                output.namespaceAttr(ns)
                            }
                        }
                        child.serialize(output)
                    }

                    else -> (encoder as XML.XmlOutput).delegateFormat().encodeToWriter(output, contentSerializer, child)
                }

            }
        }
    }

    companion object {

        const val ELEMENTLOCALNAME = "Body"
        val ELEMENTNAME = QName(Envelope.NAMESPACE, ELEMENTLOCALNAME, Envelope.PREFIX)

    }

}
