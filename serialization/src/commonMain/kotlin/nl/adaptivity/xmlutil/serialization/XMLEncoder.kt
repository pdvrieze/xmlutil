/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.structure.*

internal open class XmlEncoderBase internal constructor(
    context: SerializersModule,
    config: XmlConfig,
    val target: XmlWriter
) : XmlCodecBase(context, config) {
    private var nextAutoPrefixNo: Int = 1

    override val namespaceContext: NamespaceContext
        get() = target.namespaceContext

    fun <T> SerializationStrategy<T>.serializeSafe(
        encoder: Encoder,
        value: T,
        isValueChild: Boolean = false
    ) = when (this) {
        is XmlSerializationStrategy -> serializeXML(encoder, target, value, isValueChild)
        else -> serialize(encoder, value)
    }

    fun <T> SerializationStrategy<T>.safeDeferredWrite(
        encoder: XmlEncoder,
        value: T,
        isValueChild: Boolean
    ): DeferredWrite {
        return when (this) {
            is XmlSerializationStrategy<T> ->
                DeferredWriteXmlSerializable(encoder, this, value, isValueChild)

            else -> DeferredWriteSerializable(encoder, this, value)
        }
    }

    /**
     * Encoder class for all primitives (except for initial values). It does not handle attributes. This does not
     * implement XmlOutput as
     */
    internal open inner class XmlEncoder(
        xmlDescriptor: XmlDescriptor,
        protected val elementIndex: Int,
        protected val discriminatorName: QName? = null
    ) : XmlCodec<XmlDescriptor>(xmlDescriptor), Encoder, XML.XmlOutput {

        override val target: XmlWriter get() = this@XmlEncoderBase.target

        override val serializersModule get() = this@XmlEncoderBase.serializersModule
        override val config: XmlConfig get() = this@XmlEncoderBase.config

        override fun ensureNamespace(qName: QName, isAttr: Boolean): QName {
            return this@XmlEncoderBase.ensureNamespace(qName, isAttr)
        }

        override fun encodeBoolean(value: Boolean) = encodeString(value.toString())

        override fun encodeByte(value: Byte) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toUByte().toString())
            else -> encodeString(value.toString())
        }

        override fun encodeShort(value: Short) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toUShort().toString())
            else -> encodeString(value.toString())
        }

        override fun encodeInt(value: Int) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toUInt().toString())
            else -> encodeString(value.toString())
        }

        override fun encodeLong(value: Long) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toULong().toString())
            else -> encodeString(value.toString())
        }

        override fun encodeFloat(value: Float) = encodeString(value.toString())

        override fun encodeDouble(value: Double) = encodeString(value.toString())

        override fun encodeChar(value: Char) = encodeString(value.toString())

        @OptIn(ExperimentalXmlUtilApi::class)
        override fun encodeString(value: String) {
            // string doesn't need parsing so take a shortcut
            val defaultValue = (xmlDescriptor as XmlValueDescriptor).default

            if (value == defaultValue) return

            when (xmlDescriptor.outputKind) {
                OutputKind.Inline, // shouldn't occur, but treat as element
                OutputKind.Element -> { // This may occur with list values.
                    target.smartStartTag(serialName) {
                        if (discriminatorName != null) {
                            val typeRef = ensureNamespace(config.policy.typeQName(xmlDescriptor), false)
                            smartWriteAttribute(discriminatorName, typeRef.toCName())
                        }
                        // Write the xml preserve attribute if the values starts or ends with whitespace
                        if (!xmlDescriptor.defaultPreserveSpace.withDefault(true) &&
                            (value.first().isWhitespace() || value.last().isWhitespace())
                        ) {
                            // this uses attribute directly as no namespace declaration is valid/needed
                            target.attribute(XMLConstants.XML_NS_URI, "space", "xml", "preserve")
                        }

                        if (xmlDescriptor.isCData) target.cdsect(value) else target.text(value)
                    }
                }

                OutputKind.Attribute -> smartWriteAttribute(serialName, value)

                OutputKind.Mixed,
                OutputKind.Text -> when {
                    xmlDescriptor.isCData -> target.cdsect(value)
                    else -> target.text(value)
                }
            }
        }

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            val stringRepr = config.policy.enumEncoding(enumDescriptor, index)
            encodeString(stringRepr)
        }

        @ExperimentalSerializationApi
        override fun encodeNotNullMark() {
            // Not null is presence, no mark needed
        }

        @ExperimentalSerializationApi
        override fun encodeNull() {
            val nilAttr = config.nilAttribute
            if (xmlDescriptor.outputKind == OutputKind.Element && nilAttr != null) {
                target.smartStartTag(serialName) {
                    if (discriminatorName != null) {
                        val typeRef = ensureNamespace(config.policy.typeQName(xmlDescriptor), true)
                        smartWriteAttribute(discriminatorName, typeRef.toCName())
                    }
                    smartWriteAttribute(nilAttr.first, nilAttr.second)
                }
            }
            // Null is absence, no mark needed, except in nil mode
        }

        override fun <T> encodeSerializableValue(
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            when {
                xmlDescriptor is XmlContextualDescriptor -> {
                    val actualDescriptor = xmlDescriptor.resolve(this, serializer.descriptor)
                    val delegateEncoder = XmlEncoder(actualDescriptor, elementIndex, discriminatorName)
                    actualDescriptor.effectiveSerializationStrategy(serializer).serializeSafe(delegateEncoder, value)
                }

                else ->
                    xmlDescriptor.effectiveSerializationStrategy(serializer).serializeSafe(this, value)
            }
        }

        @ExperimentalSerializationApi
        override fun encodeInline(descriptor: SerialDescriptor): Encoder {
            return XmlEncoder(xmlDescriptor.getElementDescriptor(0), elementIndex, discriminatorName)
        }

        override fun beginStructure(descriptor: SerialDescriptor): TagEncoder<XmlDescriptor> {
            return getCompositeEncoder(xmlDescriptor, elementIndex, discriminatorName).apply { writeBegin() }
        }
    }

    internal inner class NSAttrXmlEncoder(
        xmlDescriptor: XmlDescriptor,
        namespaces: Iterable<Namespace>,
        elementIndex: Int
    ) : XmlEncoder(xmlDescriptor, elementIndex) {

        private val namespaces = namespaces.toList()

        override fun beginStructure(descriptor: SerialDescriptor): TagEncoder<XmlDescriptor> {
            val compositeEncoder = super.beginStructure(descriptor)
            for (namespace in namespaces) {
                if (target.getNamespaceUri(namespace.prefix) == null) {
                    target.namespaceAttr(namespace)
                }
            }
            return compositeEncoder
        }
    }

    internal inner class PrimitiveEncoder(
        override val serializersModule: SerializersModule,
        private val xmlDescriptor: XmlDescriptor,
    ) : Encoder, XML.XmlOutput {
        val output = StringBuilder()
        override val config: XmlConfig get() = this@XmlEncoderBase.config
        override val serialName: QName get() = xmlDescriptor.tagName
        override val target: XmlWriter = XmlStringWriter(output, this@XmlEncoderBase.target)

        override fun ensureNamespace(qName: QName, isAttr: Boolean): QName {
            return this@XmlEncoderBase.ensureNamespace(qName, isAttr)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            throw IllegalArgumentException("Primitives cannot be structs")
        }

        override fun encodeBoolean(value: Boolean) = encodeString(value.toString())

        override fun encodeByte(value: Byte) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toUByte().toString())
            else -> encodeString(value.toString())
        }


        override fun encodeChar(value: Char) = encodeString(value.toString())

        override fun encodeDouble(value: Double) = encodeString(value.toString())

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            val tagName = xmlDescriptor.getElementDescriptor(index).tagName
            if (tagName.namespaceURI == "" && tagName.prefix == "") {
                encodeString(tagName.localPart)
            } else {
                encodeSerializableValue(QNameSerializer, tagName)
            }
        }

        override fun encodeFloat(value: Float) = encodeString(value.toString())

        @ExperimentalSerializationApi
        override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

        override fun encodeInt(value: Int) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toUInt().toString())
            else -> encodeString(value.toString())
        }


        override fun encodeLong(value: Long) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toULong().toString())
            else -> encodeString(value.toString())
        }


        @ExperimentalSerializationApi
        override fun encodeNull() = Unit

        override fun encodeShort(value: Short) = when (xmlDescriptor.isUnsigned) {
            true -> encodeString(value.toUShort().toString())
            else -> encodeString(value.toString())
        }


        override fun encodeString(value: String) {
            output.append(value)
        }

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            when (val s = xmlDescriptor.effectiveSerializationStrategy(serializer)) {
                is XmlSerializationStrategy -> s.serializeXML(this, target, value)
                else -> super.encodeSerializableValue(serializer, value)
            }
        }
    }

    private class XmlStringWriter(val a: Appendable, val delegate: XmlWriter) : XmlWriter {
        override val depth: Int get() = 0

        override var indentString: String
            get() = delegate.indentString
            set(value) {
                delegate.indentString = value
            }

        override fun setPrefix(prefix: String, namespaceUri: String) {
            delegate.setPrefix(prefix, namespaceUri)
        }

        override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
            delegate.namespaceAttr(namespacePrefix, namespaceUri)
        }

        override fun close() {}

        override fun flush() {}

        override fun startTag(namespace: String?, localName: String, prefix: String?) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun comment(text: String) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun text(text: String) {
            a.append(text)
        }

        override fun cdsect(text: String) {
            a.append(text)
        }

        override fun entityRef(text: String) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun processingInstruction(text: String) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun ignorableWhitespace(text: String) {
            a.append(text)
        }

        override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun docdecl(text: String) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun endDocument() {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override fun endTag(namespace: String?, localName: String, prefix: String?) {
            throw UnsupportedOperationException("Only writing strings is possible")
        }

        override val namespaceContext: NamespaceContext
            get() = delegate.namespaceContext

        override fun getNamespaceUri(prefix: String): String? {
            return delegate.getNamespaceUri(prefix)
        }

        override fun getPrefix(namespaceUri: String?): String? {
            return delegate.getPrefix(namespaceUri)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun getCompositeEncoder(
        xmlDescriptor: XmlDescriptor,
        elementIndex: Int,
        discriminatorName: QName?
    ) = when (xmlDescriptor.serialKind) {
        is PrimitiveKind -> throw AssertionError("A primitive is not a composite")

        SerialKind.CONTEXTUAL, // TODO handle contextual in a more elegant way
        StructureKind.MAP -> when (xmlDescriptor.outputKind) {
            OutputKind.Attribute -> {
                val valueType = xmlDescriptor.getElementDescriptor(1)
                if (!valueType.effectiveOutputKind.isTextual) {
                    throw XmlSerialException("Values of an attribute map must be textual or a qname")
                }
                val keyType = xmlDescriptor.getElementDescriptor(0)
                if (!keyType.effectiveOutputKind.isTextual) {
                    throw XmlSerialException("The keys of an attribute map must be string or qname")
                }
                AttributeMapEncoder(xmlDescriptor)
            }

            else -> MapEncoder(xmlDescriptor as XmlMapDescriptor, discriminatorName)
        }

        StructureKind.CLASS,
        StructureKind.OBJECT,
        SerialKind.ENUM -> TagEncoder(xmlDescriptor, discriminatorName)

        StructureKind.LIST -> when (xmlDescriptor.outputKind) {
            OutputKind.Attribute ->
                AttributeListEncoder(xmlDescriptor as XmlListDescriptor, elementIndex)

            OutputKind.Text ->
                ValueListEncoder(xmlDescriptor as XmlListDescriptor)

            else -> ListEncoder(xmlDescriptor as XmlListDescriptor, elementIndex, discriminatorName)
        }

        is PolymorphicKind -> PolymorphicEncoder(xmlDescriptor as XmlPolymorphicDescriptor)
    }

    /**
     * Encoder that writes the elements as if they were present in the parent (without wrapping it).
     *
     * @property parent The containing tag encoder
     * @property childInParentIndex The index of the outermost inline value in the parent
     * @param inlineDescriptor The actual descriptor of the "inline" type
     * @param nestedChildIndex In the case that the inline type is not actually inline, this contains
     *   the index of the current value. By default 0 (the child descriptor of the inline)
     */
    private inner class InlineEncoder(
        private val parent: TagEncoder<XmlDescriptor>,
        private val childInParentIndex: Int,
        inlineDescriptor: XmlDescriptor = parent.xmlDescriptor.getElementDescriptor(childInParentIndex),
        nestedChildIndex: Int = 0,
    ) : XmlEncoder(inlineDescriptor, nestedChildIndex, null) {

        private val elementDescriptor get() = xmlDescriptor.getElementDescriptor(elementIndex)

        override fun encodeString(value: String) {
            parent.defer(
                childInParentIndex,
                elementDescriptor,
                DeferredWriteString(elementDescriptor as XmlValueDescriptor, value)
            )
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun <T> encodeSerializableValue(
            serializer: SerializationStrategy<T>,
            value: T
        ) {

            // just don't defer as that will be done on the parent anyway
            val encoder = when {
                elementDescriptor.doInline -> InlineEncoder(parent, childInParentIndex, elementDescriptor)
                elementDescriptor.kind is PrimitiveKind -> this
                else -> XmlEncoder(elementDescriptor, elementIndex)
            }

            val effectiveSerializer = elementDescriptor.effectiveSerializationStrategy(serializer)
            parent.defer(
                childInParentIndex,
                effectiveSerializer.safeDeferredWrite(encoder, value, parent.isValueChild(childInParentIndex))
            )
        }

        @ExperimentalSerializationApi
        override fun encodeInline(descriptor: SerialDescriptor): Encoder {
            // should only be used without calling a beginStructure from generated serializers
            return this
        }

        override fun beginStructure(descriptor: SerialDescriptor): TagEncoder<XmlDescriptor> {
            // Create the encoder, but call writeBegin on the wrapped encoder, not the original
            return InlineTagEncoder(
                parent,
                elementDescriptor,
                childInParentIndex
            ).apply { writeBegin() }
        }
    }

    private inner class InlineTagEncoder(
        private val delegate: TagEncoder<XmlDescriptor>,
        xmlDescriptor: XmlDescriptor,
        private val childInParentIndex: Int
    ) : TagEncoder<XmlDescriptor>(xmlDescriptor, null) {

        override fun writeBegin() {
            // Don't write any tag as we're inline. We also can't write a discriminator inline.
        }

        override fun <T> encodeSerializableElement(
            elementDescriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            delegate.encodeSerializableElement(elementDescriptor, childInParentIndex, serializer, value)
        }

        @ExperimentalSerializationApi
        override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
            return delegate.encodeInlineElement(descriptor, childInParentIndex)
        }

        @ExperimentalSerializationApi
        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
            return delegate.shouldEncodeElementDefault(descriptor, childInParentIndex)
        }

        @ExperimentalSerializationApi
        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            delegate.encodeNullableSerializableElement(descriptor, childInParentIndex, serializer, value)
        }

        override fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            delegate.encodeStringElement(elementDescriptor, childInParentIndex, value)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            delegate.flushDeferred() // Only flush deferred, but don't end the tag.
        }
    }

    internal open inner class TagEncoder<out D : XmlDescriptor>(
        xmlDescriptor: D,
        protected val discriminatorName: QName?,
        private var deferring: Boolean = true
    ) : XmlTagCodec<D>(xmlDescriptor), CompositeEncoder, XML.XmlOutput {

        override val target: XmlWriter get() = this@XmlEncoderBase.target
        override val namespaceContext: NamespaceContext get() = this@XmlEncoderBase.target.namespaceContext

        override fun ensureNamespace(qName: QName, isAttr: Boolean): QName {
            return this@XmlEncoderBase.ensureNamespace(qName, isAttr)
        }

        private val deferredBuffer = arrayOfNulls<DeferredWrite>(xmlDescriptor.elementsCount)

        private val reorderInfo =
            (xmlDescriptor as? XmlCompositeDescriptor)?.childReorderMap

        open fun writeBegin() {
            target.smartStartTag(serialName)
            writeNamespaceDecls()
            writeDiscriminatorAttributeIfNeeded()
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        internal fun writeNamespaceDecls() {
            for (namespace: Namespace in xmlDescriptor.namespaceDecls) {
                ensureNamespace(namespace)
            }
        }

        private fun writeDiscriminatorAttributeIfNeeded() {
            if (discriminatorName != null) {
                val typeName = ensureNamespace(config.policy.typeQName(xmlDescriptor), true)
                smartWriteAttribute(discriminatorName, typeName.toCName())
            }
        }

        open fun defer(index: Int, deferred: DeferredWrite) {
            defer(index, xmlDescriptor.getElementDescriptor(index), deferred)
        }

        @OptIn(ExperimentalSerializationApi::class)
        open fun defer(
            index: Int,
            itemDescriptor: XmlDescriptor = xmlDescriptor.getElementDescriptor(index),
            deferred: DeferredWrite
        ) {
            if (itemDescriptor.doInline) {
                // Don't defer inline values as it has a problem with the value serializer deferring
                // This is needed as we allow manual inline that is not a value class (and thus
                // doesn't call encodeInlineElement).
                deferred(this, xmlDescriptor.serialDescriptor, index)
            } else if (!deferring) { // We should never defer if we are processing deferred elements
                deferred(this, xmlDescriptor.serialDescriptor, index)
            } else if (reorderInfo != null) {
                deferredBuffer[reorderInfo[index]] = deferred
            } else {
                val outputKind =
                    itemDescriptor.outputKind
                if (outputKind == OutputKind.Attribute) {
                    deferred(this, xmlDescriptor.serialDescriptor, index)
                } else {
                    deferredBuffer[index] = deferred
                }
            }
        }

        final override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            encodeSerializableElement(xmlDescriptor.getElementDescriptor(index), index, serializer, value)
        }

        @OptIn(ExperimentalSerializationApi::class)
        internal open fun <T> encodeSerializableElement(
            elementDescriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            val encoder = when {
                elementDescriptor.doInline -> InlineEncoder(this, index, elementDescriptor)
                else -> XmlEncoder(elementDescriptor, index)
            }

            val effectiveSerializer = elementDescriptor.effectiveSerializationStrategy(serializer)

            defer(index, effectiveSerializer.safeDeferredWrite(encoder, value, isValueChild(index)))
        }

        @ExperimentalSerializationApi
        override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
            val elementDescriptor = xmlDescriptor.getElementDescriptor(index)
            return InlineEncoder(this, index, elementDescriptor)
        }

        @ExperimentalSerializationApi
        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
            val elementDescriptor = xmlDescriptor.getElementDescriptor(index)

            return config.policy.shouldEncodeElementDefault(elementDescriptor)
        }

        final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeByteElement(
            descriptor: SerialDescriptor,
            index: Int,
            value: Byte
        ) {
            when (xmlDescriptor.isUnsigned) {
                true -> encodeStringElement(descriptor, index, value.toUByte().toString())
                else -> encodeStringElement(descriptor, index, value.toString())
            }
        }


        final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            when (xmlDescriptor.isUnsigned) {
                true -> encodeStringElement(descriptor, index, value.toUShort().toString())
                else -> encodeStringElement(
                    descriptor, index, value.toString()
                )
            }
        }

        final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            when (xmlDescriptor.isUnsigned) {
                true -> encodeStringElement(descriptor, index, value.toUInt().toString())
                else -> encodeStringElement(descriptor, index, value.toString())
            }
        }

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

        @ExperimentalSerializationApi
        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            val nilAttr = config.nilAttribute
            val elemDescriptor = xmlDescriptor.getElementDescriptor(index)
            if (value != null) {
                encodeSerializableElement(descriptor, index, serializer, value)
            } else if (serializer.descriptor.isNullable) {
                val encoder = when {
                    elemDescriptor.doInline -> InlineEncoder(this, index, elemDescriptor)
                    else -> XmlEncoder(elemDescriptor, index)
                }

                // This should be safe as we are handling the case when a serializer explicitly handles nulls
                // In such case cast it to accept a null parameter and serialize through the serializer, not
                // indirectly.
                @Suppress("UNCHECKED_CAST")
                val deferredWrite = (serializer as SerializationStrategy<T?>).safeDeferredWrite(encoder, null, isValueChild(index))
                defer(index, deferredWrite)
            } else if (nilAttr != null && elemDescriptor.effectiveOutputKind == OutputKind.Element) {
                defer(index, DeferredWriteNilTag(elemDescriptor.tagName, nilAttr))
            }

            // Null is the absense of values, no need to do more
        }

        internal fun isValueChild(index: Int) = xmlDescriptor.getValueChild() == index

        final override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            val elementDescriptor = xmlDescriptor.getElementDescriptor(index)

            encodeStringElement(elementDescriptor, index, value)
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        internal open fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            val defaultValue = (elementDescriptor as? XmlValueDescriptor)?.default

            if (value == defaultValue) return


            when (elementDescriptor.outputKind) {
                OutputKind.Inline, // Treat inline as if it was element if it occurs (shouldn't happen)
                OutputKind.Element -> defer(index, DeferredWriteStringElement(elementDescriptor, value))

                OutputKind.Attribute -> doWriteAttribute(index, elementDescriptor.tagName, value)
                OutputKind.Mixed,
                OutputKind.Text -> {
                    // Write the xml preserve attribute if the values starts or ends with whitespace
                    if (!elementDescriptor.defaultPreserveSpace.withDefault(true) &&
                        (value.first().isWhitespace() || value.last().isWhitespace())
                    ) {
                        // this uses attribute directly as no namespace declaration is valid/needed
                        target.attribute(XMLConstants.XML_NS_URI, "space", "xml", "preserve")
                    }
                    defer(index, DeferredWriteText(value))
                }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            flushDeferred()
            target.endTag(serialName)
        }

        internal fun flushDeferred() {
            // This is a separate function to allow InlineTagEncoder to flush but not write the end tag
            deferring = false

//            val actions = deferredBuffer.sortedBy { it.first }
            val serialDescriptor = xmlDescriptor.serialDescriptor
            for (deferredIdx in deferredBuffer.indices) {
                val deferred = deferredBuffer[deferredIdx]
                deferred?.invoke(this, serialDescriptor, deferredIdx)
                deferredBuffer[deferredIdx] = null
            }
        }

        open fun doWriteAttribute(index: Int, name: QName, value: String) {
            if (reorderInfo != null && deferring) {
                // set directly as otherwise defer will write attributes
                deferredBuffer[reorderInfo[index]] = DeferredWriteAttribute(name, value)
            } else {
                smartWriteAttribute(name, value)
            }
        }

    }

    internal class DefaultDeferredWrite(private val action: CompositeEncoder.() -> Unit) : DeferredWrite() {
        override operator fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            compositeEncoder.action()
        }
    }

    internal inner class DeferredWriteAttribute(val attrName: QName, val value: String) : DeferredWrite() {
        override fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            smartWriteAttribute(attrName, value)
        }
    }

    internal abstract class DeferredWrite {
        abstract operator fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int)
    }

    internal class DeferredWriteString(val xmlDescriptor: XmlValueDescriptor, val value: String) : DeferredWrite() {
        override fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            compositeEncoder.encodeStringElement(xmlDescriptor, index, this.value)
        }
    }

    internal class DeferredWriteText(val value: String) : DeferredWrite() {
        override fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            val target = compositeEncoder.target
            when {
                compositeEncoder.xmlDescriptor.getElementDescriptor(index).isCData -> target.safeCdsect(value)
                else -> target.text(value)
            }
        }
    }

    internal inner class DeferredWriteStringElement(val elementDescriptor: XmlDescriptor, val value: String) : DeferredWrite() {
        override fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            target.smartStartTag(elementDescriptor.tagName) {
                // Write the xml preserve attribute if the values starts or ends with whitespace
                if (!elementDescriptor.defaultPreserveSpace.withDefault(true) &&
                    (value.first().isWhitespace() || value.last().isWhitespace())
                ) {
                    // this uses attribute directly as no namespace declaration is valid/needed
                    target.attribute(XMLConstants.XML_NS_URI, "space", "xml", "preserve")
                }

                if (elementDescriptor.isCData) target.cdsect(value) else target.text(value)
            }
        }
    }

    internal inner class DeferredWriteNilTag(val tagName: QName, val nilAttr: Pair<QName, String>) : DeferredWrite() {
        override fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            target.smartStartTag(tagName) {
                smartWriteAttribute(nilAttr.first, nilAttr.second)
            }
        }
    }

    internal class DeferredWriteSerializable<T>(
        val encoder: Encoder,
        val serializer: SerializationStrategy<T>,
        val value: T
    ) : DeferredWrite() {
        override fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            serializer.serialize(encoder, value)
        }
    }

    internal class DeferredWriteXmlSerializable<T>(
        val encoder: Encoder,
        val serializer: XmlSerializationStrategy<T>,
        val value: T,
        val isValueChild: Boolean
    ) : DeferredWrite() {
        override fun invoke(compositeEncoder: TagEncoder<*>, descriptor: SerialDescriptor, index: Int) {
            serializer.serializeXML(encoder, compositeEncoder.target, value, isValueChild)
        }
    }

    private fun NamespaceContext.nextAutoPrefix(): String {
        var prefix: String
        do {
            prefix = "n${nextAutoPrefixNo++}"
        } while (getNamespaceURI(prefix) != null)
        return prefix
    }

    private fun ensureNamespace(namespace: Namespace) {
        if (namespaceContext.getPrefix(namespace.namespaceURI) != null)
            return

        val effectivePrefix = when {
            namespaceContext.getNamespaceURI(namespace.prefix) == null -> namespace.prefix
            else -> namespaceContext.nextAutoPrefix()
        }
        target.namespaceAttr(effectivePrefix, namespace.namespaceURI)
    }

    /**
     * Determine/reserve a namespace for this element.
     * Will reuse a prefix if available.
     */
    private fun ensureNamespace(qName: QName, isAttr: Boolean): QName {
        when {
            !isAttr -> Unit // This stage is only for attribute handling
            // for empty namespace uri, force empty prefix
            qName.namespaceURI == "" -> return qName.copy(prefix = "")

            qName.prefix == "" -> { // the namespace is set
                val effectivePrefix = target.namespaceContext.getPrefixes(qName.namespaceURI).asSequence()
                    .firstOrNull { it.isNotEmpty() }
                    ?: namespaceContext.nextAutoPrefix().also {
                        target.namespaceAttr(it, qName.namespaceURI)
                    }
                return qName.copy(prefix = effectivePrefix)
            }
        }

        val registeredNamespace = target.getNamespaceUri(qName.getPrefix())

        // If things match, or no namespace, no need to do anything
        if (registeredNamespace == qName.namespaceURI) return qName

        val registeredPrefix =
            target.namespaceContext.getPrefixes(qName.namespaceURI).asSequence().filterNot { isAttr && it.isEmpty() }
                .firstOrNull()

        return when { // Attributes with empty prefix are always in the default namespace, so if they are to be otherwise

            // There is a prefix to reuse, just reuse that (TODO make configurable)
            registeredPrefix != null -> qName.copy(prefix = registeredPrefix)

            // If there is a namespace for this prefix, and it doesn't match, create a new prefix
            registeredNamespace != null -> { // prefix no longer valid
                val prefix = qName.prefix
                var lastDigitIdx = prefix.length
                while (lastDigitIdx > 0 && prefix[lastDigitIdx - 1].isDigit()) {
                    lastDigitIdx -= 1
                }
                val prefixBase: String
                val prefixStart: Int
                when {
                    lastDigitIdx == 0 -> {
                        prefixBase = "ns"
                        prefixStart = 0
                    }

                    lastDigitIdx < prefix.length -> {
                        prefixBase = prefix.substring(0, lastDigitIdx)
                        prefixStart = prefix.substring(lastDigitIdx).toInt()
                    }

                    else -> {
                        prefixBase = prefix
                        prefixStart = 0
                    }
                }
                val newPrefix = (prefixStart..Int.MAX_VALUE)
                    .asSequence()
                    .map { "${prefixBase}$it" }
                    .first { target.getNamespaceUri(it) == null }

                target.namespaceAttr(newPrefix, qName.namespaceURI)
                qName.copy(prefix = newPrefix)
            }

            else -> { // No existing namespace or prefix
                target.namespaceAttr(qName.prefix, qName.namespaceURI)
                qName
            }
        }
    }

    /**
     * Helper function that ensures writing the namespace attribute if needed.
     */
    private fun smartWriteAttribute(name: QName, value: String) {
        val argPrefix = name.getPrefix()
        val resolvedNamespace = target.getNamespaceUri(argPrefix)
        val existingPrefix = target.getPrefix(name.namespaceURI)?.takeIf { it.isNotEmpty() }

        val effectiveQName: QName = when {
            // Default namespace uses default prefix
            name.namespaceURI.isEmpty() -> QName(name.localPart)

            // handle case with qname with default prefix but not default namespace (illegal for args)
            // when the existing prefix is set (which cannot be empty) use that, otherwise ensureNamespace
            // is more robust.
            argPrefix.isEmpty() -> existingPrefix?.let { name.copy(it) } ?: ensureNamespace(name, true)

            // If the prefix doesn't resolve to a namespace use the existing name (for non-empty prefix)
            // this means argPrefix maps to a different namespace (resolvedNamespace == null), but
            // existingPrefix maps to the correct namespace.
            resolvedNamespace == null && existingPrefix != null -> name.copy(prefix = existingPrefix)

            else -> ensureNamespace(name, true)
        }

        target.writeAttribute(effectiveQName, value)
    }

    internal inner class PolymorphicEncoder(xmlDescriptor: XmlPolymorphicDescriptor) :
        TagEncoder<XmlPolymorphicDescriptor>(
            xmlDescriptor,
            discriminatorName = null,
            deferring = false
        ), XML.XmlOutput {

        override fun writeBegin() {
            // write the container if we're in tag mode (not type attribute/transparent)
            if (xmlDescriptor.polymorphicMode == PolymorphicMode.TAG) super.writeBegin()
        }

        override fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed
            val polymorphicMode = xmlDescriptor.polymorphicMode
            when {
                index == 0 -> {
                    if (polymorphicMode == PolymorphicMode.TAG) { // Don't write for non-tag mode
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
                            OutputKind.Element -> target.smartStartTag(childDesc.tagName) {
                                text(value)
                            }

                            OutputKind.Text ->
                                throw XmlSerialException("the type for a polymorphic child cannot be a text")
                        }
                    } // else if (index == 0) { } // do nothing
                }

                polymorphicMode == PolymorphicMode.TRANSPARENT -> {
                    when {
                        isMixed -> target.text(value)
                        else -> target.smartStartTag(serialName) { text(value) }
                    }
                }

                polymorphicMode is PolymorphicMode.ATTR -> {
                    target.smartStartTag(serialName) {
                        val attrQName = ensureNamespace(config.policy.typeQName(elementDescriptor), true)
                        smartWriteAttribute(polymorphicMode.name, attrQName.toCName())

                        text(value)
                    }
                }

                else -> super.encodeStringElement(elementDescriptor, index, value)
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun <T> encodeSerializableElement(
            elementDescriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            val childXmlDescriptor =
                xmlDescriptor.getPolymorphicDescriptor(serializer.descriptor)

            val discriminatorName = (xmlDescriptor.polymorphicMode as? PolymorphicMode.ATTR)?.name
            val encoder = XmlEncoder(childXmlDescriptor, index, discriminatorName)
            serializer.serializeSafe(encoder, value, isValueChild(index))
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Only write the tag here if we're not in attribute or transparent mode
            if (xmlDescriptor.polymorphicMode == PolymorphicMode.TAG) {
                super.endStructure(descriptor)
            }
        }

    }

    internal inner class AttributeMapEncoder(xmlDescriptor: XmlDescriptor) :
        TagEncoder<XmlDescriptor>(xmlDescriptor, null) {

        private lateinit var entryKey: QName

        override fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            when (index % 2) {
                0 -> entryKey = QName(value)
                1 -> smartWriteAttribute(entryKey, value)
            }
        }

        override fun <T> encodeSerializableElement(
            elementDescriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            if (index % 2 == 0) {
                val effectiveSerializer = elementDescriptor.effectiveSerializationStrategy(serializer)

                entryKey = when {
                    value is String -> QName(value)
                    value is QName -> value // this should work as long as the serializer doesn't transform the value
                    effectiveSerializer is XmlSerializationStrategy -> {
                        val primitiveEncoder = PrimitiveEncoder(serializersModule, xmlDescriptor).apply {
                            effectiveSerializer.serializeXML(this, target, value)
                        }
                        val str = primitiveEncoder.output.toString()
                        val cPos = str.indexOf(':')
                        when {
                            cPos < 0 -> QName(str)
                            else -> {
                                val prefix = str.substring(0, cPos)
                                val ns = target.getNamespaceUri(prefix)
                                if (ns == null) QName(str) else QName(ns, str.substring(cPos + 1), prefix)
                            }
                        }
                    }

                    else -> QName(PrimitiveEncoder(serializersModule, xmlDescriptor).apply {
                        encodeSerializableValue(effectiveSerializer, value)
                    }.output.toString())
                }
            } else {
                val effectiveSerializer =
                    xmlDescriptor.getElementDescriptor(1).effectiveSerializationStrategy(serializer)

                val entryValue = PrimitiveEncoder(serializersModule, xmlDescriptor).apply {
                    encodeSerializableValue(effectiveSerializer, value)
                }.output.toString()
                doWriteAttribute(index, entryKey, entryValue)
            }
        }

        override fun writeBegin() {}

        override fun endStructure(descriptor: SerialDescriptor) {}
    }

    internal abstract inner class TextualListEncoder(xmlDescriptor: XmlListDescriptor) :
        TagEncoder<XmlListDescriptor>(xmlDescriptor, null) {

        protected val valueBuilder = StringBuilder()
        private val delimiter = xmlDescriptor.delimiters.first()

        init {
            var d: XmlDescriptor = xmlDescriptor
            var ok: OutputKind
            do {
                d = d.getElementDescriptor(0)
                ok = d.outputKind
            } while (ok == OutputKind.Inline)

            if (ok != OutputKind.Attribute && ok != OutputKind.Text) {
                throw IllegalArgumentException("An xml list stored in an attribute must store atomics, not structs")
            }
        }

        override fun writeBegin() {}

        override fun <T> encodeSerializableElement(
            elementDescriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            val encoder = PrimitiveEncoder(serializersModule, elementDescriptor)
            encoder.encodeSerializableValue(serializer, value)
            encodeStringElement(elementDescriptor, index, encoder.output.toString())
        }

        override fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            if (valueBuilder.isNotEmpty()) valueBuilder.append(delimiter)

            valueBuilder.append(value)
        }

        abstract override fun endStructure(descriptor: SerialDescriptor)
    }

    internal inner class AttributeListEncoder(xmlDescriptor: XmlListDescriptor, private val elementIndex: Int) :
        TextualListEncoder(xmlDescriptor) {

        override fun endStructure(descriptor: SerialDescriptor) {
            doWriteAttribute(elementIndex, xmlDescriptor.tagName, valueBuilder.toString())
        }
    }

    internal inner class ValueListEncoder(xmlDescriptor: XmlListDescriptor) :
        TextualListEncoder(xmlDescriptor) {

        override fun endStructure(descriptor: SerialDescriptor) {
            target.text(valueBuilder.toString())
        }
    }

    /**
     * Writer that does not actually write an outer tag unless a [XmlChildrenName] is specified. In XML children don't need to
     * be wrapped inside a list (unless it is the root). If [XmlChildrenName] is not specified it will determine tag names
     * as if the list was not present and there was a single value.
     */
    internal inner class ListEncoder(
        xmlDescriptor: XmlListDescriptor,
        private val listChildIdx: Int,
        discriminatorName: QName?
    ) : TagEncoder<XmlListDescriptor>(xmlDescriptor, discriminatorName, deferring = false), XML.XmlOutput {

        private val parentXmlDescriptor: XmlDescriptor? get() = xmlDescriptor.tagParent.descriptor as? XmlDescriptor

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
            elementDescriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            val childDescriptor = xmlDescriptor.getElementDescriptor(0)
            val childEncoder = XmlEncoder(childDescriptor, index)

            serializer.serializeSafe(childEncoder, value, parentXmlDescriptor?.getValueChild() == listChildIdx)
        }

        override fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            if (index > 0) {
                XmlEncoder(elementDescriptor, index).encodeString(value)
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!xmlDescriptor.isListEluded) {
                super.endStructure(descriptor)
            }
        }
    }


    internal inner class MapEncoder(xmlDescriptor: XmlMapDescriptor, discriminatorName: QName? = null) :
        TagEncoder<XmlDescriptor>(xmlDescriptor, discriminatorName, deferring = false) {

        private val mapDescriptor get() = xmlDescriptor as XmlMapDescriptor

        private lateinit var keySerializer: SerializationStrategy<*>
        private var keyValue: Any? = null
        private var writingValue: Boolean = false

        override fun encodeStringElement(elementDescriptor: XmlDescriptor, index: Int, value: String) {
            when {
                index % 2 == 0 -> {
                    keySerializer = String.serializer()
                    keyValue = value
                }

                !writingValue ->
                    encodeSerializableElement(xmlDescriptor, index, String.serializer(), value)

                elementDescriptor.outputKind == OutputKind.Element -> if (elementDescriptor.isCData) {
                    target.cdsect(value)
                } else {
                    target.text(value)
                }

                else -> super.encodeStringElement(elementDescriptor, index, value)
            }

        }

        override fun <T> encodeSerializableElement(
            elementDescriptor: XmlDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {

            if (index % 2 == 0) {
                keySerializer = elementDescriptor.effectiveSerializationStrategy(serializer)
                keyValue = value
                /*
                                = when (effectiveKeySerializer) {
                                    XmlQNameSerializer -> value as QName
                                    else -> QName(PrimitiveEncoder(serializersModule, xmlDescriptor).apply {
                                        encodeSerializableValue(effectiveKeySerializer, value)
                                    }.output.toString())
                                }
                */
            } else { // element
                if (writingValue) {
                    super.encodeSerializableElement(elementDescriptor, index, serializer, value)
                    return
                }

                val valueDescriptor = xmlDescriptor.getElementDescriptor(1)
                val effectiveSerializer =
                    valueDescriptor.effectiveSerializationStrategy(serializer)

                val keyDescriptor = xmlDescriptor.getElementDescriptor(0)
                if (mapDescriptor.isValueCollapsed) {
                    target.smartStartTag(valueDescriptor.tagName) {
                        // Use the primitive encoder to just get the serialized attribute value. This
                        // avoids issues with xml nesting.
                        val keyEncoder = PrimitiveEncoder(serializersModule, keyDescriptor)
                        @Suppress("UNCHECKED_CAST") // it has been stored so cannot be typed
                        keyEncoder.encodeSerializableValue(keySerializer as SerializationStrategy<Any?>, keyValue)

                        val serializedKey = keyEncoder.output.toString()
                        smartWriteAttribute(keyDescriptor.tagName, serializedKey)

                        // Use an inline encoder here as we write the outer tag manually (to add the key attribute)
                        val encoder = InlineEncoder(this@MapEncoder, index, xmlDescriptor, 1)
                        writingValue = true
                        try {
                            effectiveSerializer.serializeSafe(encoder, value)
                        } finally {
                            writingValue = false
                        }
                    }
                } else {
                    target.smartStartTag(mapDescriptor.entryName) { // Tag name is not good
                        val keyEncoder = XmlEncoder(keyDescriptor, index - 1)
                        @Suppress("UNCHECKED_CAST") // it has been stored so cannot be typed
                        keyEncoder.encodeSerializableValue(keySerializer as SerializationStrategy<Any?>, keyValue)

                        effectiveSerializer.serializeSafe(XmlEncoder(valueDescriptor, index), value)
                    }

                }

            }
        }

        override fun writeBegin() {
            if (!mapDescriptor.isListEluded) {
                super.writeBegin()
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!mapDescriptor.isListEluded) {
                super.endStructure(descriptor)
            }
        }
    }

}
