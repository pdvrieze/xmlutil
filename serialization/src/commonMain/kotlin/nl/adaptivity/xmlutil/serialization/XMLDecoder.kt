/*
 * Copyright (c) 2024.
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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
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
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.CompactFragmentSerializer
import nl.adaptivity.xmlutil.util.XmlBooleanSerializer
import kotlin.collections.set
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer as DeprecatedCompactFragmentSerializer

@OptIn(ExperimentalSerializationApi::class)
internal open class XmlDecoderBase internal constructor(
    context: SerializersModule,
    config: XmlConfig,
    input: XmlReader
) : XmlCodecBase(context, config) {

    val input = XmlBufferedReader(input)

    override val namespaceContext: NamespaceContext get() = input.namespaceContext

    private val _idMap = mutableMapOf<String, Any>()

    fun hasNullMark(): Boolean {
        if (input.eventType == EventType.START_ELEMENT) {
            val hasNilAttr = (0 until input.attributeCount).any { i ->
                (input.getAttributeNamespace(i) == XSI_NS_URI &&
                        input.getAttributeLocalName(i) == "nil" &&
                        input.getAttributeValue(i) == "true") ||
                        (input.getAttributeName(i) == config.nilAttribute?.first &&
                                input.getAttributeValue(i) == config.nilAttribute.second)
            }
            if (hasNilAttr) return true // we detected a nullable element
        }
        return false
    }

    fun <T> DeserializationStrategy<T>.deserializeSafe(
        decoder: Decoder,
        previousValue: T? = null,
        isValueChild: Boolean = false
    ): T = when (this) {
        is XmlDeserializationStrategy -> deserializeXML(decoder, input, previousValue, isValueChild)
        else -> deserialize(decoder)
    }

    abstract inner class DecodeCommons(
        xmlDescriptor: XmlDescriptor,
    ) : XmlCodec<XmlDescriptor>(xmlDescriptor), XML.XmlInput, Decoder {
        final override val config: XmlConfig get() = this@XmlDecoderBase.config
        final override val serializersModule: SerializersModule get() = this@XmlDecoderBase.serializersModule
        final override val input: XmlBufferedReader get() = this@XmlDecoderBase.input

        override fun decodeNull(): Nothing? {
            // We don't write nulls, so if we know that we have a null we just return it
            return null
        }

        override fun decodeBoolean(): Boolean = when {
            config.policy.isStrictBoolean -> XmlBooleanSerializer.deserialize(this)
            else -> decodeStringCollapsed().toBoolean()
        }

        override fun decodeByte(): Byte = when {
            xmlDescriptor.isUnsigned -> decodeStringCollapsed().toUByte().toByte()
            else -> decodeStringCollapsed().toByte()
        }

        override fun decodeShort(): Short = when {
            xmlDescriptor.isUnsigned -> decodeStringCollapsed().toUShort().toShort()
            else -> decodeStringCollapsed().toShort()
        }

        override fun decodeInt(): Int = when {
            xmlDescriptor.isUnsigned -> decodeStringCollapsed().toUInt().toInt()
            else -> decodeStringCollapsed().toInt()
        }

        override fun decodeLong(): Long = when {
            xmlDescriptor.isUnsigned -> decodeStringCollapsed().toULong().toLong()
            else -> decodeStringCollapsed().toLong()
        }

        override fun decodeFloat(): Float = decodeStringCollapsed().toFloat()
        override fun decodeDouble(): Double = decodeStringCollapsed().toDouble()
        override fun decodeChar(): Char = decodeStringCollapsed().single()
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
            val stringName = decodeStringCollapsed()
            for (i in 0 until enumDescriptor.elementsCount) {
                if (stringName == config.policy.enumEncoding(enumDescriptor, i)) return i
            }
            throw SerializationException("No enum constant found for name $stringName in ${enumDescriptor.serialName}")
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
        val attrIndex: Int = -1
    ) : DecodeCommons(xmlDescriptor), Decoder, XML.XmlInput, ChunkedDecoder {

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
                input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
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
                        input.require(EventType.START_ELEMENT, serialName.namespaceURI, serialName.localPart)
                        input.readSimpleElement()
                    }

                    OutputKind.Attribute -> throw SerializationException(
                        "Attribute parsing without a concrete index is unsupported"
                    )

                    OutputKind.Inline -> throw SerializationException("Inline classes can not be decoded directly")
                    OutputKind.Mixed -> input.allConsecutiveTextContent()//.also { input.next() } // Move to the next element
                    OutputKind.Text -> if (xmlDescriptor.preserveSpace) {
                        input.allConsecutiveTextContent()
                    } else {
                        input.allText()
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
                        input.require(EventType.START_ELEMENT, serialName.namespaceURI, serialName.localPart)
                        input.readSimpleElementChunked(consumeChunk)
                        return
                    }

                    OutputKind.Attribute -> throw SerializationException(
                        "Attribute parsing without a concrete index is unsupported"
                    )

                    OutputKind.Inline -> throw SerializationException("Inline classes can not be decoded directly")
                    OutputKind.Mixed -> return input.allConsecutiveTextContentChunked(consumeChunk)
                    OutputKind.Text -> return if (xmlDescriptor.preserveSpace) {
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
                triggerInline && xmlDescriptor is XmlInlineDescriptor
                -> xmlDescriptor.getElementDescriptor(0)

                else -> xmlDescriptor

            }
            val serialValueDecoder =
                SerialValueDecoder(effectiveDeserializer, desc, polyInfo, attrIndex, typeDiscriminatorName)
            val value = effectiveDeserializer.deserializeSafe(serialValueDecoder)

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
        private val stringValue: String
    ) :
        Decoder, XML.XmlInput, DecodeCommons(xmlDescriptor) {

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            throw UnsupportedOperationException("Strings cannot be decoded to structures")
        }

        @ExperimentalSerializationApi
        override fun decodeNotNullMark(): Boolean = true

        @ExperimentalSerializationApi
        override fun decodeInline(descriptor: SerialDescriptor): Decoder {
            return StringDecoder(xmlDescriptor.getElementDescriptor(0), locationInfo, stringValue)
        }

        override fun decodeStringImpl(defaultOverEmpty: Boolean): String {
            val defaultString = (xmlDescriptor as? XmlValueDescriptor)?.default
            if (defaultOverEmpty && defaultString != null && stringValue.isEmpty()) return defaultString
            return stringValue
        }

        fun getInputWrapper(locationInfo: XmlReader.LocationInfo?): XmlReader {
            return XmlStringReader(locationInfo, stringValue)
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            val effectiveDeserializationStrategy = xmlDescriptor.effectiveDeserializationStrategy(deserializer)
            return when (effectiveDeserializationStrategy) {
                is XmlDeserializationStrategy ->
                    effectiveDeserializationStrategy.deserializeXML(this, getInputWrapper(locationInfo))

                else -> effectiveDeserializationStrategy.deserialize(this)
            }
        }
    }

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

        @Deprecated(
            "Use extLocationInfo as that allows more detailed information",
            replaceWith = ReplaceWith("extLocationInfo?.toString()")
        )
        override val locationInfo: String?
            get() = extLocationInfo?.toString()

        override val namespaceContext: IterableNamespaceContext
            get() = input.namespaceContext

        override val encoding: Nothing
            get() = throw XmlSerialException("Strings have no document declarations")


        override val standalone: Nothing
            get() = throw XmlSerialException("Strings have no document declarations")

        override val version: Nothing
            get() = throw XmlSerialException("Strings have no document declarations")
    }

    internal interface TagIdHolder {
        var tagId: String?
    }

    /**
     * Wrapper decoder that expects to be used to decode a serial value
     */
    @OptIn(ExperimentalXmlUtilApi::class)
    private open inner class SerialValueDecoder(
        private val deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlDescriptor,
        polyInfo: PolyInfo?,
        attrIndex: Int,/* = -1*/
        override val typeDiscriminatorName: QName?
    ) : XmlDecoder(xmlDescriptor, polyInfo, attrIndex) {
        private var notNullChecked = false

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
            !notNullChecked -> super.decodeSerializableValue(deserializer)
            else -> deserializer.deserializeSafe(this)
        }

        @ExperimentalSerializationApi
        override fun decodeInline(descriptor: SerialDescriptor): Decoder {
            tagIdHolder = object : TagIdHolder {
                override var tagId: String? = null
            }
            return super.decodeInline(descriptor)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            if (descriptor.isNullable) return TagDecoder(
                deserializer,
                xmlDescriptor,
                typeDiscriminatorName
            ).also { tagIdHolder = it }

            return when {
                xmlDescriptor.kind is PrimitiveKind
                -> throw AssertionError("A primitive is not a composite")

                xmlDescriptor is XmlPolymorphicDescriptor
                -> PolymorphicDecoder(deserializer, xmlDescriptor, polyInfo).also { tagIdHolder = it }

                xmlDescriptor is XmlListDescriptor
                -> {
                    when {
                        xmlDescriptor.outputKind == OutputKind.Attribute ->
                            AttributeListDecoder(
                                deserializer,
                                xmlDescriptor,
                                input.extLocationInfo,
                                attrIndex
                            ).also { tagIdHolder = it }

                        xmlDescriptor.outputKind == OutputKind.Text ->
                            ValueListDecoder(deserializer, xmlDescriptor, input.extLocationInfo)

                        xmlDescriptor.isListEluded ->
                            AnonymousListDecoder(deserializer, xmlDescriptor, polyInfo, typeDiscriminatorName).also {
                                tagIdHolder = it
                            }

                        else -> NamedListDecoder(
                            deserializer,
                            xmlDescriptor,
                            typeDiscriminatorName
                        ).also { tagIdHolder = it }
                    }
                }

                xmlDescriptor is XmlMapDescriptor
                -> when {
                    xmlDescriptor.isListEluded ->
                        AnonymousMapDecoder(
                            deserializer,
                            xmlDescriptor,
                            polyInfo,
                            typeDiscriminatorName
                        ).also { tagIdHolder = it }

                    else -> NamedMapDecoder(
                        deserializer,
                        xmlDescriptor,
                        polyInfo,
                        typeDiscriminatorName
                    ).also { tagIdHolder = it }

                }

                else -> TagDecoder(deserializer, xmlDescriptor, typeDiscriminatorName).also { tagIdHolder = it }
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
    private inner class NullDecoder(xmlDescriptor: XmlDescriptor, val isValueChild: Boolean) :
        XmlDecoder(xmlDescriptor), CompositeDecoder {

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
            val default =
                (xmlDescriptor as? XmlValueDescriptor)?.defaultValue(this@XmlDecoderBase, deserializer) ?: previousValue
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
        typeDiscriminatorName: QName?
    ) : TagDecoderBase<D>(deserializer, xmlDescriptor, typeDiscriminatorName) {

        private val readTagName = input.name

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!decodeElementIndexCalled) {
                val index = decodeElementIndex(descriptor)
                if (index != CompositeDecoder.DECODE_DONE) throw XmlSerialException("Unexpected content in end structure")
            }
            input.require(EventType.END_ELEMENT, readTagName)
        }

    }

    @OptIn(ExperimentalXmlUtilApi::class)
    internal abstract inner class TagDecoderBase<D : XmlDescriptor>(
        val deserializer: DeserializationStrategy<*>,
        xmlDescriptor: D,
        protected val typeDiscriminatorName: QName?
    ) : XmlTagCodec<D>(xmlDescriptor), CompositeDecoder, XML.XmlInput, TagIdHolder {

        override var tagId: String? = null
        private val ignoredAttributes: MutableList<QName> = mutableListOf()
        private val tagNameToMembers: Map<QName, Int>
        private val attrNameToMembers: Map<QName, Int>
        private val polyChildren: Map<QName, PolyInfo>
        private val contextualDescriptors: Array<XmlDescriptor?>

        private var preserveWhitespace = xmlDescriptor.preserveSpace

        protected val attrCount: Int = if (input.eventType == EventType.START_ELEMENT) input.attributeCount else 0
        private val tagDepth: Int = input.depth

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
        private var pendingRecovery = ArrayDeque<XML.ParsedData<*>>()

        protected var decodeElementIndexCalled = false

        private fun XmlDescriptor.toNonTransparentChild(): XmlDescriptor {
            var result = this
            while (result is XmlInlineDescriptor || // Inline descriptors are only used when we actually elude the inline content
                (result is XmlListDescriptor && result.isListEluded)
            ) { // Lists may or may not be eluded

                result = result.getElementDescriptor(0)
            }
            if (result is XmlMapDescriptor && result.isListEluded && result.isValueCollapsed) { // some transparent tags
                return result.getElementDescriptor(1).toNonTransparentChild()
            }
            return result
        }

        init {
            val polyMap: MutableMap<QName, PolyInfo> = mutableMapOf()
            val tagNameMap: MutableMap<QName, Int> = mutableMapOf()
            val attrNameMap: MutableMap<QName, Int> = mutableMapOf()
            val contextList = arrayOfNulls<XmlDescriptor>(xmlDescriptor.elementsCount)
            val seenTagNames = HashSet<QName>()
            val seenAttrNames = HashSet<QName>()

            for (idx in 0 until xmlDescriptor.elementsCount) {
                val child = xmlDescriptor.getElementDescriptor(idx).toNonTransparentChild()

                if (child is XmlPolymorphicDescriptor && child.isTransparent) {
                    for ((_, childDescriptor) in child.polyInfo) {
                        /*
                         * For polymorphic value classes this cannot be a multi-value inline. Get
                         * the tag name from the child (even if it is inline).
                        */

                        val tagName = childDescriptor.tagName.normalize()
                        check(seenTagNames.add(tagName)) {
                            "Duplicate name $tagName as polymorphic child in ${xmlDescriptor.serialDescriptor.serialName}"
                        }
                        polyMap[tagName] = PolyInfo(tagName, idx, childDescriptor)
                    }
                } else if (xmlDescriptor.serialKind !is PolymorphicKind && child is XmlContextualDescriptor) {
                    val childSer = deserializer.findChildSerializer(idx, serializersModule)
                    val resolved = child.resolve(childSer.descriptor, config, serializersModule)
                    val childName = resolved.tagName.normalize()
                    when (resolved.effectiveOutputKind) {
                        OutputKind.Attribute -> {
                            check(seenAttrNames.add(childName)) {
                                "Duplicate attribute name $childName as contextual child in ${xmlDescriptor.serialDescriptor.serialName}"
                            }
                            attrNameMap[childName] = idx
                        }

                        else -> {
                            check(seenTagNames.add(childName)) {
                                "Duplicate tag name $childName as contextual child in ${xmlDescriptor.serialDescriptor.serialName}"
                            }
                            tagNameMap[childName] = idx
                        }

                    }

                    contextList[idx] = resolved
                } else {
                    when (child.effectiveOutputKind) {
                        OutputKind.Attribute -> {
                            check(seenAttrNames.add(child.tagName)) {
                                "Duplicate name ${child.tagName} as child in ${xmlDescriptor.serialDescriptor.serialName}"
                            }
                            attrNameMap[child.tagName.normalize()] = idx
                        }

                        else -> {
                            check(seenTagNames.add(child.tagName)) {
                                "Duplicate name ${child.tagName} as child in ${xmlDescriptor.serialDescriptor.serialName}"
                            }
                            tagNameMap[child.tagName.normalize()] = idx
                        }

                    }

                }
            }
            polyChildren = polyMap
            tagNameToMembers = tagNameMap
            attrNameToMembers = attrNameMap
            contextualDescriptors = contextList
        }

        final override val input: XmlBufferedReader get() = this@XmlDecoderBase.input

        override val namespaceContext: NamespaceContext get() = input.namespaceContext

        protected open fun <T> serialElementDecoder(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
        ): XmlDecoder? {
            if (nulledItemsIdx >= 0) return null

            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)

            val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)

            return when {
                effectiveDeserializer.descriptor.kind is PrimitiveKind ->
                    XmlDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)

                else -> SerialValueDecoder(
                    effectiveDeserializer,
                    childXmlDescriptor,
                    currentPolyInfo,
                    lastAttrIndex,
                    null
                )
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

            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)

            val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)

            val isValueChild = xmlDescriptor.getValueChild() == index

            if (false && ((effectiveDeserializer as DeserializationStrategy<*>) == DeprecatedCompactFragmentSerializer) &&
                isValueChild
            ) {
                // handle missing compact fragments
                @Suppress("UNCHECKED_CAST")
                if (nulledItemsIdx >= 0) return (CompactFragment("") as T)
//                input.require(EventType.START_ELEMENT, null)
                return input.siblingsToFragment().let {
                    input.pushBackCurrent() // Make the closing tag again be the next read.
                    @Suppress("UNCHECKED_CAST")
                    (it as? CompactFragment ?: CompactFragment(it)) as T
                }
            }

            val decoder: Decoder = if (lastAttrIndex >= 0 && childXmlDescriptor is XmlAttributeMapDescriptor) {
                AttributeMapDecoder(effectiveDeserializer, childXmlDescriptor, lastAttrIndex)
            } else {
                serialElementDecoder(descriptor, index, effectiveDeserializer)
                    ?: NullDecoder(childXmlDescriptor, isValueChild)
            }

            val result: T = when (effectiveDeserializer) {
                is XmlDeserializationStrategy -> {
                    val r = effectiveDeserializer
                        .deserializeXML(decoder, input, previousValue, xmlDescriptor.getValueChild() == index)

                    // Make sure that the (end tag is not consumed) - it will be consumed by the endStructure function
                    if (input.eventType == EventType.END_ELEMENT && /*isValueChild && */input.depth < tagDepth) {
                        input.pushBackCurrent()
                    }

                    r
                }

                is AbstractCollectionSerializer<*, T, *> ->
                    effectiveDeserializer.merge(decoder, previousValue)

                else -> try {
                    effectiveDeserializer.deserialize(decoder)
                } catch (e: XmlException) {
                    throw e
                } catch (e: Exception) {
                    throw XmlException(
                        "In: ${xmlDescriptor.tagName}/${descriptor.getElementName(index)} Error: ${input.extLocationInfo} - ${e.message}",
                        input.extLocationInfo,
                        e
                    )
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

            if (hasNullMark()) { // process the element
                if (input.nextTag() != EventType.END_ELEMENT)
                    throw SerializationException("Elements with nil tags may not have content")
                return null
            }


            val decoder = serialElementDecoder(descriptor, index, deserializer) ?: return null

            val effectiveDeserializer = xmlDescriptor
                .getElementDescriptor(index)
                .effectiveDeserializationStrategy(deserializer)

            // TODO make merging more reliable
            val result: T? = when (effectiveDeserializer) {
                is XmlDeserializationStrategy -> effectiveDeserializer
                    .deserializeXML(decoder, input, previousValue, xmlDescriptor.getValueChild() == index)

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
            return when (descriptor.kind) {
                is PrimitiveKind -> XmlDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)

                else -> SerialValueDecoder(
                    deserializer,
                    childXmlDescriptor,
                    currentPolyInfo,
                    lastAttrIndex,
                    typeDiscriminatorName
                )
            }
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        open fun indexOf(name: QName, inputType: InputKind): Int {
            // Two functions that allow matching only if the input kind matches the outputkind of the candidate

            fun Int.checkInputType(): Int? {
                val desc = contextualDescriptors[this] ?: xmlDescriptor.getElementDescriptor(this)
                return if (inputType.mapsTo(desc)) this else null
            }

            fun PolyInfo.checkInputType(): PolyInfo? {
                return if (inputType.mapsTo(this.descriptor)) this else null
            }

            val isNameOfAttr = inputType == InputKind.Attribute

            currentPolyInfo = null

            val polyMap = polyChildren
            val tagNameMap = tagNameToMembers
            val attrNameMap = attrNameToMembers

            val normalizedName = name.normalize()

            val nameMap = if (isNameOfAttr) attrNameMap else tagNameMap

            nameMap[normalizedName]?.checkInputType()?.let { return it.checkRepeatAndOrder(inputType) }

            polyMap[normalizedName]?.checkInputType()?.let {
                return it.index.checkRepeatAndOrder(inputType).apply {
                    currentPolyInfo = it
                }
            }

            val containingNamespaceUri = serialName.namespaceURI
            // Allow attributes in the null namespace to match candidates with a name that is that of the parent tag
            if (isNameOfAttr && !config.policy.isStrictAttributeNames) {
                if (name.namespaceURI.isEmpty()) {
                    val attrName = normalizedName.copy(namespaceURI = containingNamespaceUri)
                    nameMap[attrName]?.checkInputType()?.let { return it.checkRepeat() }
                    polyMap[attrName]?.checkInputType()?.let {
                        currentPolyInfo = it
                        return it.index.checkRepeat()
                    }
                }

                if (name.prefix.isEmpty()) {
                    val emptyNsPrefix = input.getNamespaceURI("")
                    if (emptyNsPrefix != null) {
                        val attrName = normalizedName.copy(namespaceURI = emptyNsPrefix)
                        nameMap[attrName]?.checkInputType()?.let { return it.checkRepeat() }
                        polyMap[attrName]?.checkInputType()?.let { return it.index.checkRepeat() }
                    }
                }
            }

            // If the parent namespace uri is the same as the namespace uri of the element, try looking for an element
            // with a null namespace instead
            if (!config.policy.isStrictAttributeNames && containingNamespaceUri.isNotEmpty() && containingNamespaceUri == name.namespaceURI) {
                nameMap[QName(name.getLocalPart())]?.checkInputType()?.let { return it.checkRepeatAndOrder(inputType) }
            }

            if (inputType == InputKind.Attribute && lastAttrIndex in 0 until attrCount) {
                otherAttrIndex.takeIf { it >= 0 }?.let { return it }
            } else {
                xmlDescriptor.getValueChild().takeIf { it >= 0 }?.let { valueChildIdx ->
                    return valueChildIdx.checkRepeat()
                }
            }

            // Hook that will normally throw an exception on an unknown name.
            config.policy.handleUnknownContentRecovering(
                input,
                inputType,
                xmlDescriptor,
                name,
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
            checkRepeat()
            if (config.policy.verifyElementOrder && inputType == InputKind.Element) {
                if (xmlDescriptor is XmlCompositeDescriptor) {
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

        private inline fun Int.markSeenOrHandleUnknown(body: () -> Int): Int {
            return when (this) {
                CompositeDecoder.UNKNOWN_NAME -> body()
                else -> {
                    seenItems[this] = true
                    this
                }
            }
        }


        @OptIn(ExperimentalXmlUtilApi::class)
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (!decodeElementIndexCalled && input.depth < tagDepth) {
                return CompositeDecoder.DECODE_DONE
            }

            decodeElementIndexCalled = true
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
            if (nulledItemsIdx >= 0) {
                // This processes all "missing" elements.

                input.require(EventType.END_ELEMENT, xmlDescriptor.tagName)

                if (nulledItemsIdx >= seenItems.size) return CompositeDecoder.DECODE_DONE

                return nulledItemsIdx.also {// return the current index, and then move to the next value
                    nextNulledItemsIdx()
                }
            }

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

                val name = input.getAttributeName(lastAttrIndex)

                if (name == typeDiscriminatorName || name.getNamespaceURI() == XMLNS_ATTRIBUTE_NS_URI ||
                    name.prefix == XMLNS_ATTRIBUTE ||
                    (name.prefix.isEmpty() && name.localPart == XMLNS_ATTRIBUTE)
                ) {
                    // Ignore namespace decls, just recursively call the function itself
                    return decodeElementIndex(descriptor)
                } else if (name.getNamespaceURI() == XML_NS_URI && name.localPart == "space") {
                    when (input.getAttributeValue(lastAttrIndex)) {
                        "preserve" -> preserveWhitespace = true
                        "default" -> preserveWhitespace = xmlDescriptor.preserveSpace
                    }
                    // If this was explicitly declared as attribute use that as index, otherwise
                    // just skip the attribute.
                    return attrNameToMembers[name]?.also { seenItems[it] = true } ?: decodeElementIndex(descriptor)
                }

                // The ifNegative function will recursively call this function if we didn't find it (and the handler
                // didn't throw an exception). This allows for ignoring unknown elements.
                return indexOf(name, InputKind.Attribute).markSeenOrHandleUnknown { decodeElementIndex(descriptor) }

            }
            lastAttrIndex = Int.MIN_VALUE // Ensure to reset here, this should not practically get bigger than 0

            val valueChild = descriptor.getValueChild()
            // Handle the case of an empty tag for a value child. This is not a nullable item (so shouldn't be
            // treated as such).
            if (valueChild >= 0 && /*input.peek() is XmlEvent.EndElementEvent &&*/ !seenItems[valueChild]) {
                val valueChildDesc = xmlDescriptor.getElementDescriptor(valueChild)
                // Lists/maps need to be empty (treated as null/missing)
                if ((!valueChildDesc.isNullable) && valueChildDesc.kind !is StructureKind.LIST && valueChildDesc.kind !is StructureKind.MAP) {
                    // This code can rely on seenItems to avoid infinite item loops as it only triggers on an empty tag.
                    seenItems[valueChild] = true
                    return valueChild
                }
            }
            for (eventType in input) {
                when (eventType) {
                    EventType.END_ELEMENT -> {
                        return readElementEnd(descriptor)
                    }

                    EventType.START_DOCUMENT,
                    EventType.COMMENT,
                    EventType.DOCDECL,
                    EventType.PROCESSING_INSTRUCTION -> {
                    } // do nothing/ignore

                    EventType.ENTITY_REF,
                    EventType.CDSECT,
                    EventType.IGNORABLE_WHITESPACE,
                    EventType.TEXT -> {
                        // The android reader doesn't check whitespaceness. This code should throw
                        if (input.isWhitespace()) {
                            if (valueChild != CompositeDecoder.UNKNOWN_NAME &&
                                preserveWhitespace
                            ) {
                                val valueKind = xmlDescriptor.getElementDescriptor(valueChild).kind
                                if (valueKind == StructureKind.LIST || valueKind is PrimitiveKind
                                ) { // this allows all primitives (
                                    seenItems[valueChild] = true
                                    return valueChild // We can handle whitespace
                                }
                            }
                        } else if (!input.isWhitespace()) {
                            return valueChild.markSeenOrHandleUnknown {
                                config.policy.handleUnknownContentRecovering(
                                    input,
                                    InputKind.Text,
                                    xmlDescriptor,
                                    QName("<CDATA>"),
                                    emptyList()
                                ).let { pendingRecovery.addAll(it) }
                                decodeElementIndex(descriptor) // if this doesn't throw, recursively continue
                            }
                        }
                    }

                    EventType.ATTRIBUTE -> return indexOf(
                        input.name,
                        InputKind.Attribute
                    ).markSeenOrHandleUnknown { decodeElementIndex(descriptor) }

                    EventType.START_ELEMENT -> when (val i = indexOf(input.name, InputKind.Element)) {
                        // If we have an unknown element read it all, but ignore this. We use elementContentToFragment for this
                        // as a shortcut.
                        CompositeDecoder.UNKNOWN_NAME -> {
                            if (pendingRecovery.isNotEmpty()) {
                                return pendingRecovery.first().elementIndex
                            }
                            input.elementContentToFragment()
                        }

                        else -> return i.also { seenItems[i] = true }
                    }

                    EventType.END_DOCUMENT -> throw XmlSerialException("End document in unexpected location")
                }
            }
            return CompositeDecoder.DECODE_DONE
        }

        private fun nextNulledItemsIdx() {
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
                            childDesc.isNullable || when (childDesc.kind) {
                                StructureKind.LIST,
                                StructureKind.MAP -> true

                                else -> false
                            }
                        }
                    }
                    if (defaultOrList) {
                        nulledItemsIdx = i
                        return
                    }
                }
            }
            nulledItemsIdx = seenItems.size
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!decodeElementIndexCalled) {
                val index = decodeElementIndex(descriptor)
                if (index != CompositeDecoder.DECODE_DONE) throw XmlSerialException("Unexpected content in end structure")
            }
            if (typeDiscriminatorName == null) {
                input.require(EventType.END_ELEMENT, serialName)
            } else { // if there is a type discriminator we have an inherited name
                input.require(EventType.END_ELEMENT, null)
            }
        }

        open fun readElementEnd(desc: SerialDescriptor): Int {
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
            } else if (nulledItemsIdx >= 0) { // Now reading nulls
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
                OutputKind.Text -> {
                    input.allConsecutiveTextContent().also { // add some checks that we only have text content
                        val peek = input.peek()
                        if (peek !is XmlEvent.EndElementEvent) {
                            throw XmlSerialException("Missing end tag after text only content (found: ${peek})")
                        } else if (peek.localName != serialName.localPart) {
                            throw XmlSerialException("Expected end tag local name ${serialName.localPart}, found ${peek.localName}")
                        }
                    }
                }

                OutputKind.Attribute -> error("Attributes should already be read now")
            }
        }

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
            return decodeStringElementCollapsed(descriptor, index).toInt()
        }

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
            val startPos = input.extLocationInfo
            val stringValue = decodeStringElementCollapsed(descriptor, index)
            return when {
                config.policy.isStrictBoolean -> XmlBooleanSerializer.deserialize(
                    StringDecoder(xmlDescriptor.getElementDescriptor(index), startPos, stringValue)
                )

                else -> stringValue.toBoolean()
            }
        }

        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
            return decodeStringElementCollapsed(descriptor, index).toByte()
        }

        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
            return decodeStringElementCollapsed(descriptor, index).toShort()
        }

        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
            return decodeStringElementCollapsed(descriptor, index).toLong()
        }

        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
            return decodeStringElementCollapsed(descriptor, index).toFloat()
        }

        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
            return decodeStringElementCollapsed(descriptor, index).toDouble()
        }

        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
            return decodeStringElementCollapsed(descriptor, index).single()
        }

        fun ignoreAttribute(attrName: QName) {
            ignoredAttributes.add(attrName)
        }

    }

    internal inner class AttributeMapDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlAttributeMapDescriptor,
        private val attrIndex: Int
    ) : TagDecoderBase<XmlAttributeMapDescriptor>(deserializer, xmlDescriptor, null), Decoder {

        private var correctStartIndex = -1
        private var nextIndex: Int = 0

        @ExperimentalSerializationApi
        override fun decodeSequentially(): Boolean = true

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = 1

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = when (nextIndex) {
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

            val decoder = StringDecoder(xmlDescriptor.valueDescriptor, startPos, value)
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
        private val locationInfo: XmlReader.LocationInfo?
    ) : TagDecoderBase<XmlListDescriptor>(deserializer, xmlDescriptor, null) {
        private var listIndex = 0

        private val textValues by lazy {
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
                StringDecoder(xmlDescriptor.getElementDescriptor(index), locationInfo, textValues[listIndex++])
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
        private val attrIndex: Int
    ) : TextualListDecoder(deserializer, xmlDescriptor, locationInfo) {

        override fun getTextValue(): String = input.getAttributeValue(attrIndex)
    }

    internal inner class ValueListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        locationInfo: XmlReader.LocationInfo?
    ) : TextualListDecoder(deserializer, xmlDescriptor, locationInfo) {

        override fun getTextValue(): String = input.text
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    private inner class AnonymousListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        private val polyInfo: PolyInfo?,
        typeDiscriminatorName: QName?,
    ) : TagDecoderBase<XmlListDescriptor>(deserializer, xmlDescriptor, typeDiscriminatorName) {

        private val parentXmlDescriptor: XmlDescriptor get() = xmlDescriptor.tagParent.descriptor as XmlDescriptor
        private val listChildIdx: Int = (0 until parentXmlDescriptor.elementsCount)
            .firstOrNull {
                parentXmlDescriptor.serialDescriptor.getElementAnnotations(it).firstOrNull<XmlValue>()?.value == true
            } ?: -1

        private var finished: Boolean = false

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when {
                finished -> CompositeDecoder.DECODE_DONE
                else -> { // lists are always decoded as single element lists
                    finished = true; 0
                }
            }
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

            @Suppress("DEPRECATION")
            if ((effectiveDeserializer is DeprecatedCompactFragmentSerializer) &&
                (parentXmlDescriptor.getValueChild() == listChildIdx)
            ) {
                return input.elementToFragment().let {
                    @Suppress("UNCHECKED_CAST")
                    (it as? CompactFragment ?: CompactFragment(it)) as T
                }
            } else if ((effectiveDeserializer is CompactFragmentSerializer) &&
                (parentXmlDescriptor.getValueChild() == listChildIdx)
            ) {
                return input.elementToFragment().let {
                    @Suppress("UNCHECKED_CAST")
                    (it as? CompactFragment ?: CompactFragment(it)) as T
                }
            }

            val decoder = SerialValueDecoder(
                effectiveDeserializer,
                childXmlDescriptor,
                polyInfo,
                Int.MIN_VALUE,
                typeDiscriminatorName
            )

            val result = deserializer.deserializeSafe(decoder, previousValue)

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
        typeDiscriminatorName: QName?
    ) : TagDecoderBase<XmlListDescriptor>(deserializer, xmlDescriptor, typeDiscriminatorName) {

        private var childCount = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            decodeElementIndexCalled = true
            return when (input.nextTag()) {
                EventType.END_ELEMENT -> CompositeDecoder.DECODE_DONE
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
                null
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
    ) : TagDecoderBase<XmlMapDescriptor>(deserializer, xmlDescriptor, typeDiscriminatorName) {

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

                    val decoder = StringDecoder(keyDescriptor, input.extLocationInfo, key)

                    return deserializer.deserializeSafe(decoder, previousValue)

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
                typeDiscriminatorName
            )
            if (xmlDescriptor.isValueCollapsed) {
                decoder.ignoreAttribute(keyDescriptor.tagName)
            }

            val result = effectiveDeserializer.deserializeSafe(decoder, previousValue)

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
    ) : MapDecoderBase(deserializer, xmlDescriptor, polyInfo, typeDiscriminatorName) {


        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (!xmlDescriptor.isValueCollapsed) {
                if (lastIndex < 0) {
                    check(input.eventType == EventType.START_ELEMENT)
                    if (!(xmlDescriptor.entryName isEquivalent input.name))
                        throw XmlSerialException(
                            "Map entry not found. Found ${input.name}@${input.extLocationInfo} instead",
                            input.extLocationInfo
                        )
                }
                if (lastIndex % 2 == 0) assert(xmlDescriptor.entryName isEquivalent input.name) {
                    "${xmlDescriptor.entryName} != ${input.name}"
                }
                // Use the default, but correct the index (map serializer is dumb)
                val rawIndex = super.decodeElementIndex(descriptor)

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
        typeDiscriminatorName: QName?
    ) : MapDecoderBase(deserializer, xmlDescriptor, polyInfo, typeDiscriminatorName) {
        override fun Int.checkRepeat(): Int = this

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {

            if (!xmlDescriptor.isValueCollapsed) {
                while (input.peek()?.eventType == EventType.IGNORABLE_WHITESPACE) input.next()

                if (lastIndex.mod(2) == 1) {
                    when (input.peek()?.eventType) {
                        EventType.START_ELEMENT -> {
                            input.next()
                            require(input.name.isEquivalent(xmlDescriptor.entryName))
                            return super.decodeElementIndex(descriptor).also {
                                require(it >= 0) { "Map entry must contain a (key) child" }
                            }
                        }

                        else -> {
                            check(super.decodeElementIndex(descriptor) == CompositeDecoder.DECODE_DONE) { "Finished parsing map" }
                            return CompositeDecoder.DECODE_DONE // should be the value
                        }
                    }
                } else { // value (is inside entry)
                    return super.decodeElementIndex(descriptor).also {
                        require(it >= 0) { "Map entry must contain a value child" }
                    }
                }
            } else {

                // Use the default, but correct the index (map serializer is dumb)
                if (lastIndex.mod(2) == 1 && super.decodeElementIndex(descriptor) < 0) {
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
        private val polyInfo: PolyInfo?
    ) : TagDecoderBase<XmlPolymorphicDescriptor>(deserializer, xmlDescriptor, null) {

        private var nextIndex = 0
        private var detectedPolyType: String? = null
        private var polyTypeAttrname: QName? = null

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
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
                                    input.getAttributeValue(i)
                                )
                                val typeQName = QNameSerializer.deserializeXML(sdec, input, isValueChild = true)

                                val childQnames = xmlDescriptor.polyInfo.map { (childSerialName, childDesc) ->
                                    childSerialName to config.policy.typeQName(childDesc)
                                }

                                detectedPolyType = childQnames.firstOrNull { it.second == typeQName }?.first
                                    ?: throw XmlSerialException("Could not find child for type with qName: $typeQName. Candidates are: ${childQnames.joinToString()}")

                                polyTypeAttrname = attrName
                                nextIndex = 1

                                return 0 // Must be the type discriminator of the polymorphic type
                            }
                        }
                    }
                    return super.decodeElementIndex(descriptor).also { nextIndex = it + 1 }
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

                    isMixed && (input.eventType == EventType.TEXT ||
                            input.eventType == EventType.IGNORABLE_WHITESPACE ||
                            input.eventType == EventType.CDSECT)
                    -> "kotlin.String" // hardcode handling text input polymorphically

                    polyInfo == null -> error("PolyInfo is null for a transparent polymorphic decoder")

                    else -> polyInfo.describedName
                }

                else -> when { // In a mixed context, pure text content doesn't need a wrapper
                    !xmlDescriptor.isTransparent ->
                        throw XmlSerialException("NonTransparent polymorphic values cannot have text content only")

                    isMixed -> input.allConsecutiveTextContent()

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
                ?: xmlDescriptor.getPolymorphicDescriptor(deserializer.descriptor.serialName)

            val effectiveDeserializer = childXmlDescriptor.effectiveDeserializationStrategy(deserializer)

            return SerialValueDecoder(
                effectiveDeserializer,
                childXmlDescriptor,
                currentPolyInfo,
                lastAttrIndex,
                polyTypeAttrname
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
                    polyTypeAttrname
                )
                nextIndex = 2
                val result = when (deserializer) {
                    is XmlDeserializationStrategy -> deserializer.deserializeXML(decoder, input)
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
                input.require(EventType.START_ELEMENT, null, "value")
                return super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
            }

            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

            return when {
                isMixed && deserializer.descriptor.kind is PrimitiveKind -> {
                    val childXmlDescriptor = xmlDescriptor.getPolymorphicDescriptor(deserializer.descriptor.serialName)
                    deserializer.deserialize(XmlDecoder(childXmlDescriptor))
                }

                else ->
                    super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!xmlDescriptor.isTransparent) {
                input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
            } else {
                val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

                if (!isMixed || !xmlDescriptor.isTransparent) { // Don't check in mixed mode as we could have just had raw text
                    val t = polyInfo?.tagName
                    if (t != null) {
                        input.require(EventType.END_ELEMENT, t.namespaceURI, t.localPart)
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
