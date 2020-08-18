/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.builtins.UnitSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerialModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlListDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlPolymorphicDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlValueDescriptor

internal open class XmlEncoderBase internal constructor(
    context: SerialModule,
    config: XmlConfig,
    val target: XmlWriter
                                                       ) : XmlCodecBase(context, config) {

    override val namespaceContext: NamespaceContext
        get() = target.namespaceContext

    /**
     * Encoder class for all primitives (except for initial values). It does not handle attributes. This does not
     * implement XmlOutput as
     */
    internal open inner class XmlEncoder(
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        val serializier: SerializationStrategy<*>,
        xmlDescriptor: XmlDescriptor,
        childDesc: SerialDescriptor? = serializier.descriptor
                                        ) :
        XmlCodec<XmlDescriptor>(xmlDescriptor), Encoder, XML.XmlOutput {

        override val target: XmlWriter get() = this@XmlEncoderBase.target

        override val context get() = this@XmlEncoderBase.context

        override fun encodeBoolean(value: Boolean) = encodeString(value.toString())

        override fun encodeByte(value: Byte) = encodeString(value.toString())

        override fun encodeShort(value: Short) = encodeString(value.toString())

        override fun encodeInt(value: Int) = encodeString(value.toString())

        override fun encodeLong(value: Long) = encodeString(value.toString())

        override fun encodeFloat(value: Float) = encodeString(value.toString())

        override fun encodeDouble(value: Double) = encodeString(value.toString())

        override fun encodeChar(value: Char) = encodeString(value.toString())

        override fun encodeString(value: String) {
            val defaultValue = (xmlDescriptor as XmlValueDescriptor).default
            if (value == defaultValue) return

            when (xmlDescriptor.outputKind) {
                OutputKind.Element   -> { // This may occur with list values.
                    target.smartStartTag(serialName) { target.text(value) }
                }
                OutputKind.Attribute -> {
                    target.attribute(serialName.namespaceURI, serialName.localPart, serialName.prefix, value)
                }
                OutputKind.Mixed,
                OutputKind.Text      -> target.text(value)
            }
        }

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            // TODO allow policy to determine actually used constant, including @XmlSerialName with or without prefix/ns
            encodeString(enumDescriptor.getElementName(index))
        }

        override fun encodeNotNullMark() {
            // Not null is presence, no mark needed
        }

        override fun encodeUnit() {
            when (xmlDescriptor.outputKind) {
                OutputKind.Attribute -> target.writeAttribute(serialName, serialName.localPart)
                OutputKind.Mixed,
                OutputKind.Element   -> target.smartStartTag(serialName) { /*No content*/ }
                OutputKind.Text      -> target.text("Unit")
            }

        }

        override fun encodeNull() {
            // Null is absence, no mark needed
        }

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            serializer.serialize(this, value)
        }

        override fun beginStructure(
            descriptor: SerialDescriptor,
            vararg typeSerializers: KSerializer<*>
                                   ): CompositeEncoder {

            return beginEncodeCompositeImpl(
                serializier,
                xmlDescriptor
                                           )
        }
    }

    fun beginEncodeCompositeImpl(
        serializer: SerializationStrategy<*>,
        xmlDescriptor: XmlDescriptor
                                ): CompositeEncoder {

        return when (xmlDescriptor.serialKind) {
            is PrimitiveKind    -> throw AssertionError("A primitive is not a composite")

            UnionKind.CONTEXTUAL, // TODO handle contextual in a more elegant way
            StructureKind.MAP,
            StructureKind.CLASS,
            StructureKind.OBJECT,
            UnionKind.ENUM_KIND -> TagEncoder(serializer, xmlDescriptor)

            StructureKind.LIST  -> ListEncoder(serializer, xmlDescriptor as XmlListDescriptor)

            is PolymorphicKind  -> PolymorphicEncoder(serializer, xmlDescriptor as XmlPolymorphicDescriptor)
        }.apply { writeBegin() }
    }

    internal open inner class TagEncoder<D : XmlDescriptor>(
        val serializer: SerializationStrategy<*>,
        xmlDescriptor: D,
        private var deferring: Boolean = true
                                                           ) :
        XmlTagCodec<D>(xmlDescriptor), CompositeEncoder, XML.XmlOutput {

        override val target: XmlWriter get() = this@XmlEncoderBase.target
        override val namespaceContext: NamespaceContext get() = this@XmlEncoderBase.target.namespaceContext

        private val deferredBuffer = mutableListOf<CompositeEncoder.() -> Unit>()

        open fun writeBegin() {
            target.smartStartTag(serialName)
        }

        open fun defer(index: Int, deferred: CompositeEncoder.() -> Unit) {
            if (!deferring) {
                deferred()
            } else {
                val outputKind = xmlDescriptor.getElementDescriptor(index).outputKind
                if (outputKind == OutputKind.Attribute) {
                    deferred()
                } else {
                    deferredBuffer.add(deferred)
                }
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val encoder = XmlEncoder(
                xmlDescriptor.serialDescriptor,
                index,
                serializer,
                xmlDescriptor.getElementDescriptor(index)
                                    )
            defer(index) { serializer.serialize(encoder, value) }
        }

        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
            return descriptor.getElementAnnotations(index).none { it is XmlDefault }
        }

        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            encodeStringElement(descriptor, index, "kotlin.Unit")
        }

        final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
            encodeStringElement(descriptor, index, value.toString())
        }

        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
                                                                ) {
            if (value != null) {
                encodeSerializableElement(descriptor, index, serializer, value)
            }
            // Null is the absense of values, no need to do more
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            val defaultValue = descriptor.getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
            if (value == defaultValue) return

            val kind = descriptor.outputKind(index, null)
            val requestedName = xmlDescriptor.getChildDescriptor(index, String.serializer()).tagName
            when (kind) {
                OutputKind.Element   -> defer(index) { target.smartStartTag(requestedName) { text(value) } }
                OutputKind.Attribute -> doWriteAttribute(requestedName, value)
                OutputKind.Mixed,
                OutputKind.Text      -> defer(index) { target.text(value) }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            deferring = false
            for (deferred in deferredBuffer) {
                deferred()
            }
            target.endTag(serialName)
        }

        open fun doWriteAttribute(name: QName, value: String) {

            val actualAttrName: QName = when {
                name.getNamespaceURI().isEmpty() ||
                        (serialName.getNamespaceURI() == name.getNamespaceURI() &&
                                (serialName.prefix == name.prefix))
                     -> QName(name.localPart) // Breaks in android otherwise

                else -> name
            }

            smartWriteAttribute(actualAttrName, value)
        }


    }

    /**
     * Helper function that ensures writing the namespace attribute if needed.
     */
    private fun smartWriteAttribute(name: QName, value: String) {
        val needsNsAttr = name.namespaceURI.isNotEmpty() &&
                target.getPrefix(name.namespaceURI) == null

        if (needsNsAttr) target.namespaceAttr(name.prefix, name.namespaceURI)

        target.writeAttribute(name, value)
    }

    internal inner class PolymorphicEncoder(
        serializer: SerializationStrategy<*>,
        xmlDescriptor: XmlPolymorphicDescriptor
                                           ) :
        TagEncoder<XmlPolymorphicDescriptor>(serializer, xmlDescriptor, deferring = false),
        XML.XmlOutput {

        val isMixed get() = xmlDescriptor.outputKind == OutputKind.Mixed

        override fun defer(index: Int, deferred: CompositeEncoder.() -> Unit) {
            deferred()
        }

        override fun writeBegin() {
            // Don't write a tag if we are transparent
            if (!xmlDescriptor.transparent) super.writeBegin()
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            when {
                index == 0                -> {
                    if (!xmlDescriptor.transparent) {
                        val childDesc = xmlDescriptor.getElementDescriptor(0)
                        assert(xmlDescriptor.parentSerialName == parentDesc.serialName) {
                            "parentSerialName ('${xmlDescriptor.parentSerialName}') should match the one in the encoder: '${parentDesc.serialName}'"
                        }
                        when (childDesc.outputKind) {
                            OutputKind.Attribute -> doWriteAttribute(
                                childDesc.tagName,
                                value.tryShortenTypeName(xmlDescriptor.parentSerialName)
                                                                    )
                            OutputKind.Mixed,
                            OutputKind.Element   -> defer(index) { target.smartStartTag(childDesc.tagName) { text(value) } }
                            OutputKind.Text      -> throw XmlSerialException("the type for a polymorphic child cannot be a text")
                        }
                    } // else if (index == 0) { } // do nothing
                }
                xmlDescriptor.transparent -> {
                    when {
                        isMixed -> target.text(value)
                        else    -> target.smartStartTag(serialName) { text(value) }
                    }
                }
                else                      -> super.encodeStringElement(descriptor, index, value)
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val childXmlDescriptor = xmlDescriptor.getPolymorphicDescriptor(serializer.descriptor.serialName)
            val encoder = XmlEncoder(descriptor, index, serializer, childXmlDescriptor)
            serializer.serialize(encoder, value)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Don't write anything if we're transparent
            if (!xmlDescriptor.transparent) {
                super.endStructure(descriptor)
            }
        }

    }

    /**
     * Writer that does not actually write an outer tag unless a [XmlChildrenName] is specified. In XML children don't need to
     * be wrapped inside a list (unless it is the root). If [XmlChildrenName] is not specified it will determine tag names
     * as if the list was not present and there was a single value.
     */
    internal inner class ListEncoder(
        serializer: SerializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor
                                    ) :
        TagEncoder<XmlListDescriptor>(
            serializer,
            xmlDescriptor,
            deferring = false
                                     ), XML.XmlOutput {

        val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

        override fun defer(index: Int, deferred: CompositeEncoder.() -> Unit) {
            deferred()
        }

        override fun writeBegin() {
            if (!xmlDescriptor.isAnonymous) { // Do the default thing if the children name has been specified
                val childrenName = xmlDescriptor.getElementDescriptor(0).tagName
                super.writeBegin()
                val childName = childrenName
                val tagName = serialName

                //declare namespaces on the parent rather than the children of the list.
                if (tagName.prefix != childName.prefix && target.getNamespaceUri(
                        childName.prefix
                                                                                ) != childName.namespaceURI
                ) {
                    target.namespaceAttr(childName.prefix, childName.namespaceURI)
                }

            }

        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val childDescriptor = xmlDescriptor.getElementDescriptor(0)
            if (xmlDescriptor.isAnonymous) { // Use the outer decriptor and element index
                serializer.serialize(XmlEncoder(parentDesc, elementIndex, serializer, childDescriptor), value)
            } else {
                serializer.serialize(XmlEncoder(descriptor, 0, serializer, childDescriptor), value)
            }
        }

        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            XmlEncoder(
                descriptor,
                index,
                UnitSerializer(),
                xmlDescriptor.getChildDescriptor(index, UnitSerializer())
                      ).encodeUnit()
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            if (index > 0) {
                when (xmlDescriptor.outputKind) {
                    OutputKind.Mixed -> target.text(value) // Mixed will be a list of strings and other stuff
                    else             -> {
                        XmlEncoder(
                            descriptor,
                            index,
                            String.serializer(),
                            xmlDescriptor.getChildDescriptor(index, String.serializer())
                                  ).encodeString(
                            value
                                                )
                    }
                }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!xmlDescriptor.isAnonymous) {
                super.endStructure(descriptor)
            }
        }
    }
}
