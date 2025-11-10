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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.ChunkedDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.internal.AbstractCollectionSerializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSI_NS_URI
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.impl.*
import nl.adaptivity.xmlutil.serialization.structure.*
import nl.adaptivity.xmlutil.util.CompactFragmentSerializer
import nl.adaptivity.xmlutil.util.XmlBooleanSerializer
import nl.adaptivity.xmlutil.util.XmlDoubleSerializer
import nl.adaptivity.xmlutil.util.XmlFloatSerializer

@OptIn(ExperimentalSerializationApi::class)
internal open class XmlDecoderBase internal constructor(
    context: SerializersModule,
    config: XmlConfig,
    input: XmlReader
) : XmlCodecBase(context, config) {

    val input: XmlPeekingReader = PseudoBufferedReader(input)

    override val namespaceContext: NamespaceContext get() = input.namespaceContext

    private val _idMap = mutableMapOf<String, Any>()

    fun hasNullMark(): Boolean {
        if (input.eventType == EventType.START_ELEMENT) {
            val declNilAttr: QName = config.nilAttributeName
                ?: return when {
                    !config.isAlwaysDecodeXsiNil -> false
                    else -> when (input.getAttributeValue(XSI_NS_URI, "nil")) {
                        "true", "1" -> true
                        else -> false
                    }
                }

            val targetNS = declNilAttr.namespaceURI
            val targetName = declNilAttr.localPart

            for (i in 0 until input.attributeCount) {
                val ns = input.getAttributeNamespace(i)
                when (ns) {
                    targetNS -> if (input.getAttributeLocalName(i) == targetName) {
                        return input.getAttributeValue(i) == config.nilAttributeValue
                    }

                    XSI_NS_URI -> if(config.isAlwaysDecodeXsiNil && input.getAttributeLocalName(i) == "nil") {
                        return when(input.getAttributeValue(i)) {
                            "true", "1" -> true
                            else -> false
                        }
                    }
                }
            }
            return false
        }
        return false
    }

    fun <T> DeserializationStrategy<T>.deserializeSafe(
        decoder: Decoder,
        isParseAllSiblings: Boolean,
        previousValue: T? = null,
        isValueChild: Boolean = false
    ): T {
        val initialDepth = if (input.eventType == EventType.START_ELEMENT) input.depth - 1 else input.depth
        val r = handleParseError {
            when (this) {
                is XmlDeserializationStrategy -> {
                    val safeInput = SubDocumentReader(input, isParseAllSiblings)
                    safeInput.next() // start the parsing to maintain existing behaviour.
                    deserializeXML(decoder, safeInput, previousValue, isValueChild).also {
                        if (!input.hasPeekItems && input.eventType == EventType.END_ELEMENT && initialDepth == input.depth) {
                            input.pushBackCurrent()
                        }
                    }
                }

                else -> deserialize(decoder)
            }
        }
        return r
    }

    private inline fun <R> handleParseError(body: () -> R): R {
        try {
            val initialLocation = input.extLocationInfo
            return body()
        } catch (e: XmlSerialException) {
            throw e
        } catch (e: XmlException) {
            throw XmlParsingException(e.locationInfo, e.message ?: "<unknown>", e)
        } catch (e: Exception) {
            throw XmlParsingException(input.extLocationInfo, e.message ?: "<unknown>", e)
        }
    }

    abstract inner class DecodeCommons(
        xmlDescriptor: XmlDescriptor,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : XmlCodec<XmlDescriptor>(xmlDescriptor), XML.XmlInput, Decoder {
        final override val config: XmlConfig get() = this@XmlDecoderBase.config
        final override val serializersModule: SerializersModule get() = this@XmlDecoderBase.serializersModule

        protected val preserveSpace: DocumentPreserveSpace = inheritedPreserveWhitespace.withDefault(xmlDescriptor.defaultPreserveSpace)

        override fun decodeNull(): Nothing? {
            // We don't write nulls, so if we know that we have a null we just return it
            return null
        }

        override fun decodeBoolean(): Boolean = handleParseError {
            when {
                config.policy.isStrictBoolean -> XmlBooleanSerializer.deserialize(this)
                else -> decodeStringCollapsed().toBoolean()
            }
        }

        override fun decodeByte(): Byte = handleParseError {
            val str = decodeStringCollapsed()
            when (xmlDescriptor.isUnsigned) {
                true -> str.toUByte().toByte()
                else -> str.toByte()
            }
        }

        override fun decodeShort(): Short = handleParseError {
            val str = decodeStringCollapsed()
            when (xmlDescriptor.isUnsigned) {
                true -> str.toUShort().toShort()
                else -> str.toShort()
            }
        }

        override fun decodeInt(): Int = handleParseError {
            val str = decodeStringCollapsed()
            when (xmlDescriptor.isUnsigned) {
                true -> str.toUInt().toInt()
                else -> str.toInt()
            }
        }

        override fun decodeLong(): Long = handleParseError {
            val str = decodeStringCollapsed()
            when (xmlDescriptor.isUnsigned) {
                true -> str.toULong().toLong()
                else -> str.toLong()
            }
        }

        override fun decodeFloat(): Float = handleParseError {
            when {
                config.policy.isXmlFloat -> XmlFloatSerializer.deserialize(this)
                else -> when (val s = decodeStringCollapsed()) {
                    "INF" -> Float.POSITIVE_INFINITY
                    "-INF" -> Float.NEGATIVE_INFINITY
                    else -> s.toFloat()
                }
            }
        }

        override fun decodeDouble(): Double = handleParseError {
            when {
                config.policy.isXmlFloat -> XmlDoubleSerializer.deserialize(this)
                else -> when (val s = decodeStringCollapsed()) {
                    "INF" -> Double.POSITIVE_INFINITY
                    "-INF" -> Double.NEGATIVE_INFINITY
                    else -> s.toDouble()
                }
            }
        }

        override fun decodeChar(): Char = handleParseError {
            decodeStringCollapsed().single()
        }

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
            val stringName = decodeStringCollapsed()
            for (i in 0 until enumDescriptor.elementsCount) {
                if (stringName == config.policy.enumEncoding(enumDescriptor, i)) return i
            }
            throw XmlSerialException("No enum constant found for name $stringName in ${enumDescriptor.serialName}", input.extLocationInfo)
        }

        fun decodeStringCollapsed(defaultOverEmpty: Boolean = true): String =
            xmlCollapseWhitespace(decodeStringImpl(defaultOverEmpty))

        abstract fun decodeStringImpl(defaultOverEmpty: Boolean = true): String
        override fun decodeString(): String = decodeStringImpl(false)
    }

    /**
     * @param xmlDescriptor The descriptor for the value that this decoder should decode
     * @property polyInfo If this was instantiated due to a polymorphic match, this property holds that information
     * @property attrIndex If this was instantiated to deserialize an attribute this parameter determines which
     *                     attribute it deserialize (index into the attribute list of the containing tag)
     */
    internal open inner class
    XmlDecoder @OptIn(ExperimentalXmlUtilApi::class) constructor(
        xmlDescriptor: XmlDescriptor,
        protected val polyInfo: PolyInfo? = null,
        protected val isValueChild: Boolean = false,
        val attrIndex: Int = -1,
        inheritedPreserveWhitespace: DocumentPreserveSpace
    ) : DecodeCommons(xmlDescriptor, inheritedPreserveWhitespace), Decoder, XML.XmlInput, ChunkedDecoder {
        final override val input: XmlPeekingReader get() = this@XmlDecoderBase.input

        private var triggerInline = false

        protected open val typeDiscriminatorName: QName? get() = null

        override fun decodeNotNullMark(): Boolean {
            if (hasNullMark()) return false

            // No null values if we don't have a null mark (or are not looking at an element) unless
            // the entire document is empty (not sure the parser is happy with it)
            return input.eventType != EventType.END_DOCUMENT
        }

        override fun decodeNull(): Nothing? {
            if (hasNullMark()) { // we have a nullable element marked nil using a nil attribute
                input.nextTag()
                if (!config.isUnchecked) input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
                return null
            }
            return super.decodeNull()
        }

        @ExperimentalSerializationApi
        override fun decodeInline(descriptor: SerialDescriptor): Decoder {
            triggerInline = true
            return this
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        override fun decodeStringImpl(defaultOverEmpty: Boolean): String {
            val descOutputKind = xmlDescriptor.outputKind

            val stringValue = if (attrIndex >= 0) {
                input.getAttributeValue(attrIndex)
            } else {
                when (descOutputKind) {
                    OutputKind.Element -> { // This may occur with list values.
                        if (!config.isUnchecked) input.require(EventType.START_ELEMENT, serialName.namespaceURI, serialName.localPart)
                        input.readSimpleElement()
                    }

                    OutputKind.Attribute -> throw SerializationException(
                        "Attribute parsing without a concrete index is unsupported"
                    )

                    OutputKind.Inline -> throw SerializationException("Inline classes can not be decoded directly")

                    OutputKind.Mixed -> {
                        val t = input.allConsecutiveTextContent()
                        val d = preserveSpace.withDefault(xmlDescriptor.defaultPreserveSpace)
                        when {
                            d.withDefault(true) -> t
                            else -> xmlCollapseWhitespace(t)
                        }
                    }

                    OutputKind.Text -> when {
                        xmlDescriptor.defaultPreserveSpace.withDefault(true) -> input.allConsecutiveTextContent()

                        else -> input.allText()
                    }
                }
            }
            return when {
                defaultOverEmpty && stringValue.isEmpty() ->
                    when (val defaultString = (xmlDescriptor as? XmlValueDescriptor)?.default) {
                        null -> stringValue
                        else -> defaultString
                    }

                else -> stringValue
            }
        }


        @ExperimentalSerializationApi
        override fun decodeStringChunked(consumeChunk: (chunk: String) -> Unit) {

            if (attrIndex >= 0) {
                consumeChunksFromString(input.getAttributeValue(attrIndex), consumeChunk)
                return
            } else {
                when (xmlDescriptor.outputKind) {
                    OutputKind.Element -> {
                        if (!config.isUnchecked) input.require(EventType.START_ELEMENT, serialName.namespaceURI, serialName.localPart)
                        input.readSimpleElementChunked(consumeChunk)
                        return
                    }

                    OutputKind.Attribute -> throw SerializationException(
                        "Attribute parsing without a concrete index is unsupported"
                    )

                    OutputKind.Inline -> throw SerializationException("Inline classes can not be decoded directly")
                    OutputKind.Mixed -> return input.allConsecutiveTextContentChunked(consumeChunk)
                    OutputKind.Text -> return if (xmlDescriptor.defaultPreserveSpace.withDefault(true)) {
                        input.allConsecutiveTextContentChunked(consumeChunk)
                    } else {
                        input.allTextChunked(consumeChunk)
                    }

                }
            }
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            throw AssertionError("This should not happen as decodeSerializableValue should be called first")
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            val effectiveDeserializer: DeserializationStrategy<T> =
                xmlDescriptor.effectiveDeserializationStrategy(deserializer)
            /*
             * When the element is actually an inline we need to make sure to use the child descriptor (for the inline).
             * But only if decodeInline was called previously.
             */
            val desc = when {
                xmlDescriptor is XmlContextualDescriptor ->
                    xmlDescriptor.resolve(this, deserializer.descriptor)

                triggerInline && xmlDescriptor is XmlInlineDescriptor ->
                    xmlDescriptor.getElementDescriptor(0)

                else -> xmlDescriptor

            }
            val startsWithTag = input.eventType == EventType.START_ELEMENT
            val startDepth = input.depth
            val serialValueDecoder =
                SerialValueDecoder(effectiveDeserializer, desc, polyInfo, attrIndex, typeDiscriminatorName, isValueChild, preserveSpace)
            val value: T = effectiveDeserializer.deserializeSafe<T>(
                serialValueDecoder,
                isParseAllSiblings = isValueChild,
                isValueChild = isValueChild
            )
            if (startsWithTag && !input.hasPeekItems && input.depth < startDepth) {
                input.pushBackCurrent()
            }

            val tagId = serialValueDecoder.tagIdHolder?.tagId
            if (tagId != null) {
                checkNotNull(value) // only a non-null value can have an id
                if (_idMap.put(tagId, value) != null) throw XmlException("Duplicate use of id $tagId")
            }
            return value
        }

    }

    internal inner class StringDecoder(
        xmlDescriptor: XmlDescriptor,
        private val locationInfo: XmlReader.LocationInfo?,
        private val stringValue: String,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : Decoder, XML.XmlInput, DecodeCommons(xmlDescriptor, inheritedPreserveWhitespace) {
        // Decoding should not involve any threads. This type is internal and should not escape.
        // Initiating in a custom serializer is not supported
        override val input: XmlPeekingReader by lazy(LazyThreadSafetyMode.NONE) { PseudoBufferedReader(XmlStringReader(locationInfo, stringValue)) }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            throw UnsupportedOperationException("Strings cannot be decoded to structures")
        }

        @ExperimentalSerializationApi
        override fun decodeNotNullMark(): Boolean = true

        @ExperimentalSerializationApi
        override fun decodeInline(descriptor: SerialDescriptor): Decoder {
            return StringDecoder(xmlDescriptor.getElementDescriptor(0), locationInfo, stringValue, preserveSpace)
        }

        override fun decodeStringImpl(defaultOverEmpty: Boolean): String {
            val defaultString = (xmlDescriptor as? XmlValueDescriptor)?.default
            if (defaultOverEmpty && defaultString != null && stringValue.isEmpty()) return defaultString
            return stringValue
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            val effectiveDeserializationStrategy = xmlDescriptor.effectiveDeserializationStrategy(deserializer)
            return when (effectiveDeserializationStrategy) {
                is XmlDeserializationStrategy ->
                    effectiveDeserializationStrategy.deserializeXML(this, input)

                else -> effectiveDeserializationStrategy.deserialize(this)
            }
        }
    }

    /**
     * Implementation of XmlReader that only reads a string. This is provided to allow XmlSerializers
     * to work in cases such as an attribute list of QNames.
     */
    private inner class XmlStringReader(
        override val extLocationInfo: XmlReader.LocationInfo?,
        private val stringValue: String
    ) : XmlReader {
        private var pos = -1

        override val depth: Int get() = if (pos == 0) 0 else -1

        override fun hasNext(): Boolean = pos < 0

        override fun next(): EventType {
            if (pos >= 0) throw XmlSerialException("Reading beyond string")
            ++pos
            return EventType.TEXT
        }

        override val namespaceURI: Nothing get() = throw XmlSerialException("Strings have no namespace uri")

        override val localName: Nothing get() = throw XmlSerialException("Strings have no localname")

        override val prefix: Nothing get() = throw XmlSerialException("Strings have no prefix")

        override val isStarted: Boolean get() = pos >= 0

        override val isKnownEntity: Boolean get() = throw XmlSerialException("Entity references are not strings")

        override val text: String
            get() {
                if (pos != 0) throw XmlSerialException("Not in text position")
                return stringValue
            }

        override val piTarget: Nothing get() = throw XmlSerialException("Strings have no pi targets")

        override val piData: Nothing get() = throw XmlSerialException("Strings have no pi data")
        override val attributeCount: Nothing
            get() = throw XmlSerialException("Strings have no attributes")

        override fun getAttributeNamespace(index: Int): Nothing =
            throw XmlSerialException("Strings have no attributes")

        override fun getAttributePrefix(index: Int): Nothing =
            throw XmlSerialException("Strings have no attributes")

        override fun getAttributeLocalName(index: Int): Nothing =
            throw XmlSerialException("Strings have no attributes")

        override fun getAttributeValue(index: Int): Nothing =
            throw XmlSerialException("Strings have no attributes")

        override val eventType: EventType
            get() {
                if (pos != 0) throw XmlSerialException("Not in text position")
                return EventType.TEXT
            }

        override fun getAttributeValue(nsUri: String?, localName: String): Nothing {
            throw XmlSerialException("Strings have no attributes")
        }

        override fun getNamespacePrefix(namespaceUri: String): String? =
            input.getNamespacePrefix(namespaceUri)

        override fun close() {}

        override fun getNamespaceURI(prefix: String): String? =
            input.getNamespaceURI(prefix)

        override val namespaceDecls: List<Namespace>
            get() = input.namespaceDecls

        override val namespaceContext: IterableNamespaceContext
            get() = input.namespaceContext

        override val encoding: Nothing? get() = null

        override val standalone: Nothing? get() = null

        override val version: Nothing? get() = null
    }

    internal interface TagIdHolder {
        var tagId: String?
    }

    /**
     * Wrapper decoder that expects to be used to decode a *single* serial value
     */
    @OptIn(ExperimentalXmlUtilApi::class)
    private open inner class SerialValueDecoder(
        private val deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlDescriptor,
        polyInfo: PolyInfo?,
        attrIndex: Int,/* = -1*/
        override val typeDiscriminatorName: QName?,
        isValueChild: Boolean,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : XmlDecoder(xmlDescriptor, polyInfo, isValueChild, attrIndex, inheritedPreserveWhitespace) {
        private var notNullChecked = false

        /** Object that allows recording id attributes */
        var tagIdHolder: TagIdHolder? = null

        private val ignoredAttributes: MutableList<QName> = mutableListOf()

        fun ignoreAttribute(name: QName) {
            ignoredAttributes.add(name)
        }

        override fun decodeStringImpl(defaultOverEmpty: Boolean): String {
            val value = super.decodeStringImpl(defaultOverEmpty)
            if (attrIndex >= 0 && xmlDescriptor.isIdAttr) {
                tagIdHolder?.run { tagId = xmlCollapseWhitespace(value) }
            }
            return value
        }

        override fun decodeNotNullMark(): Boolean {
            notNullChecked = true
            return super.decodeNotNullMark()
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when {
            // This is needed to avoid loops with [kotlinx.serialization.internal.NullableSerializer]
            notNullChecked -> deserializer.deserializeSafe(this, isParseAllSiblings = isValueChild)
            else -> super.decodeSerializableValue(deserializer)
        }

        @ExperimentalSerializationApi
        override fun decodeInline(descriptor: SerialDescriptor): Decoder {
            // TODO is this valid
            tagIdHolder = object : TagIdHolder {
                override var tagId: String? = null
            }
            return super.decodeInline(descriptor)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            if (descriptor.isNullable) return TagDecoder(
                deserializer,
                xmlDescriptor,
                typeDiscriminatorName,
                preserveSpace
            ).also { tagIdHolder = it }

            return when {
                xmlDescriptor.kind is PrimitiveKind ->
                    throw AssertionError("A primitive is not a composite")

                xmlDescriptor is XmlPolymorphicDescriptor ->
                    PolymorphicDecoder(deserializer, xmlDescriptor, polyInfo, isValueChild, preserveSpace).also { tagIdHolder = it }

                xmlDescriptor is XmlListDescriptor -> when {
                    xmlDescriptor.outputKind == OutputKind.Attribute ->
                        AttributeListDecoder(
                            deserializer,
                            xmlDescriptor,
                            input.extLocationInfo,
                            attrIndex,
                            preserveSpace,
                        ).also { tagIdHolder = it }

                    xmlDescriptor.outputKind == OutputKind.Text -> ValueListDecoder(
                        deserializer,
                        xmlDescriptor,
                        input.extLocationInfo,
                        preserveSpace
                    )

                    xmlDescriptor.isListEluded ->
                        AnonymousListDecoder(
                            deserializer,
                            xmlDescriptor,
                            polyInfo,
                            typeDiscriminatorName,
                            isValueChild,
                            preserveSpace
                        ).also {
                            tagIdHolder = it
                        }

                    else -> NamedListDecoder(
                        deserializer,
                        xmlDescriptor,
                        typeDiscriminatorName,
                        preserveSpace
                    ).also { tagIdHolder = it }
                }

                xmlDescriptor is XmlMapDescriptor -> when {
                    xmlDescriptor.isListEluded ->
                        AnonymousMapDecoder(
                            deserializer,
                            xmlDescriptor,
                            polyInfo,
                            typeDiscriminatorName,
                            preserveSpace,
                        ).also { tagIdHolder = it }

                    else -> NamedMapDecoder(
                        deserializer,
                        xmlDescriptor,
                        polyInfo,
                        typeDiscriminatorName,
                        preserveSpace,
                    ).also { tagIdHolder = it }

                }

                else -> TagDecoder(deserializer, xmlDescriptor, typeDiscriminatorName, preserveSpace).also { tagIdHolder = it }
            }.also {
                for (attrName in ignoredAttributes) {
                    it.ignoreAttribute(attrName)
                }
            }
        }

    }

    /**
     * Special class that handles null values that are not mere primitives. Nice side-effect is that XmlDefault values
     * are actually parsed as XML and can be complex
     */
    private inner class NullDecoder(xmlDescriptor: XmlDescriptor, isValueChild: Boolean) :
        XmlDecoder(xmlDescriptor, isValueChild = isValueChild, inheritedPreserveWhitespace = DocumentPreserveSpace.DEFAULT), CompositeDecoder {

        override fun decodeNotNullMark() = (xmlDescriptor as? XmlValueDescriptor)?.default != null

        override fun decodeStringImpl(defaultOverEmpty: Boolean): String {
            if (isValueChild && (!defaultOverEmpty)) return ""
            val default = (xmlDescriptor as? XmlValueDescriptor)?.defaultValue(this@XmlDecoderBase, String.serializer())
            return default ?: ""
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            val default = (xmlDescriptor as? XmlValueDescriptor)?.defaultValue(this@XmlDecoderBase, deserializer)
            @Suppress("UNCHECKED_CAST")
            return default as T
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            val default = (xmlDescriptor as? XmlValueDescriptor)
                ?.defaultValue(this@XmlDecoderBase, deserializer) ?: previousValue

            @Suppress("UNCHECKED_CAST")
            return default as T
        }

        @ExperimentalSerializationApi
        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            previousValue: T?
        ): T? {
            return null
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return this
        }

        override fun endStructure(descriptor: SerialDescriptor) {}

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            when (descriptor.kind) {
                // Exception to allow for empty lists. They will read the index even if a 0 size was returned
                is StructureKind.MAP,
                is StructureKind.LIST -> return CompositeDecoder.DECODE_DONE

                else -> throw AssertionError("Null objects have no members")
            }
        }

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
            throw AssertionError("Null objects have no members")

        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
            throw AssertionError("Null objects have no members")

        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
            throw AssertionError("Null objects have no members")

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
            throw AssertionError("Null objects have no members")

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = 0

        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
            throw AssertionError("Null objects have no members")

        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
            throw AssertionError("Null objects have no members")

        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
            throw AssertionError("Null objects have no members")

        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
            throw AssertionError("Null objects have no members")

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
            throw AssertionError("Null objects have no members")

        @ExperimentalSerializationApi
        override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
            throw AssertionError("Null objects have no members")
        }
    }

    internal inner class TagDecoder<D : XmlDescriptor>(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: D,
        typeDiscriminatorName: QName?,
        preserveWhitespace: DocumentPreserveSpace,
    ) : TagDecoderBase<D>(deserializer, xmlDescriptor, typeDiscriminatorName, preserveWhitespace) {

        private val readTagName = if(config.isUnchecked) xmlDescriptor.tagName else input.name

        override fun endStructure(descriptor: SerialDescriptor) {
            // If we aren't in the closed stage, we read the index again to check that we are finished
            // Don't do the check for custom strategies
            if (stage < STAGE_CLOSE && deserializer !is XmlDeserializationStrategy) {
                stage = STAGE_NULLS
                val index = decodeElementIndex()
                if (true && index != CompositeDecoder.DECODE_DONE) throw XmlSerialException(
                    "Unexpected content in end structure: ${
                        xmlDescriptor.friendlyChildName(index)
                    }"
                )
            }
            check(input.depth == tagDepth) { "Unexpected tag depth: ${input.depth} (expected: ${tagDepth})" }
            if (!config.isUnchecked) input.require(EventType.END_ELEMENT, readTagName)
        }

    }

    @OptIn(ExperimentalXmlUtilApi::class)
    internal abstract inner class TagDecoderBase<D : XmlDescriptor>(
        protected val deserializer: DeserializationStrategy<*>,
        xmlDescriptor: D,
        protected val typeDiscriminatorName: QName?,
        preserveWhitespace: DocumentPreserveSpace,
    ) : XmlTagCodec<D>(xmlDescriptor), CompositeDecoder, XML.XmlInput, TagIdHolder {

        override var tagId: String? = null
        private val ignoredAttributes: MutableList<QName> = ArrayList(2)

        /**
         * Determine whether whitespace is preserved in the content of the tag. This is a var as it
         * may be changed by an xml:space attribute.
         */
        protected var preserveWhitespace = preserveWhitespace
            private set

        protected val attrCount: Int = if (input.eventType == EventType.START_ELEMENT) input.attributeCount else 0
        protected val tagDepth: Int = input.depth

        /**
         * Array that records for each child element whether it has been encountered. After processing the entire tag
         * this array will allow for the remaining tags to be handled as null (or defaulted/optional)
         */
        private val seenItems = BooleanArray(xmlDescriptor.elementsCount)

        private var nulledItemsIdx = -1

        // We need to do this here so we can move up when reading (allowing for repeated queries for nullable values)
        protected var lastAttrIndex: Int = -1
            private set

        protected var currentPolyInfo: PolyInfo? = null

        private val otherAttrIndex: Int = xmlDescriptor.getAttrMap()

        @OptIn(ExperimentalXmlUtilApi::class)
        protected var pendingRecovery = ArrayDeque<XML.ParsedData<*>>()

        protected var stage = STAGE_INIT

        private val tagNameToMembers: QNameMap<Int>
        private val attrNameToMembers: QNameMap<Int>
        private val polyChildren: QNameMap<PolyInfo> = xmlDescriptor.polyMap
        private val contextualDescriptors: Array<XmlDescriptor?>

        init {
            if (xmlDescriptor.contextualChildren.isNotEmpty()) {
                val tagNameMap: QNameMap<Int> = xmlDescriptor.tagNameMap.copyOf()
                val attrNameMap: QNameMap<Int> = xmlDescriptor.attrMap.copyOf()
                val contextList = arrayOfNulls<XmlDescriptor>(xmlDescriptor.elementsCount)

                for (idx in xmlDescriptor.contextualChildren) {
                    val child = xmlDescriptor.getElementDescriptor(idx) as XmlContextualDescriptor
                    // This uses a "workaround" to find the actual deserializer.
                    val childSer = deserializer.findChildSerializer(idx, serializersModule)
                    val resolved = child.resolve(this, childSer.descriptor)
                    val childName = resolved.tagName.normalize()
                    when (resolved.effectiveOutputKind) {
                        OutputKind.Attribute -> {
                            check(attrNameMap.put(childName, idx) == null || config.isUnchecked) {
                                "Duplicate attribute name $childName as contextual child in ${xmlDescriptor.serialDescriptor.serialName}"
                            }
                        }

                        else -> {
                            check(
                                (tagNameMap.put(childName, idx) == null || config.isUnchecked)
                                        && (config.isUnchecked || childName !in polyChildren)
                            ) {
                                "Duplicate tag name $childName as contextual child in ${xmlDescriptor.serialDescriptor.serialName}"
                            }
                        }
                    }
                }

                tagNameToMembers = tagNameMap
                attrNameToMembers = attrNameMap
                contextualDescriptors = contextList

            } else {
                tagNameToMembers = xmlDescriptor.tagNameMap
                attrNameToMembers = xmlDescriptor.attrMap
                contextualDescriptors = arrayOfNulls(xmlDescriptor.elementsCount)
            }
        }

        final override val input: XmlPeekingReader get() = this@XmlDecoderBase.input

        override val namespaceContext: NamespaceContext get() = input.namespaceContext

        protected open fun <T> serialElementDecoder(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
        ): XmlDecoder? {
            if (nulledItemsIdx >= 0) return null

            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)

            // The deserializer is provided as fallback/default
            val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)

            val isValueChild = index == xmlDescriptor.getValueChild()
            return when (effectiveDeserializer.descriptor.kind) {
                is PrimitiveKind ->
                    XmlDecoder(childXmlDescriptor, currentPolyInfo, isValueChild, lastAttrIndex, preserveWhitespace)

                else -> {
                    SerialValueDecoder(
                        effectiveDeserializer,
                        childXmlDescriptor,
                        currentPolyInfo,
                        lastAttrIndex,
                        null,
                        isValueChild,
                        preserveWhitespace,
                    )
                }
            }
        }

        // TODO: Rewrite this to no longer rely on collection merging, rather keep list elements internal
        @OptIn(InternalSerializationApi::class)
        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            @Suppress("UNCHECKED_CAST")
            handleRecovery<Any?>(index) { return it as T }
            require(stage < STAGE_CLOSE) { "Reading content in end state" }

            val initialChildXmlDescriptor = xmlDescriptor.getElementDescriptor(index)

            val effectiveDeserializer = initialChildXmlDescriptor.effectiveDeserializationStrategy(deserializer)

            val childXmlDescriptor = when {
                effectiveDeserializer == deserializer -> initialChildXmlDescriptor
                else -> initialChildXmlDescriptor.overrideDescriptor(this, effectiveDeserializer.descriptor)
            }


            val isValueChild = xmlDescriptor.getValueChild() == index

            val decoder = when {
                stage == STAGE_NULLS -> NullDecoder(childXmlDescriptor, isValueChild)

                lastAttrIndex >= 0 && childXmlDescriptor is XmlAttributeMapDescriptor -> {
                    AttributeMapDecoder(effectiveDeserializer, childXmlDescriptor, lastAttrIndex, preserveWhitespace)
                }

                // handle empty value children separately
                isValueChild && input.hasPeekItems && input.peekNextEvent() == EventType.END_ELEMENT ->
                    StringDecoder(childXmlDescriptor, input.extLocationInfo, "", preserveWhitespace)

                else -> {
                    serialElementDecoder(descriptor, index, effectiveDeserializer)
                        ?: NullDecoder(childXmlDescriptor, isValueChild)
                }
            }

            val effectiveInput: XmlPeekingReader = when (decoder) {
                is NullDecoder -> decoder.input
                is StringDecoder -> decoder.input
                else -> input
            }

            val result: T = when (effectiveDeserializer) {
                is XmlDeserializationStrategy ->
                    effectiveDeserializer.deserializeXML(decoder, effectiveInput, previousValue, isValueChild)

                is AbstractCollectionSerializer<*, T, *> ->
                    effectiveDeserializer.merge(decoder, previousValue)

                else -> try {
                    // For value children ignore whitespace content
                    if (isValueChild && !input.hasPeekItems && input.eventType == EventType.IGNORABLE_WHITESPACE) {
                        input.next()
                    }
                    effectiveDeserializer.deserialize(decoder)
                } catch (e: XmlException) {
                    throw e
                } catch (e: Exception) {
                    if (input.hasPeekItems) input.next()
                    throw XmlException(
                        "In: ${xmlDescriptor.tagName}/${descriptor.getElementName(index)} Error: ${input.extLocationInfo} - ${e.message}",
                        input.extLocationInfo,
                        e
                    )
                }
            }

            if (stage == STAGE_CONTENT) {
                if (!input.hasPeekItems) {
                    if (isValueChild && input.eventType == EventType.END_ELEMENT && input.depth == tagDepth) {
                        input.pushBackCurrent() // unread the end of the containing element tag
                    }
                } else { //has peek items
                    if (input.peekNextEvent() == EventType.END_ELEMENT && input.depth > tagDepth + 1) {
                        input.next() // consume peeked event if needed
                    }
                }
            }

            val tagId = (decoder as? SerialValueDecoder)?.tagIdHolder?.tagId
            if (tagId != null) {
                checkNotNull(result) // only a non-null value can have an id
                if (_idMap.put(tagId, result) != null) throw XmlException("Duplicate use of id $tagId")
            }

            seenItems[index] = true
            return result
        }

        @OptIn(InternalSerializationApi::class)
        @ExperimentalSerializationApi
        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            previousValue: T?
        ): T? {
            @Suppress("UNCHECKED_CAST")
            handleRecovery<Any?>(index) { return it as T }

            if (stage == STAGE_NULLS) {
                return null
            }

            if (hasNullMark()) { // process the element
                if (input.nextTag() != EventType.END_ELEMENT)
                    throw SerializationException("Elements with nil tags may not have content")
                return null
            }


            val decoder = serialElementDecoder(descriptor, index, deserializer) ?: return null

            val effectiveDeserializer = xmlDescriptor
                .getElementDescriptor(index)
                .effectiveDeserializationStrategy(deserializer)

            val isValueChild = xmlDescriptor.getValueChild() == index
            // TODO make merging more reliable
            val result: T? = when (effectiveDeserializer) {
                is XmlDeserializationStrategy -> effectiveDeserializer.deserializeSafe(decoder, isValueChild, previousValue, isValueChild)

                is AbstractCollectionSerializer<*, T?, *> ->
                    effectiveDeserializer.merge(decoder, previousValue)

                else -> effectiveDeserializer.deserialize(decoder)
            }

            val tagId = (decoder as? SerialValueDecoder)?.tagIdHolder?.tagId
            if (tagId != null) {
                checkNotNull(result) // only a non-null value can have an id
                if (_idMap.put(tagId, result) != null) throw XmlException("Duplicate use of id $tagId")
            }

            seenItems[index] = true
            return result
        }

        @ExperimentalSerializationApi
        override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
            handleRecovery<Any?>(index) { return DummyDecoder(it) }

            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)
            val isValueChild = index == xmlDescriptor.getValueChild()
            return when (descriptor.kind) {
                is PrimitiveKind -> XmlDecoder(childXmlDescriptor, currentPolyInfo, isValueChild, lastAttrIndex, preserveWhitespace)

                else -> {
                    SerialValueDecoder(
                        deserializer,
                        childXmlDescriptor,
                        currentPolyInfo,
                        lastAttrIndex,
                        typeDiscriminatorName,
                        isValueChild,
                        preserveWhitespace,
                    )
                }
            }
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        fun indexOf(namespace: String, localName: String, inputType: InputKind): Int {

            val isNameOfAttr = inputType == InputKind.Attribute

            currentPolyInfo = null

            val polyMap = polyChildren
            val tagNameMap = tagNameToMembers
            val attrNameMap = attrNameToMembers

            val nameMap = if (isNameOfAttr) attrNameMap else tagNameMap

            // If it is a known (non-polymorphic) child just return the index of the value
            nameMap[namespace, localName]?.let { return it.checkRepeatAndOrder(inputType) }

            // Or if a tag check that hit is known as a polymorphic value
            if (!isNameOfAttr) {
                polyMap[namespace, localName]?.let {
                    return it.index.checkRepeatAndOrder(inputType).apply {
                        currentPolyInfo = it
                    }
                }
            }

            val containingNamespaceUri = serialName.namespaceURI
            // Allow attributes in the null namespace to match candidates with a name that is that of the parent tag
            if (isNameOfAttr && !config.policy.isStrictAttributeNames) {
                if (namespace.isEmpty()) {
                    nameMap[containingNamespaceUri, localName]?.let { return it.checkRepeat() }
                }
            }

            // If the parent namespace uri is the same as the namespace uri of the element, try looking for an element
            // with a null namespace instead
            if (!config.policy.isStrictAttributeNames && containingNamespaceUri.isNotEmpty() && containingNamespaceUri == namespace) {
                nameMap["", localName]?.let { return it.checkRepeatAndOrder(inputType) }
            }

            // TODO check that the attr count test should not be needed
            if (inputType == InputKind.Attribute && lastAttrIndex in 0 until attrCount) {
                val other = otherAttrIndex
                if (other >= 0) return other
            } else {
                val vc = xmlDescriptor.getValueChild()
                if (vc >= 0) { // only map to a value child for a polymorphic child if it is actually matche
                    var vcdesc = xmlDescriptor.getElementDescriptor(vc)
                    while (vcdesc is XmlListDescriptor && vcdesc.isListEluded) vcdesc = vcdesc.getElementDescriptor(0)
                    if (vcdesc !is XmlPolymorphicDescriptor ||
                        ! vcdesc.isTransparent ||
                        vcdesc.kind == PolymorphicKind.SEALED || // sealed classes have no base class
                        vcdesc.polyMap[namespace, localName] != null) {
                        return vc.checkRepeat()
                    }
                    val m = vcdesc.resolvePolymorphicTypeNameCandidates(input.name, serializersModule)
                    if (m.size == 1) {
                        return vc.checkRepeat()
                    }
                    val baseClass = vcdesc.serialDescriptor.capturedKClass
                    if (baseClass != null) {
                        val defaultSerializer = serializersModule.getPolymorphic(baseClass, localName)
                        if (defaultSerializer != null) {
                            return vc.checkRepeat()
                        }
                    }
                }
            }

            // Hook that will normally throw an exception on an unknown name.
            config.policy.handleUnknownContentRecovering(
                input,
                inputType,
                xmlDescriptor,
                QName(namespace, localName),
                (attrNameMap.map { (k, v) ->
                    PolyInfo(k, v, xmlDescriptor.getElementDescriptor(v))
                } + tagNameMap.map { (k, v) ->
                    PolyInfo(k, v, xmlDescriptor.getElementDescriptor(v))
                } + polyMap.values)
            ).let {
                val singleParsed = it.singleOrNull()
                if (singleParsed?.unParsed == true) { // Support index only returns
                    return singleParsed.elementIndex
                }
                pendingRecovery.addAll(it)
            }

            return CompositeDecoder.UNKNOWN_NAME // Special value to indicate the element is unknown (but possibly ignored)
        }

        protected open fun Int.checkRepeat(): Int = also { idx ->
            if (idx >= 0 && seenItems[idx]) {
                val desc = xmlDescriptor.getElementDescriptor(idx)
                if (desc !is XmlListLikeDescriptor || !desc.isListEluded) {
                    config.policy.onElementRepeated(xmlDescriptor, idx)
                }
            }
        }

        protected open fun Int.checkRepeatAndOrder(inputType: InputKind): Int = also { idx ->
            if (!config.isUnchecked) {
                checkRepeat()
                if (config.policy.verifyElementOrder && inputType == InputKind.Element) {
                    if (xmlDescriptor is XmlCompositeDescriptor) {
                        // TODO optimize by caching ordering.
                        val constraints = xmlDescriptor.childConstraints
                        if (constraints != null) {
                            for (childIdx in seenItems.indices) {
                                if (seenItems[childIdx]) {
                                    if (constraints.isOrderedAfter(childIdx, this)) {
                                        throw XmlSerialException(
                                            "In ${xmlDescriptor.tagName}, found element ${
                                                xmlDescriptor.friendlyChildName(childIdx)
                                            } before ${xmlDescriptor.friendlyChildName(idx)} in conflict with ordering constraints"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private inline fun Int.markSeenOrHandleUnknown(body: () -> Int): Int {
            return when (this) {
                CompositeDecoder.UNKNOWN_NAME -> body()
                else -> {
                    seenItems[this] = true
                    this
                }
            }
        }

        /**
         * Rather than descriptor the code should use the xmlDescriptor
         */
        final override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return decodeElementIndex()
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        protected open fun decodeElementIndex(): Int {
            if (stage == STAGE_CLOSE) {
                return CompositeDecoder.DECODE_DONE
            }

            /* Decoding works in 4 stages: pending injected elements, attributes,
             * child content, null values.
             * Pending injected elements allow for handling unexpected content, and should be
             * handled first.
             * Attribute decoding is finished when attrIndex<0
             * child content is finished when nulledItemsIdx >=0
             * Fully finished when the nulled items returns a DONE value
             */
            if (pendingRecovery.isNotEmpty()) {
                return pendingRecovery.first().elementIndex
            }
            if (stage == STAGE_NULLS) {
                nextNulledItemsIdx()
                if (stage == STAGE_NULLS) { // still reading nulls
                    // This processes all "missing" elements.
                    if (!config.isUnchecked && !input.hasPeekItems) input.require(EventType.END_ELEMENT, xmlDescriptor.tagName)

                    if (nulledItemsIdx >= seenItems.size) {
                        stage = STAGE_CLOSE
                        return CompositeDecoder.DECODE_DONE
                    }

                    return nulledItemsIdx
                }
            }

            if (stage <= STAGE_ATTRS) {
                // Move to next attribute. Continuing to increase is harmless (given less than 2^31 children)
                lastAttrIndex++

                // Allow for ignoring attributes (like keys on collapsed maps).
                // This must be separate as it may be the last attribute that is ignored
                while (lastAttrIndex in 0 until attrCount &&
                    ignoredAttributes.any { it isEquivalent input.getAttributeName(lastAttrIndex) }
                ) {
                    ++lastAttrIndex
                }

                if (lastAttrIndex in 0 until attrCount) {

                    val attrIdx = lastAttrIndex
                    val attrNS = input.getAttributeNamespace(attrIdx)
                    val attrPrefix = input.getAttributePrefix(attrIdx)
                    val attrLName = input.getAttributeLocalName(attrIdx)

                    if (attrNS == XMLNS_ATTRIBUTE_NS_URI ||
                        attrPrefix == XMLNS_ATTRIBUTE ||
                        (attrPrefix.isEmpty() && attrLName == XMLNS_ATTRIBUTE) ||
                        typeDiscriminatorName.let {
                            it != null && attrNS == it.namespaceURI && attrLName == it.localPart
                        }
                    ) {
                        // Ignore namespace decls and the type discriminator attribute, just recursively call the function itself
                        return decodeElementIndex()
                    } else if (attrNS == XML_NS_URI && attrLName == "space") {
                        when (input.getAttributeValue(attrIdx)) {
                            "preserve" -> preserveWhitespace = DocumentPreserveSpace.DOCUMENT_PRESERVE
                            "default" -> preserveWhitespace = DocumentPreserveSpace.DEFAULT
                        }
                        // If this was explicitly declared as attribute use that as index, otherwise
                        // just skip the attribute.
                        return attrNameToMembers[QName(attrNS, attrLName)]?.also { seenItems[it] = true }
                            ?: decodeElementIndex()
                    }

                    // The ifNegative function will recursively call this function if we didn't find it (and the handler
                    // didn't throw an exception). This allows for ignoring unknown elements.
                    return indexOf(
                        attrNS,
                        attrLName,
                        InputKind.Attribute
                    ).markSeenOrHandleUnknown { decodeElementIndex() }

                }
                stage = STAGE_CONTENT // no more attributes
                lastAttrIndex = Int.MIN_VALUE // Ensure to reset here, this should not practically get bigger than 0
            }

            if (stage <= STAGE_CONTENT) {
                stage = STAGE_CONTENT
                val valueChild = xmlDescriptor.getValueChild()
                // Handle the case of an empty tag for a value child. This is not a nullable item (so shouldn't be
                // treated as such).
                if (valueChild >= 0 && !seenItems[valueChild]) {
                    val valueChildDesc = xmlDescriptor.getElementDescriptor(valueChild)
                    // Lists/maps need to be empty (treated as null/missing)
                    if ((!valueChildDesc.isNullable) && valueChildDesc.kind !is StructureKind.LIST && valueChildDesc.kind !is StructureKind.MAP) {
                        // This code can rely on seenItems to avoid infinite item loops as it only triggers on an empty tag.
                        seenItems[valueChild] = true

                        when (input.next()) {
                            EventType.END_ELEMENT -> {
                                // empty value
                                input.pushBackCurrent()
                            }
                            EventType.CDSECT,
                            EventType.IGNORABLE_WHITESPACE,
                            EventType.TEXT -> currentPolyInfo = polyChildren["", "kotlin.String"]
                            else -> {}
                        }

                        return valueChild
                    }
                }
                for (eventType in input) {
                    when (eventType) {
                        EventType.END_ELEMENT -> {
                            stage = STAGE_NULLS
                            return decodeElementIndex()
                        }

                        EventType.START_DOCUMENT,
                        EventType.COMMENT,
                        EventType.DOCDECL,
                        EventType.PROCESSING_INSTRUCTION -> {
                        } // do nothing/ignore

                        EventType.CDSECT -> { // cdata is never whitespace
                            // The android reader doesn't check whitespaceness. This code should throw
                            return valueChild.markSeenOrHandleUnknown {
                                config.policy.handleUnknownContentRecovering(
                                    input,
                                    InputKind.Text,
                                    xmlDescriptor,
                                    QName("<CDATA>"),
                                    emptyList()
                                ).let { pendingRecovery.addAll(it) }
                                decodeElementIndex() // if this doesn't throw, recursively continue
                            }
                        }
                        EventType.ENTITY_REF,
                        EventType.IGNORABLE_WHITESPACE,
                        EventType.TEXT -> {
                            // The android reader doesn't check whitespaceness. This code should throw
                            if (input.isWhitespace()) {
                                if (valueChild != CompositeDecoder.UNKNOWN_NAME) {
                                    var valueDesc = xmlDescriptor.getElementDescriptor(valueChild)
                                    while (valueDesc is XmlListDescriptor && valueDesc.isListEluded) {
                                        valueDesc = valueDesc.getElementDescriptor(0)
                                    }
                                    val actualPreserveWS = preserveWhitespace.withDefault(valueDesc.defaultPreserveSpace)

                                    if (actualPreserveWS.withDefault(true)) {
                                        if (valueDesc.defaultPreserveSpace.withDefault(true)) { // if the type is not explicitly marked to ignore whitespace
                                            if (valueDesc.outputKind.isTextOrMixed) { // this allows all primitives (
                                                seenItems[valueChild] = true
                                                currentPolyInfo = polyChildren["", "kotlin.String"]
                                                return valueChild // We can handle whitespace
                                            }
                                        }
                                    }
                                }
                            } else if (!input.isWhitespace()) {
                                currentPolyInfo = polyChildren["", "kotlin.String"]
                                return valueChild.markSeenOrHandleUnknown {
                                    config.policy.handleUnknownContentRecovering(
                                        input,
                                        InputKind.Text,
                                        xmlDescriptor,
                                        QName("<CDATA>"),
                                        emptyList()
                                    ).let { pendingRecovery.addAll(it) }
                                    decodeElementIndex() // if this doesn't throw, recursively continue
                                }
                            }
                        }

                        EventType.ATTRIBUTE -> return indexOf(
                            input.namespaceURI,
                            input.localName,
                            InputKind.Attribute
                        ).markSeenOrHandleUnknown { decodeElementIndex() }

                        EventType.START_ELEMENT -> {
                            val startPos = input.extLocationInfo
                            when (val i = indexOf(input.namespaceURI, input.localName, InputKind.Element)) {
                                // Note that the recovery must consume the element.
                                CompositeDecoder.UNKNOWN_NAME -> {
                                    if (pendingRecovery.isNotEmpty()) {
                                        return pendingRecovery.first().elementIndex
                                    }
                                    // Add a special check to handle recovery that doesn't consume the tag
                                    // check the event type as a self-closing tag has the same end position.
                                    if (input.eventType == EventType.START_ELEMENT && startPos != null && startPos == input.extLocationInfo) {
                                        input.elementContentToFragment()
                                    }
                                }

                                else -> return i.also { seenItems[i] = true }
                            }
                        }

                        EventType.END_DOCUMENT -> throw XmlSerialException("End document in unexpected location")
                    }
                }
            }
            return CompositeDecoder.DECODE_DONE
        }

        private fun nextNulledItemsIdx() {
            val seenItems = seenItems
            for (i in (nulledItemsIdx + 1) until seenItems.size) {
                // Items that have been seen don't need to be passed as null
                // Items that are declared as optional, don't need to be passed as ull
                if (!(seenItems[i] || xmlDescriptor.isElementOptional(i))) {
                    val childDesc = xmlDescriptor.getElementDescriptor(i)

                    val default = (childDesc as? XmlValueDescriptor)?.default

                    val defaultOrList = when {
                        i == xmlDescriptor.getValueChild() -> true
                        default != null -> true
                        else -> {

                            // If there is no child descriptor and it is missing this can only be because it is
                            // either a list or nullable. It can be treated as such.
                            when (childDesc.kind) {
                                StructureKind.LIST,
                                StructureKind.MAP -> true

                                else -> childDesc.isNullable
                            }
                        }
                    }
                    if (defaultOrList) {
                        nulledItemsIdx = i
                        return
                    }
                }
            }
            stage = STAGE_CLOSE // finished processing nulls
            nulledItemsIdx = seenItems.size
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (stage < STAGE_CLOSE) {
                val index = decodeElementIndex()
                if (index != CompositeDecoder.DECODE_DONE) throw XmlSerialException("Unexpected content in end structure")
            }
            if (!config.isUnchecked) {
                if (typeDiscriminatorName == null) {
                    input.require(EventType.END_ELEMENT, serialName)
                } else { // if there is a type discriminator we have an inherited name
                    input.require(EventType.END_ELEMENT, null)
                }
            }
        }

        open fun readElementEnd(): Int {
            // We are now in the null/default reading stage
            stage = STAGE_NULLS
            // this is triggered on endTag, so we increase the null index to be 0 or higher.
            // We use this function as we still need to do the skipping of the relevant children
            nextNulledItemsIdx()

            return when {
                nulledItemsIdx < seenItems.size -> nulledItemsIdx
                else -> CompositeDecoder.DECODE_DONE
            }
        }

        open fun doReadAttribute(lastAttrIndex: Int): String {
            return input.getAttributeValue(this.lastAttrIndex)
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        private inline fun <reified T> handleRecovery(index: Int, onSuccess: (T) -> Unit) {
            if (pendingRecovery.isNotEmpty()) {
                val d = pendingRecovery.removeFirst()
                if (d.elementIndex != index) {
                    throw IllegalStateException("Recovery state is inconsistent")
                }
                onSuccess(d.value as T)
            }
        }

        fun decodeStringElementCollapsed(descriptor: SerialDescriptor, index: Int): String {
            return xmlCollapseWhitespace(decodeStringElement(descriptor, index))
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            handleRecovery<String>(index) { return it }

            val childDesc = xmlDescriptor.getElementDescriptor(index)


            seenItems[index] = true
            val isAttribute = lastAttrIndex >= 0
            if (isAttribute) {
                val a = doReadAttribute(lastAttrIndex)
                if (xmlDescriptor.getElementDescriptor(index).isIdAttr) {
                    tagId = xmlCollapseWhitespace(a)
                }
                return a
            } else if (stage == STAGE_NULLS) { // Now reading nulls
                val default = (childDesc as? XmlValueDescriptor)?.default
                return when {
                    default != null -> default
                    index == xmlDescriptor.getValueChild() -> ""
                    else -> throw XmlSerialException("Missing child ${descriptor.getElementName(index)}:$index")
                }
            }

            return when (childDesc.outputKind) {
                OutputKind.Inline -> throw XmlSerialException("Inline elements can not be directly decoded")

                OutputKind.Element -> input.readSimpleElement()

                OutputKind.Mixed,
                OutputKind.Text -> when {
                    xmlDescriptor.defaultPreserveSpace.withDefault(true) -> input.allConsecutiveTextContent()
                    else -> input.allText()
                }.also { // add some checks that we only have text content
                    val peek = input.peekNextEvent()
                    if (peek != EventType.END_ELEMENT) {
                        throw XmlSerialException("Missing end tag after text only content (found: ${peek})")
                    } /*else if (peek.localName != serialName.localPart) {
                            throw XmlSerialException("Expected end tag local name ${serialName.localPart}, found ${peek.localName}")
                        }*/
                }


                OutputKind.Attribute -> error("Attributes should already be read now")
            }
        }

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = handleParseError {
            return decodeStringElementCollapsed(descriptor, index).toInt()
        }

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
            val startPos = input.extLocationInfo
            val stringValue = decodeStringElementCollapsed(descriptor, index)
            return when {
                config.policy.isStrictBoolean -> XmlBooleanSerializer.deserialize(
                    StringDecoder(xmlDescriptor.getElementDescriptor(index), startPos, stringValue, preserveWhitespace)
                )

                else -> stringValue.toBoolean()
            }
        }

        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = handleParseError {
            return decodeStringElementCollapsed(descriptor, index).toByte()
        }

        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = handleParseError {
            return decodeStringElementCollapsed(descriptor, index).toShort()
        }

        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = handleParseError {
            return decodeStringElementCollapsed(descriptor, index).toLong()
        }

        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = handleParseError {
            return decodeStringElementCollapsed(descriptor, index).toFloat()
        }

        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = handleParseError {
            return decodeStringElementCollapsed(descriptor, index).toDouble()
        }

        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = handleParseError {
            return decodeStringElementCollapsed(descriptor, index).single()
        }

        fun ignoreAttribute(attrName: QName) {
            ignoredAttributes.add(attrName)
        }

    }

    internal inner class AttributeMapDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlAttributeMapDescriptor,
        private val attrIndex: Int,
        inheritedPreserveWhitespace: DocumentPreserveSpace
    ) : TagDecoderBase<XmlAttributeMapDescriptor>(deserializer, xmlDescriptor, null, inheritedPreserveWhitespace), Decoder {

        private var correctStartIndex = -1
        private var nextIndex: Int = 0

        @ExperimentalSerializationApi
        override fun decodeSequentially(): Boolean = true

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = 1

        override fun decodeElementIndex(): Int = when (nextIndex) {
            0, 1 -> nextIndex++
            else -> CompositeDecoder.DECODE_DONE
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            if (correctStartIndex < 0) correctStartIndex = index
            val fixedIndex = (index - correctStartIndex) % 2

            val effectiveDeserializer = xmlDescriptor
                .getElementDescriptor(fixedIndex)
                .effectiveDeserializationStrategy(deserializer)

            if (fixedIndex == 0 && effectiveDeserializer == QNameSerializer) {
                @Suppress("UNCHECKED_CAST")
                return input.getAttributeName(attrIndex) as T
            }

            val startPos = input.extLocationInfo
            val value = decodeStringElement(descriptor, index)

            val decoder = StringDecoder(xmlDescriptor.valueDescriptor, startPos, value, preserveWhitespace)
            return when (effectiveDeserializer) {
                is XmlDeserializationStrategy ->
                    effectiveDeserializer.deserializeXML(decoder, XmlStringReader(startPos, value))

                else -> effectiveDeserializer.deserialize(decoder)
            }
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = when (index % 2) {
            0 -> {
                val name = input.getAttributeName(attrIndex)
                if (name.prefix.isEmpty() || name.namespaceURI.isEmpty()) {
                    name.localPart
                } else {
                    throw XmlSerialException("A QName in a namespace cannot be converted to a string")
                }
            }

            else -> xmlCollapseWhitespace(input.getAttributeValue(attrIndex))
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // do nothing
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this

        override fun decodeBoolean(): Boolean = throw UnsupportedOperationException("Expect map structure")

        override fun decodeByte(): Byte = throw UnsupportedOperationException("Expect map structure")

        override fun decodeChar(): Char = throw UnsupportedOperationException("Expect map structure")

        override fun decodeDouble(): Double = throw UnsupportedOperationException("Expect map structure")

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
            throw UnsupportedOperationException("Expect map structure")

        override fun decodeFloat(): Float = throw UnsupportedOperationException("Expect map structure")

        @ExperimentalSerializationApi
        override fun decodeInline(descriptor: SerialDescriptor): Decoder {
            return this
        }

        override fun decodeInt(): Int = throw UnsupportedOperationException("Expect map structure")

        override fun decodeLong(): Long = throw UnsupportedOperationException("Expect map structure")

        @ExperimentalSerializationApi
        override fun decodeNotNullMark(): Boolean = throw UnsupportedOperationException("Expect map structure")

        @ExperimentalSerializationApi
        override fun decodeNull(): Nothing = throw UnsupportedOperationException("Expect map structure")

        override fun decodeShort(): Short = throw UnsupportedOperationException("Expect map structure")

        override fun decodeString(): String = throw UnsupportedOperationException("Expect map structure")
    }

    internal abstract inner class TextualListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        private val locationInfo: XmlReader.LocationInfo?,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : TagDecoderBase<XmlListDescriptor>(deserializer, xmlDescriptor, null, inheritedPreserveWhitespace) {
        private var listIndex = 0

        // This decoder is only used from a single tread and should not "escape"
        private val textValues by lazy(LazyThreadSafetyMode.NONE) {
            xmlCollapseWhitespace(getTextValue()).split(*xmlDescriptor.delimiters)
        }

        abstract fun getTextValue(): String

        @ExperimentalSerializationApi
        override fun decodeSequentially(): Boolean = true

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
            return textValues.size
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            val decoder =
                StringDecoder(xmlDescriptor.getElementDescriptor(index), locationInfo, textValues[listIndex++], preserveWhitespace)
            return decoder.decodeSerializableValue(deserializer)
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            return textValues[listIndex++]
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Do nothing
        }
    }

    internal inner class AttributeListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        locationInfo: XmlReader.LocationInfo?,
        private val attrIndex: Int,
        inheritedPreserveWhitespace: DocumentPreserveSpace
    ) : TextualListDecoder(deserializer, xmlDescriptor, locationInfo, inheritedPreserveWhitespace) {

        override fun getTextValue(): String = input.getAttributeValue(attrIndex)
    }

    internal inner class ValueListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        locationInfo: XmlReader.LocationInfo?,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : TextualListDecoder(deserializer, xmlDescriptor, locationInfo, inheritedPreserveWhitespace) {

        override fun getTextValue(): String = input.text
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private inner class AnonymousListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        private val polyInfo: PolyInfo?,
        typeDiscriminatorName: QName?,
        private val isValueChild: Boolean,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : TagDecoderBase<XmlListDescriptor>(deserializer, xmlDescriptor, typeDiscriminatorName, inheritedPreserveWhitespace) {

        private val parentXmlDescriptor: XmlDescriptor get() = xmlDescriptor.tagParent.descriptor as XmlDescriptor

        private var finished: Boolean = false

        override fun decodeElementIndex(): Int = when {
            finished -> CompositeDecoder.DECODE_DONE

            // lists are always decoded as single element lists
            else -> 0.also { finished = true }
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {

            // This is an anonymous list decoder. The descriptor passed here is for a list, not the xml parent element.

            // Note that the child descriptor a list is always at index 0
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(0)

            val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)

            if (isValueChild && (effectiveDeserializer is CompactFragmentSerializer)) {

                return input.elementToFragment().let {
                    @Suppress("UNCHECKED_CAST")
                    it as T
                }
            }

            val decoder = SerialValueDecoder(
                effectiveDeserializer,
                childXmlDescriptor,
                polyInfo,
                Int.MIN_VALUE,
                typeDiscriminatorName,
                isValueChild,
                preserveWhitespace,
            )

            /* On a list we never parse all siblings */
            val result = deserializer.deserializeSafe(decoder, false, previousValue, false && isValueChild)

            val tagId = (decoder as? SerialValueDecoder)?.tagIdHolder?.tagId
            if (tagId != null) {
                checkNotNull(result) // only a non-null value can have an id
                if (_idMap.put(tagId, result) != null) throw XmlException("Duplicate use of id $tagId")
            }

            return result
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Do nothing. There are no tags. Verifying the presence of an end tag here is invalid
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int { // always 1 child
            return 1
        }
    }

    internal inner class NamedListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        typeDiscriminatorName: QName?,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : TagDecoderBase<XmlListDescriptor>(deserializer, xmlDescriptor, typeDiscriminatorName, inheritedPreserveWhitespace) {

        private var childCount = 0

        override fun decodeElementIndex(): Int {
            stage = STAGE_CONTENT
            return when (input.nextTag()) {
                EventType.END_ELEMENT -> CompositeDecoder.DECODE_DONE.also { stage = STAGE_CLOSE }
                else -> childCount++ // This is important to ensure appending in the list.
            }
        }

        @OptIn(InternalSerializationApi::class, ExperimentalXmlUtilApi::class)
        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            // The index of the descriptor of list children is always at index 0
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(0)
            val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)
            val decoder = SerialValueDecoder(
                effectiveDeserializer,
                childXmlDescriptor,
                super.currentPolyInfo,
                super.lastAttrIndex,
                null,
                false,
                preserveWhitespace,
            )

            // TODO make merging more reliable

            val result = when (effectiveDeserializer) {
                is XmlDeserializationStrategy<T> -> effectiveDeserializer.deserializeXML(decoder, input, previousValue)
                is AbstractCollectionSerializer<*, T, *> -> effectiveDeserializer.merge(decoder, previousValue)
                else -> effectiveDeserializer.deserialize(decoder)
            }

            val tagId = (decoder as? SerialValueDecoder)?.tagIdHolder?.tagId
            if (tagId != null) {
                checkNotNull(result) // only a non-null value can have an id
                if (_idMap.put(tagId, result) != null) throw XmlException("Duplicate use of id $tagId")
            }

            return result
        }
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private abstract inner class MapDecoderBase(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlMapDescriptor,
        private val polyInfo: PolyInfo?,
        typeDiscriminatorName: QName?,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : TagDecoderBase<XmlMapDescriptor>(deserializer, xmlDescriptor, typeDiscriminatorName, inheritedPreserveWhitespace) {

        protected var lastIndex: Int = -1

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            lastIndex = index
            val keyDescriptor = xmlDescriptor.getElementDescriptor(0)
            if (index % 2 == 0) { //key
                if (keyDescriptor.effectiveOutputKind == OutputKind.Attribute) {
                    // When the key is an attribute it is always on the outer tag (either an entry tag or collapsed)
                    val key = input.getAttributeValue(keyDescriptor.tagName)
                        ?: throw XmlSerialException(
                            "Missing key attribute (${keyDescriptor.tagName}) on ${input.name}@${input.extLocationInfo}",
                            input.extLocationInfo
                        )

                    val decoder = StringDecoder(keyDescriptor, input.extLocationInfo, key, preserveWhitespace)

                    /** Map elements again would not be parsing everything in a value child */
                    return deserializer.deserializeSafe(decoder, isParseAllSiblings = false, previousValue)

                } else { // Only attributes collapse, so not collapsed, tag instead. doIndex should handle that
                    assert(!xmlDescriptor.isValueCollapsed)
                    check(input.name isEquivalent keyDescriptor.tagName) { "${input.name} != ${xmlDescriptor.entryName}" }
                    return super.decodeSerializableElement(descriptor, index % 2, deserializer, previousValue)
                }
            }

            // value. Should always be at the value tag at this point
            val valueDescriptor = xmlDescriptor.getElementDescriptor(1)

            val effectiveDeserializer = valueDescriptor.effectiveDeserializationStrategy(deserializer)

            val decoder = SerialValueDecoder(
                effectiveDeserializer,
                valueDescriptor,
                polyInfo,
                Int.MIN_VALUE,
                typeDiscriminatorName,
                false, // TODO: this might need to be more specific.
                preserveWhitespace,
            )
            if (xmlDescriptor.isValueCollapsed) {
                decoder.ignoreAttribute(keyDescriptor.tagName)
            }

            val result = effectiveDeserializer.deserializeSafe(decoder, isParseAllSiblings = false, previousValue)

            val tagId = (decoder as? SerialValueDecoder)?.tagIdHolder?.tagId
            if (tagId != null) {
                checkNotNull(result) // only a non-null value can have an id
                if (_idMap.put(tagId, result) != null) throw XmlException("Duplicate use of id $tagId")
            }

            return result
        }

    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private inner class AnonymousMapDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlMapDescriptor,
        polyInfo: PolyInfo?,
        typeDiscriminatorName: QName?,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : MapDecoderBase(deserializer, xmlDescriptor, polyInfo, typeDiscriminatorName, inheritedPreserveWhitespace) {


        override fun decodeElementIndex(): Int {
            if (!xmlDescriptor.isValueCollapsed) {
                if (lastIndex < 0) {
                    check(input.eventType == EventType.START_ELEMENT)
                    if (!(xmlDescriptor.entryName isEquivalent input.name))
                        throw XmlSerialException(
                            "Map entry not found. Found ${input.name}@${input.extLocationInfo} instead",
                            input.extLocationInfo
                        )
                } else if (lastIndex % 2 == 0) {
                    assert(xmlDescriptor.entryName isEquivalent input.name) {
                        "${xmlDescriptor.entryName} != ${input.name}"
                    }
                }
                // Use the default, but correct the index (map serializer is dumb)
                val rawIndex = super.decodeElementIndex()

                if (rawIndex < 0) return rawIndex
                lastIndex = lastIndex - (lastIndex % 2) + (rawIndex % 2)
                return lastIndex

            } else { // collapsed, thus key as attribute (read in order)
                return when {
                    lastIndex >= 0 && lastIndex % 2 == 1 -> CompositeDecoder.DECODE_DONE
                    else -> { // lists are always decoded as single element lists
                        ++lastIndex
                        lastIndex
                    }
                }
            }

        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!xmlDescriptor.isValueCollapsed) { // close tags if needed
                // TODO (handle unexpected child tags here)
                check(input.eventType == EventType.END_ELEMENT)
            }
            check(input.name isEquivalent xmlDescriptor.entryName)
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int { // always 1 child
            return 2
        }
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private inner class NamedMapDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlMapDescriptor,
        polyInfo: PolyInfo?,
        typeDiscriminatorName: QName?,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : MapDecoderBase(deserializer, xmlDescriptor, polyInfo, typeDiscriminatorName, inheritedPreserveWhitespace) {
        override fun Int.checkRepeat(): Int = this

        override fun decodeElementIndex(): Int {
            if (stage > STAGE_CONTENT) return CompositeDecoder.DECODE_DONE
            if (!xmlDescriptor.isValueCollapsed) {
                if (lastIndex.mod(2) == 1) {
                    while (input.hasNext()) {
                        when (val e = input.peekNextEvent()) {
                            EventType.START_ELEMENT -> {
                                input.next()
                                if (!config.isUnchecked) require(input.name.isEquivalent(xmlDescriptor.entryName))
                                return super.decodeElementIndex().also {
                                    require(it >= 0) { "Map entry must contain a (key) child" }
                                }
                            }

                            EventType.IGNORABLE_WHITESPACE -> input.next()

                            EventType.TEXT -> {
                                input.next()
                                require(input.isWhitespace()) {
                                    "Non-ignorable text content found in map: '${input.text}'"
                                }
                            }

                            EventType.END_ELEMENT -> {
                                check(super.decodeElementIndex() == CompositeDecoder.DECODE_DONE) { "Finished parsing map" }
                                stage = STAGE_CLOSE // no nulls
                                return CompositeDecoder.DECODE_DONE // should be the value
                            }

                            else -> {
                                throw IllegalArgumentException("Unexpected event ${e} in map content")
                            }
                        }
                    }
                } else { // value (is inside entry)
                    return super.decodeElementIndex().also {
                        require(it >= 0) { "Map entry must contain a value child" }
                    }
                }
            } else {

                // Use the default, but correct the index (map serializer is dumb)
                if (lastIndex.mod(2) == 1 && super.decodeElementIndex() < 0) {
                    stage = STAGE_CLOSE
                    return CompositeDecoder.DECODE_DONE // should be the value
                }
            }
            ++lastIndex
//                rawIndex = lastIndex + 1

//            if (rawIndex < 0) return rawIndex
//            lastIndex = lastIndex - (lastIndex % 2) + (rawIndex % 2)
            return lastIndex
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            return super.decodeSerializableElement(descriptor, index, deserializer, previousValue).also {
                if (index % 2 == 1 && !xmlDescriptor.isValueCollapsed) {
                    check(input.nextTag() == EventType.END_ELEMENT) // Do unexpected tag handling
                    assert(xmlDescriptor.entryName isEquivalent input.name)
                }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            assert(xmlDescriptor.tagName isEquivalent input.name)
            super.endStructure(descriptor)
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = -1
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private inner class PolymorphicDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlPolymorphicDescriptor,
        private val polyInfo: PolyInfo?,
        private val isValueChild: Boolean,
        inheritedPreserveWhitespace: DocumentPreserveSpace,
    ) : TagDecoderBase<XmlPolymorphicDescriptor>(deserializer, xmlDescriptor, null, inheritedPreserveWhitespace) {

        private var nextIndex = 0
        private var detectedPolyType: String? = null
        private var polyTypeAttrname: QName? = null

        override fun decodeElementIndex(): Int {
            when (val polyMode = xmlDescriptor.polymorphicMode) {
                PolymorphicMode.TRANSPARENT -> return when (nextIndex) {
                    0, 1 -> nextIndex++
                    else -> CompositeDecoder.DECODE_DONE
                }

                else -> {
                    if (detectedPolyType != null) return when (nextIndex) {
                        1 -> 1
                        else -> CompositeDecoder.DECODE_DONE
                    }
                    if (nextIndex == 0) {
                        for (i in 0 until attrCount) {
                            val attrName = input.getAttributeName(i)

                            if ((attrName.namespaceURI == XSI_NS_URI && attrName.localPart == "type") ||
                                attrName == (polyMode as? PolymorphicMode.ATTR)?.name
                            ) {
                                val sdec = StringDecoder(
                                    xmlDescriptor.getElementDescriptor(0),
                                    input.extLocationInfo,
                                    input.getAttributeValue(i),
                                    preserveWhitespace
                                )

                                // The QName corresponding to the type serialized
                                val typeQName = QNameSerializer.deserializeXML(sdec, input, isValueChild = true).normalize()

                                detectedPolyType = xmlDescriptor.typeQNameToSerialName[typeQName]
                                    ?: throw XmlSerialException("Could not find child for type with qName: $typeQName. " +
                                            "Candidates are: ${xmlDescriptor.typeQNameToSerialName.keys.sortedBy { it.localPart }.joinToString()}")

                                polyTypeAttrname = attrName
                                nextIndex = 1

                                return 0 // Must be the type discriminator of the polymorphic type
                            }
                        }
                    }
                    return super.decodeElementIndex().also { nextIndex = it + 1 }
                }
            }
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

            return when (index) {
                0 -> when {
                    detectedPolyType != null -> detectedPolyType!!
                    !xmlDescriptor.isTransparent -> {
                        val typeTag = xmlDescriptor.getElementDescriptor(0).tagName
                        input.getAttributeValue(typeTag.namespaceURI, typeTag.localPart)
                            ?.expandTypeNameIfNeeded(xmlDescriptor.parentSerialName)
                            ?: throw XmlParsingException(input.extLocationInfo, "Missing type for polymorphic value")
                    }

                    polyInfo != null -> polyInfo.describedName

                    isMixed && (input.eventType == EventType.TEXT ||
                            input.eventType == EventType.IGNORABLE_WHITESPACE ||
                            input.eventType == EventType.CDSECT)
                        -> "kotlin.String" // hardcode handling text input polymorphically

                    else -> {
                        if (input.eventType == EventType.START_ELEMENT) {
                            val m = xmlDescriptor.resolvePolymorphicTypeNameCandidates(input.name, serializersModule)
                            when (m.size) {
                                0 -> error("No XmlSerializable found to handle unrecognized value tag ${input.name} in polymorphic context")
                                1 -> m.first()
                                else -> error("No unique non-primitive polymorphic candidate for value child ${input.name} in polymorphic context (${m.joinToString()})")
                            }
                        } else error("PolyInfo is null for a transparent polymorphic decoder and not in start element context")
                    }
                }

                else -> when { // In a mixed context, pure text content doesn't need a wrapper
                    !xmlDescriptor.isTransparent ->
                        throw XmlSerialException("NonTransparent polymorphic values cannot have text content only")

                    isMixed -> {
                        if (xmlDescriptor.defaultPreserveSpace.withDefault(true)) {
                            input.allConsecutiveTextContent()
                        } else {
                            input.allText()
                        }
                    }

                    else -> super.decodeStringElement(descriptor, index)
                }
            }
        }

        override fun <T> serialElementDecoder(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
        ): XmlDecoder {

            val childXmlDescriptor = polyInfo?.descriptor
                ?: xmlDescriptor.getPolymorphicDescriptor(deserializer.descriptor)

            val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)

            return SerialValueDecoder(
                effectiveDeserializer,
                childXmlDescriptor,
                currentPolyInfo,
                lastAttrIndex,
                polyTypeAttrname,
                isValueChild,
                preserveWhitespace,
            )
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            detectedPolyType?.let {
                val childXmlDescriptor = xmlDescriptor.getPolymorphicDescriptor(it)

                val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)

                val decoder = SerialValueDecoder(
                    effectiveDeserializer,
                    childXmlDescriptor,
                    currentPolyInfo,
                    lastAttrIndex,
                    polyTypeAttrname,
                    isValueChild,
                    preserveWhitespace,
                )
                nextIndex = 2
                val result = when (deserializer) {
                    is XmlDeserializationStrategy ->
                        deserializer.deserializeXML(decoder, input, isValueChild = isValueChild)
                    else -> deserializer.deserialize(decoder)
                }

                val tagId = (decoder as? SerialValueDecoder)?.tagIdHolder?.tagId
                if (tagId != null) {
                    checkNotNull(result) // only a non-null value can have an id
                    if (_idMap.put(tagId, result) != null) throw XmlException("Duplicate use of id $tagId")
                }

                return result
            }

            if (!xmlDescriptor.isTransparent) {
                if (!config.isUnchecked) input.require(EventType.START_ELEMENT, null, "value")
                return super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
            }

            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed
            val isValueChild = index == xmlDescriptor.getValueChild()

            return when {
                isMixed && deserializer.descriptor.kind is PrimitiveKind -> {
                    val childXmlDescriptor = xmlDescriptor.getPolymorphicDescriptor(deserializer.descriptor)
                    deserializer.deserialize(XmlDecoder(childXmlDescriptor, isValueChild = isValueChild, inheritedPreserveWhitespace = preserveWhitespace))
                }

                else ->
                    super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!xmlDescriptor.isTransparent) {
                if (!config.isUnchecked) input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
            } else {
                val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

                if (!isMixed || !xmlDescriptor.isTransparent) { // Don't check in mixed mode as we could have just had raw text
                    val t = polyInfo?.tagName
                    if (t != null) {
                        if (!config.isUnchecked) input.require(EventType.END_ELEMENT, t.namespaceURI, t.localPart)
                    } else {
                        super.endStructure(descriptor)
                    }
                }
            }
        }
    }

}


/**
 * Helper class that collates information used in matching tags with types.
 *
 * @property tagName The expected tag name to test for. Note that prefix is *not* significant.
 * @property index The index of the element that would ultimately be parsed (but maybe indirectly)
 * @property descriptor The descriptor of the actual element in the tag.
 */
@ExperimentalXmlUtilApi
public data class PolyInfo(
    val tagName: QName,
    val index: Int,
    val descriptor: XmlDescriptor
) {

    @OptIn(ExperimentalSerializationApi::class)
    internal val describedName get() = descriptor.serialDescriptor.serialName

}

@OptIn(ExperimentalSerializationApi::class)
internal fun XmlDescriptor.friendlyChildName(idx: Int): String = when (val c = getElementDescriptor(idx)) {
    is XmlPolymorphicDescriptor -> c.polyInfo.values.joinToString(
        separator = " | ",
        prefix = "${serialDescriptor.getElementName(idx)}(",
        postfix = ")"
    ) { it.tagName.toString() }

    else -> c.tagName.toString()
}

private const val STAGE_INIT = 1
private const val STAGE_ATTRS = 2
private const val STAGE_CONTENT = 3
private const val STAGE_NULLS = 4
private const val STAGE_CLOSE = 5

