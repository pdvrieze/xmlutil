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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.structure.*

@ExperimentalSerializationApi
internal open class XmlEncoderBase internal constructor(
    context: SerializersModule,
    config: XmlConfig,
    val target: XmlWriter
                                                       ) :
    XmlCodecBase(context, config) {

    override val namespaceContext: NamespaceContext
        get() = target.namespaceContext

    /**
     * Encoder class for all primitives (except for initial values). It does not handle attributes. This does not
     * implement XmlOutput as
     */
    internal open inner class XmlEncoder(xmlDescriptor: XmlDescriptor) :
        XmlCodec<XmlDescriptor>(xmlDescriptor), Encoder, XML.XmlOutput {

        override val target: XmlWriter get() = this@XmlEncoderBase.target

        override val serializersModule get() = this@XmlEncoderBase.serializersModule
        override val config: XmlConfig get() = this@XmlEncoderBase.config

        override fun encodeBoolean(value: Boolean) =
            encodeString(value.toString())

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun encodeByte(value: Byte) =
            when (xmlDescriptor.isUnsigned) {
                true -> encodeString(value.toUByte().toString())
                else -> encodeString(value.toString())
            }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun encodeShort(value: Short) =
            when (xmlDescriptor.isUnsigned) {
                true -> encodeString(value.toUShort().toString())
                else -> encodeString(value.toString())
            }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun encodeInt(value: Int) =
            when (xmlDescriptor.isUnsigned) {
                true -> encodeString(value.toUInt().toString())
                else -> encodeString(value.toString())
            }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun encodeLong(value: Long) =
            when (xmlDescriptor.isUnsigned) {
                true -> encodeString(value.toULong().toString())
                else -> encodeString(value.toString())
            }

        override fun encodeFloat(value: Float) =
            encodeString(value.toString())

        override fun encodeDouble(value: Double) =
            encodeString(value.toString())

        override fun encodeChar(value: Char) =
            encodeString(value.toString())

        override fun encodeString(value: String) {
            // string doesn't need parsing so take a shortcut
            val defaultValue =
                (xmlDescriptor as XmlValueDescriptor).default

            if (value == defaultValue) return

            when (xmlDescriptor.outputKind) {
                OutputKind.Inline, // shouldn't occur, but treat as element
                OutputKind.Element   -> { // This may occur with list values.
                    target.smartStartTag(serialName) { target.text(value) }
                }
                OutputKind.Attribute -> {
                    target.attribute(
                        serialName.namespaceURI,
                        serialName.localPart,
                        serialName.prefix,
                        value
                                    )
                }
                OutputKind.Mixed,
                OutputKind.Text      -> target.text(value)
            }
        }

        override fun encodeEnum(
            enumDescriptor: SerialDescriptor,
            index: Int
                               ) {
            // TODO allow policy to determine actually used constant, including @XmlSerialName with or without prefix/ns
            encodeString(enumDescriptor.getElementName(index))
        }

        override fun encodeNotNullMark() {
            // Not null is presence, no mark needed
        }

        override fun encodeNull() {
            // Null is absence, no mark needed
        }

        override fun <T> encodeSerializableValue(
            serializer: SerializationStrategy<T>,
            value: T
                                                ) {
            serializer.serialize(this, value)
        }

        @ExperimentalSerializationApi
        override fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder {
            return XmlEncoder(xmlDescriptor.getElementDescriptor(0))
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {

            return beginEncodeCompositeImpl(xmlDescriptor)
        }
    }

    fun beginEncodeCompositeImpl(xmlDescriptor: XmlDescriptor): CompositeEncoder {

        return when (xmlDescriptor.serialKind) {
            is PrimitiveKind   -> throw AssertionError("A primitive is not a composite")

            SerialKind.CONTEXTUAL, // TODO handle contextual in a more elegant way
            StructureKind.MAP,
            StructureKind.CLASS,
            StructureKind.OBJECT,
            SerialKind.ENUM    -> TagEncoder(xmlDescriptor)

            StructureKind.LIST -> ListEncoder(xmlDescriptor as XmlListDescriptor)

            is PolymorphicKind -> PolymorphicEncoder(xmlDescriptor as XmlPolymorphicDescriptor)
        }.apply { writeBegin() }
    }

    private inner class InlineEncoder<D : XmlDescriptor>(
        private val parent: TagEncoder<D>,
        private val childIndex: Int
                                                        ) :
        XmlEncoder(parent.xmlDescriptor.getElementDescriptor(childIndex)) {
        override fun encodeString(value: String) {
            val d = xmlDescriptor.getElementDescriptor(0)
            parent.encodeStringElement(d, childIndex, value)
        }

        override fun <T> encodeSerializableValue(
            serializer: SerializationStrategy<T>,
            value: T
                                                ) {
            val d = xmlDescriptor.getElementDescriptor(0)
            parent.encodeSerializableElement(
                d,
                childIndex,
                serializer,
                value
                                            )
        }

        @ExperimentalSerializationApi
        override fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder {
            return this
        }
    }

    internal open inner class TagEncoder<D : XmlDescriptor>(
        xmlDescriptor: D,
        private var deferring: Boolean = true
                                                           ) :
        XmlTagCodec<D>(xmlDescriptor), CompositeEncoder, XML.XmlOutput {

        override val target: XmlWriter get() = this@XmlEncoderBase.target
        override val namespaceContext: NamespaceContext get() = this@XmlEncoderBase.target.namespaceContext

        private val deferredBuffer =
            mutableListOf<Pair<Int, CompositeEncoder.() -> Unit>>()

        private val reorderInfo =
            (xmlDescriptor as? XmlCompositeDescriptor)?.childReorderMap

        open fun writeBegin() {
            target.smartStartTag(serialName)
        }

        open fun defer(index: Int, deferred: CompositeEncoder.() -> Unit) {
            if (reorderInfo != null) {
                deferredBuffer.add(reorderInfo[index] to deferred)
            } else if (!deferring) {
                deferred()
            } else {
                val outputKind =
                    xmlDescriptor.getElementDescriptor(index).outputKind
                if (outputKind == OutputKind.Attribute) {
                    deferred()
                } else {
                    deferredBuffer.add(index to deferred)
                }
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            encodeSerializableElement(
                xmlDescriptor.getElementDescriptor(
                    index
                                                  ),
                index,
                serializer,
                value
                                     )

        }

        internal fun <T> encodeSerializableElement(
            descriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val encoder = when {
                descriptor.doInline -> InlineEncoder(this, index)
                else                -> XmlEncoder(descriptor)
            }
            defer(index) { serializer.serialize(encoder, value) }

        }

        @ExperimentalSerializationApi
        override fun encodeInlineElement(
            descriptor: SerialDescriptor,
            index: Int
                                        ): Encoder {
            return InlineEncoder(this, index)
        }

        override fun shouldEncodeElementDefault(
            descriptor: SerialDescriptor,
            index: Int
                                               ): Boolean {
            val elementDescriptor =
                xmlDescriptor.getElementDescriptor(index)

            return config.policy.shouldEncodeElementDefault(
                elementDescriptor
                                                           )
        }

        final override fun encodeBooleanElement(
            descriptor: SerialDescriptor,
            index: Int,
            value: Boolean
                                               ) {
            encodeStringElement(descriptor, index, value.toString())
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        final override fun encodeByteElement(
            descriptor: SerialDescriptor,
            index: Int,
            value: Byte
                                            ) {
            when (xmlDescriptor.isUnsigned) {
                true -> encodeStringElement(
                    descriptor,
                    index,
                    value.toUByte().toString()
                                           )
                else -> encodeStringElement(
                    descriptor,
                    index,
                    value.toString()
                                           )
            }
        }


        @OptIn(ExperimentalUnsignedTypes::class)
        final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            when (xmlDescriptor.isUnsigned) {
                true -> encodeStringElement(descriptor, index, value.toUShort().toString())
                else -> encodeStringElement(descriptor, index, value.toString()
                                           )
            }
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            when (xmlDescriptor.isUnsigned) {
                true -> encodeStringElement(descriptor, index, value.toUInt().toString())
                else -> encodeStringElement(descriptor, index, value.toString())
            }
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
            when (xmlDescriptor.isUnsigned) {
                true -> encodeStringElement(descriptor, index, value.toULong().toString())
                else -> encodeStringElement(descriptor, index, value.toString())
            }
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
            } else if (serializer.descriptor.isNullable) {
                val xmlDescriptor = xmlDescriptor.getElementDescriptor(index)
                val encoder = when {
                    xmlDescriptor.doInline -> InlineEncoder(this, index)
                    else                   -> XmlEncoder(xmlDescriptor)
                }

                // This should be safe as we are handling the case when a serializer explicitly handles nulls
                // In such case cast it to accept a null parameter and serialize through the serializer, not
                // indirectly.
                @Suppress("UNCHECKED_CAST")
                defer(index) {
                    (serializer as SerializationStrategy<T?>).serialize(encoder, null)
                }
            }

            // Null is the absense of values, no need to do more
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            val elementDescriptor = xmlDescriptor.getElementDescriptor(index)

            encodeStringElement(elementDescriptor, index, value)
        }

        internal fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            val defaultValue = (elementDescriptor as? XmlValueDescriptor)?.default

            if (value == defaultValue) return


            when (elementDescriptor.outputKind) {
                OutputKind.Inline, // Treat inline as if it was element if it occurs (shouldn't happen)
                OutputKind.Element   -> defer(index) {
                    target.smartStartTag(elementDescriptor.tagName) {
                        text(value)
                    }
                }
                OutputKind.Attribute -> doWriteAttribute(index, elementDescriptor.tagName, value)
                OutputKind.Mixed,
                OutputKind.Text      -> defer(index) { target.text(value) }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            deferring = false
            for ((_, deferred) in deferredBuffer.sortedBy { it.first }) {
                deferred()
            }
            target.endTag(serialName)
        }

        open fun doWriteAttribute(index: Int, name: QName, value: String) {

            val actualAttrName: QName = when {
                name.getNamespaceURI().isEmpty() ||
                        (serialName.getNamespaceURI() == name.getNamespaceURI() &&
                                (serialName.prefix == name.prefix))
                     -> QName(name.localPart) // Breaks in android otherwise

                else -> name
            }

            if (reorderInfo != null) {
                val deferred: CompositeEncoder.() -> Unit =
                    { smartWriteAttribute(actualAttrName, value) }
                deferredBuffer.add(reorderInfo[index] to deferred)
            } else {
                smartWriteAttribute(actualAttrName, value)
            }
        }


    }

    /**
     * Helper function that ensures writing the namespace attribute if needed.
     */
    private fun smartWriteAttribute(name: QName, value: String) {
        val needsNsAttr = name.namespaceURI.isNotEmpty() &&
                target.getPrefix(name.namespaceURI) == null

        if (needsNsAttr) target.namespaceAttr(
            name.prefix,
            name.namespaceURI
                                             )

        target.writeAttribute(name, value)
    }

    internal inner class PolymorphicEncoder(xmlDescriptor: XmlPolymorphicDescriptor) :
        TagEncoder<XmlPolymorphicDescriptor>(
            xmlDescriptor,
            deferring = false
                                            ), XML.XmlOutput {

        override fun defer(
            index: Int,
            deferred: CompositeEncoder.() -> Unit
                          ) {
            deferred()
        }

        override fun writeBegin() {
            // Don't write a tag if we are transparent
            if (!xmlDescriptor.isTransparent) super.writeBegin()
        }

        override fun encodeStringElement(
            descriptor: SerialDescriptor,
            index: Int,
            value: String
                                        ) {
            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed
            when {
                index == 0                  -> {
                    if (!xmlDescriptor.isTransparent) {
                        val childDesc =
                            xmlDescriptor.getElementDescriptor(0)

                        when (childDesc.outputKind) {
                            OutputKind.Attribute -> doWriteAttribute(
                                0,
                                childDesc.tagName,
                                value.tryShortenTypeName(xmlDescriptor.parentSerialName)
                                                                    )

                            OutputKind.Mixed,
                            OutputKind.Inline,
                            OutputKind.Element   -> target.smartStartTag(
                                childDesc.tagName
                                                                        ) {
                                text(
                                    value
                                    )
                            }

                            OutputKind.Text      ->
                                throw XmlSerialException("the type for a polymorphic child cannot be a text")
                        }
                    } // else if (index == 0) { } // do nothing
                }
                xmlDescriptor.isTransparent -> {
                    when {
                        isMixed -> target.text(value)
                        else    -> target.smartStartTag(serialName) {
                            text(
                                value
                                )
                        }
                    }
                }
                else                        -> super.encodeStringElement(
                    descriptor,
                    index,
                    value
                                                                        )
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val childXmlDescriptor =
                xmlDescriptor.getPolymorphicDescriptor(serializer.descriptor.serialName)
            val encoder = XmlEncoder(childXmlDescriptor)
            serializer.serialize(encoder, value)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Don't write anything if we're transparent
            if (!xmlDescriptor.isTransparent) {
                super.endStructure(descriptor)
            }
        }

    }

    /**
     * Writer that does not actually write an outer tag unless a [XmlChildrenName] is specified. In XML children don't need to
     * be wrapped inside a list (unless it is the root). If [XmlChildrenName] is not specified it will determine tag names
     * as if the list was not present and there was a single value.
     */
    internal inner class ListEncoder(xmlDescriptor: XmlListDescriptor) :
        TagEncoder<XmlListDescriptor>(xmlDescriptor, deferring = false),
        XML.XmlOutput {

        override fun defer(
            index: Int,
            deferred: CompositeEncoder.() -> Unit
                          ) {
            deferred()
        }

        override fun writeBegin() {
            if (!xmlDescriptor.isListEluded) { // Do the default thing if the children name has been specified
                val childName =
                    xmlDescriptor.getElementDescriptor(0).tagName
                super.writeBegin()
                val tagName = serialName

                //declare namespaces on the parent rather than the children of the list.
                if (tagName.prefix != childName.prefix &&
                    target.getNamespaceUri(childName.prefix) != childName.namespaceURI
                ) {
                    target.namespaceAttr(
                        childName.prefix,
                        childName.namespaceURI
                                        )
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

            if (xmlDescriptor.isListEluded) { // Use the outer decriptor and element index
                serializer.serialize(XmlEncoder(childDescriptor), value)
            } else {
                serializer.serialize(XmlEncoder(childDescriptor), value)
            }
        }

        override fun encodeStringElement(
            descriptor: SerialDescriptor,
            index: Int,
            value: String
                                        ) {
            if (index > 0) {
                XmlEncoder(xmlDescriptor.getElementDescriptor(index)).encodeString(
                    value
                                                                                  )
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!xmlDescriptor.isListEluded) {
                super.endStructure(descriptor)
            }
        }
    }
}
