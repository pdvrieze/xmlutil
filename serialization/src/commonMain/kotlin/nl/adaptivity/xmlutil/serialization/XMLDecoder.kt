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
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.internal.AbstractCollectionSerializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.structure.*
import kotlin.collections.set

@OptIn(ExperimentalSerializationApi::class)
internal open class XmlDecoderBase internal constructor(
    context: SerializersModule,
    config: XmlConfig,
    input: XmlReader
                                                       ) : XmlCodecBase(context, config) {

    val input = XmlBufferedReader(input)

    override val namespaceContext: NamespaceContext get() = input.namespaceContext

    /**
     * @param xmlDescriptor The descriptor for the value that this decoder should decode
     * @property polyInfo If this was instantiated due to a polymorphic match, this property holds that information
     * @property attrIndex If this was instantiated to deserialize an attribute this parameter determines which
     *                     attribute it deserialize (index into the attribute list of the containing tag)
     */
    internal open inner class XmlDecoder(
        xmlDescriptor: XmlDescriptor,
        protected val polyInfo: PolyInfo? = null,
        private val attrIndex: Int = -1
                                        ) :
        XmlCodec<XmlDescriptor>(xmlDescriptor), Decoder, XML.XmlInput {

        private var triggerInline = false

        override val input: XmlBufferedReader get() = this@XmlDecoderBase.input

        override val config: XmlConfig get() = this@XmlDecoderBase.config

        override val serializersModule get() = this@XmlDecoderBase.serializersModule

        override fun decodeNotNullMark(): Boolean {
            // No null values unless the entire document is empty (not sure the parser is happy with it)
            return input.eventType != EventType.END_DOCUMENT
        }

        override fun decodeNull(): Nothing? {
            // We don't write nulls, so if we know that we have a null we just return it
            return null
        }

        override fun decodeBoolean(): Boolean = decodeStringImpl().toBoolean()

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun decodeByte(): Byte = when {
            xmlDescriptor.isUnsigned -> decodeStringImpl().toUByte().toByte()
            else                     -> decodeStringImpl().toByte()
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun decodeShort(): Short = when {
            xmlDescriptor.isUnsigned -> decodeStringImpl().toUShort().toShort()
            else                     -> decodeStringImpl().toShort()
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun decodeInt(): Int = when {
            xmlDescriptor.isUnsigned -> decodeStringImpl().toUInt().toInt()
            else                     -> decodeStringImpl().toInt()
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun decodeLong(): Long = when {
            xmlDescriptor.isUnsigned -> decodeStringImpl().toULong().toLong()
            else                     -> decodeStringImpl().toLong()
        }

        override fun decodeFloat(): Float = decodeStringImpl().toFloat()

        override fun decodeDouble(): Double = decodeStringImpl().toDouble()

        override fun decodeChar(): Char = decodeStringImpl().single()

        override fun decodeString(): String = decodeStringImpl(false)

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
            val stringName = decodeStringImpl()
            for (i in 0 until enumDescriptor.elementsCount) {
                if (stringName == config.policy.enumEncoding(enumDescriptor, i)) return i
            }
            throw SerializationException("No enum constant found for name $enumDescriptor")
        }

        @ExperimentalSerializationApi
        override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder {
            triggerInline = true
            return this
        }

        private fun decodeStringImpl(defaultOverEmpty: Boolean = true): String {
            val defaultString = (xmlDescriptor as? XmlValueDescriptor)?.default
            val descOutputKind = xmlDescriptor.outputKind

            val stringValue = if (attrIndex >= 0) {
                input.getAttributeValue(attrIndex)
            } else {
                when (descOutputKind) {
                    OutputKind.Element   -> { // This may occur with list values.
                        input.require(EventType.START_ELEMENT, serialName.namespaceURI, serialName.localPart)
                        input.readSimpleElement()
                    }
                    OutputKind.Attribute -> throw SerializationException(
                        "Attribute parsing without a concrete index is unsupported"
                                                                        )
                    OutputKind.Inline    -> throw SerializationException("Inline classes can not be decoded directly")
                    OutputKind.Mixed     -> input.consecutiveTextContent()//.also { input.next() } // Move to the next element
                    OutputKind.Text      -> input.allText()
                }
            }
            return when {
                defaultOverEmpty && stringValue.isEmpty() && defaultString != null -> defaultString
                else                                                               -> stringValue
            }
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            throw AssertionError("This should not happen as decodeSerializableValue should be called first")
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            /*
             * When the element is actually an inline we need to make sure to use the child descriptor (for the inline).
             * But only if decodeInline was called previously.
             */
            val desc = when {
                triggerInline && xmlDescriptor is XmlInlineDescriptor
                     -> xmlDescriptor.getElementDescriptor(0)

                else -> xmlDescriptor

            }
            return deserializer.deserialize(SerialValueDecoder(desc, polyInfo, attrIndex))
        }

    }

    private open inner class SerialValueDecoder(
        xmlDescriptor: XmlDescriptor,
        polyInfo: PolyInfo?/* = null*/,
        attrIndex: Int/* = -1*/
                                               ) :
        XmlDecoder(xmlDescriptor, polyInfo, attrIndex) {

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            if (descriptor.isNullable) return TagDecoder(xmlDescriptor)

            return when {
                xmlDescriptor.kind is PrimitiveKind
                     -> throw AssertionError("A primitive is not a composite")

                xmlDescriptor is XmlPolymorphicDescriptor
                     -> PolymorphicDecoder(xmlDescriptor, polyInfo)

                xmlDescriptor is XmlListDescriptor
                     -> {
                    if (xmlDescriptor.isListEluded) {
                        AnonymousListDecoder(xmlDescriptor, polyInfo)
                    } else {
                        NamedListDecoder(xmlDescriptor)
                    }
                }

                else -> TagDecoder(xmlDescriptor)
            }
        }

    }

    /**
     * Special class that handles null values that are not mere primitives. Nice side-effect is that XmlDefault values
     * are actually parsed as XML and can be complex
     */
    private inner class NullDecoder(xmlDescriptor: XmlDescriptor) :
        XmlDecoder(xmlDescriptor), CompositeDecoder {

        override fun decodeNotNullMark() = false

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
                                                  ): T {
            val default = (xmlDescriptor as? XmlValueDescriptor)?.defaultValue(deserializer) ?: previousValue
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
                else                  -> throw AssertionError("Null objects have no members")
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

    internal open inner class TagDecoder<D : XmlDescriptor>(xmlDescriptor: D) :
        XmlTagCodec<D>(xmlDescriptor), CompositeDecoder, XML.XmlInput {

        private val nameToMembers: Map<QName, Int>
        private val polyChildren: Map<QName, PolyInfo>

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

        init {
            val polyMap: MutableMap<QName, PolyInfo> = mutableMapOf()
            val nameMap: MutableMap<QName, Int> = mutableMapOf()

            for (idx in 0 until xmlDescriptor.elementsCount) {
                var child = xmlDescriptor.getElementDescriptor(idx)
                while (child is XmlInlineDescriptor || // Inline descriptors are only used when we actually elude the inline content
                    (child is XmlListDescriptor && child.isListEluded)
                ) { // Lists may or may not be eluded

                    child = child.getElementDescriptor(0)
                }

                if (child is XmlPolymorphicDescriptor && child.isTransparent) {
                    for ((_, childDescriptor) in child.polyInfo) {
                        val tagName = childDescriptor.tagName.normalize()
                        polyMap[tagName] = PolyInfo(tagName, idx, childDescriptor)
//                        nameMap[tagName] = idx
                    }
                } else {
                    nameMap[child.tagName.normalize()] = idx
                }
            }
            polyChildren = polyMap
            nameToMembers = nameMap

        }

        override val input: XmlBufferedReader get() = this@XmlDecoderBase.input

        override val namespaceContext: NamespaceContext get() = input.namespaceContext

        protected open fun <T> serialElementDecoder(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                   ): XmlDecoder? {
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)
            return when {
                nulledItemsIdx >= 0 -> null
                deserializer.descriptor.kind is PrimitiveKind
                                    -> XmlDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)

                else                -> SerialValueDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)
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
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)

            @Suppress("UNCHECKED_CAST")
            val effectiveDeserializer = (childXmlDescriptor.overriddenSerializer as DeserializationStrategy<T>?)
                ?: deserializer

            val decoder = serialElementDecoder(descriptor, index, effectiveDeserializer)
                ?: NullDecoder(childXmlDescriptor)

            val result: T = if (effectiveDeserializer is AbstractCollectionSerializer<*, T, *>) {
                effectiveDeserializer.merge(decoder, previousValue)
            } else {
                effectiveDeserializer.deserialize(decoder)
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
            val decoder = serialElementDecoder(descriptor, index, deserializer) ?: return null

            // TODO make merging more reliable
            val result: T? = if (deserializer is AbstractCollectionSerializer<*, T?, *>) {
                deserializer.merge(decoder, previousValue)
            } else {
                deserializer.deserialize(decoder)
            }

            seenItems[index] = true
            return result
        }

        @ExperimentalSerializationApi
        override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)
            return when (descriptor.kind) {
                is PrimitiveKind -> XmlDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)
                else             -> SerialValueDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)
            }
        }

        open fun indexOf(name: QName, inputType: InputKind): Int {
            // Two functions that allow matching only if the input kind matches the outputkind of the candidate

            fun Int.checkInputType(): Int? {
                return if (inputType.mapsTo(xmlDescriptor.getElementDescriptor(this))) this else null
            }

            fun PolyInfo.checkInputType(): PolyInfo? {
                return if (inputType.mapsTo(this.descriptor)) this else null
            }

            val isNameOfAttr = inputType == InputKind.Attribute

            currentPolyInfo = null

            val polyMap = polyChildren
            val nameMap = nameToMembers

            val normalizedName = name.normalize()
            nameMap[normalizedName]?.checkInputType()?.let { return it }

            polyMap[normalizedName]?.checkInputType()?.let {
                currentPolyInfo = it
                return it.index
            }

            val containingNamespaceUri = serialName.namespaceURI
            // Allow attributes in the null namespace to match candidates with a name that is that of the parent tag
            if (isNameOfAttr) {
                if (name.namespaceURI.isEmpty()) {
                    val attrName = normalizedName.copy(namespaceURI = containingNamespaceUri)
                    nameMap[attrName]?.checkInputType()?.let { return it }
                    polyMap[attrName]?.checkInputType()?.let {
                        currentPolyInfo = it
                        return it.index
                    }
                }

                if (name.prefix.isEmpty()) {
                    val emptyNsPrefix = input.getNamespaceURI("")
                    if (emptyNsPrefix != null) {
                        val attrName = normalizedName.copy(namespaceURI = emptyNsPrefix)
                        nameMap[attrName]?.checkInputType()?.let { return it }
                        polyMap[attrName]?.checkInputType()?.let { return it.index }
                    }
                }
            }

            // If the parent namespace uri is the same as the namespace uri of the element, try looking for an element
            // with a null namespace instead
            if (containingNamespaceUri.isNotEmpty() && containingNamespaceUri == name.namespaceURI) {
                nameMap[QName(name.getLocalPart())]?.checkInputType()?.let { return it }
            }

            // Hook that will normally throw an exception on an unknown name.
            config.unknownChildHandler(
                input,
                inputType,
                name,
                (nameMap.keys + polyMap.keys)
                                      )
            return CompositeDecoder.UNKNOWN_NAME // Special value to indicate the element is unknown (but possibly ignored)
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            /* Decoding works in 3 stages: attributes, child content, null values.
             * Attribute decoding is finished when attrIndex<0
             * child content is finished when nulledItemsIdx >=0
             * Fully finished when the nulled items returns a DONE value
             */
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

            if (lastAttrIndex >= 0 && lastAttrIndex < input.attributeCount) {

                val name = input.getAttributeName(lastAttrIndex)

                return if (name.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI ||
                    name.prefix == XMLConstants.XMLNS_ATTRIBUTE ||
                    (name.prefix.isEmpty() && name.localPart == XMLConstants.XMLNS_ATTRIBUTE)
                ) {
                    // Ignore namespace decls, just recursively call the function itself
                    decodeElementIndex(descriptor)
                } else {
                    // The ifNegative function will recursively call this function if we didn't find it (and the handler
                    // didn't throw an exception). This allows for ignoring unknown elements.
                    return indexOf(name, InputKind.Attribute).ifUnknown { decodeElementIndex(descriptor) }
                }
            }
            lastAttrIndex = Int.MIN_VALUE // Ensure to reset here, this should not practically get bigger than 0

            for (eventType in input) {
                if (!eventType.isIgnorable) {

                    when (eventType) {
                        EventType.END_ELEMENT   -> return readElementEnd(descriptor)

                        EventType.ENTITY_REF,
                        EventType.CDSECT,
                        EventType.TEXT          -> {
                            // The android reader doesn't check whitespaceness. This code should throw
                            if (!input.isWhitespace()) {
                                return descriptor.getValueChild().ifUnknown {
                                    config.unknownChildHandler(input, InputKind.Text, QName("<CDATA>"), emptyList())
                                    decodeElementIndex(descriptor) // if this doesn't throw, recursively continue
                                }
                            }
                        }

                        EventType.ATTRIBUTE     -> return indexOf(
                            input.name,
                            InputKind.Attribute
                                                                 ).ifUnknown { decodeElementIndex(descriptor) }

                        EventType.START_ELEMENT -> when (val i = indexOf(input.name, InputKind.Element)) {
                            // If we have an unknown element read it all, but ignore this. We use elementContentToFragment for this
                            // as a shortcut.
                            CompositeDecoder.UNKNOWN_NAME -> input.elementContentToFragment()
                            else                          -> return i
                        }
                        else                    -> throw AssertionError("Unexpected event in stream")
                    }
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
                        default != null -> true
                        else            -> {

                            // If there is no child descriptor and it is missing this can only be because it is
                            // either a list or nullable. It can be treated as such.
                            childDesc.isNullable || when (childDesc.kind) {
                                StructureKind.LIST,
                                StructureKind.MAP -> true
                                else              -> false
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
            input.require(EventType.END_ELEMENT, serialName)
        }

        open fun readElementEnd(desc: SerialDescriptor): Int {
            // this is triggered on endTag, so we increase the null index to be 0 or higher.
            // We use this function as we still need to do the skipping of the relevant children
            nextNulledItemsIdx()

            return when {
                nulledItemsIdx < seenItems.size -> nulledItemsIdx
                else                            -> CompositeDecoder.DECODE_DONE
            }
        }

        open fun doReadAttribute(lastAttrIndex: Int): String {
            return input.getAttributeValue(this.lastAttrIndex)
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            val childDesc = xmlDescriptor.getElementDescriptor(index)


            seenItems[index] = true
            val isAttribute = lastAttrIndex >= 0
            if (isAttribute) {
                return doReadAttribute(lastAttrIndex)
            } else if (nulledItemsIdx >= 0) { // Now reading nulls
                return (childDesc as? XmlValueDescriptor)?.default
                    ?: throw XmlSerialException("Missing child ${descriptor.getElementName(index)}:$index")
            }

            return when (childDesc.outputKind) {
                OutputKind.Inline    -> throw XmlSerialException("Inline elements can not be directly decoded")

                OutputKind.Element   -> input.readSimpleElement()

                OutputKind.Mixed,
                OutputKind.Text      -> {
                    input.consecutiveTextContent().also { // add some checks that we only have text content
                        val peek = input.peek()
                        if (peek !is XmlEvent.EndElementEvent) {
                            throw XmlSerialException("Missing end tag after text only content")
                        } else if (peek.localName != serialName.localPart) {
                            throw XmlSerialException("Expected end tag local name ${serialName.localPart}, found ${peek.localName}")
                        }
                    }
                }

                OutputKind.Attribute -> error("Attributes should already be read now")
            }
        }

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
            return decodeStringElement(descriptor, index).toInt()
        }

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
            return decodeStringElement(descriptor, index).toBoolean()
        }

        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
            return decodeStringElement(descriptor, index).toByte()
        }

        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
            return decodeStringElement(descriptor, index).toShort()
        }

        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
            return decodeStringElement(descriptor, index).toLong()
        }

        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
            return decodeStringElement(descriptor, index).toFloat()
        }

        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
            return decodeStringElement(descriptor, index).toDouble()
        }

        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
            return decodeStringElement(descriptor, index).single()
        }


    }

    internal data class PolyInfo(
        val tagName: QName,
        val index: Int,
        val descriptor: XmlDescriptor
                                ) {

        val describedName get() = descriptor.serialDescriptor.serialName

    }

    private inner class AnonymousListDecoder(xmlDescriptor: XmlListDescriptor, private val polyInfo: PolyInfo?) :
        TagDecoder<XmlListDescriptor>(xmlDescriptor) {

        private var finished: Boolean = false

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when {
                finished -> CompositeDecoder.DECODE_DONE
                else     -> { // lists are always decoded as single element lists
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

            val decoder = SerialValueDecoder(childXmlDescriptor, polyInfo, Int.MIN_VALUE)
            return deserializer.deserialize(decoder)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Do nothing. There are no tags. Verifying the presence of an end tag here is invalid
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int { // always 1 child
            return 1
        }
    }

    internal inner class NamedListDecoder(xmlDescriptor: XmlListDescriptor) :
        TagDecoder<XmlListDescriptor>(xmlDescriptor) {

        private var childCount = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when (input.nextTag()) {
                EventType.END_ELEMENT -> CompositeDecoder.DECODE_DONE
                else                  -> childCount++ // This is important to ensure appending in the list.
            }
        }

        @OptIn(InternalSerializationApi::class)
        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
                                                  ): T {
            // The index of the descriptor of list children is always at index 0
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(0)
            val decoder = SerialValueDecoder(childXmlDescriptor, super.currentPolyInfo, super.lastAttrIndex)

            // TODO make merging more reliable

            return when (deserializer) {
                is AbstractCollectionSerializer<*, T, *> -> deserializer.merge(decoder, previousValue)
                else                                     -> deserializer.deserialize(decoder)
            }
        }
    }

    private inner class PolymorphicDecoder(xmlDescriptor: XmlPolymorphicDescriptor, private val polyInfo: PolyInfo?) :
        TagDecoder<XmlPolymorphicDescriptor>(xmlDescriptor) {

        private var nextIndex = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = when {
            xmlDescriptor.isTransparent -> when (nextIndex) {
                0, 1 -> nextIndex++
                else -> CompositeDecoder.DECODE_DONE
            }
            else                        -> super.decodeElementIndex(descriptor)
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

            return when (index) {
                0    -> when {
                    !xmlDescriptor.isTransparent -> {
                        val typeTag = xmlDescriptor.getElementDescriptor(0).tagName
                        input.getAttributeValue(typeTag.namespaceURI, typeTag.localPart)
                            ?.expandTypeNameIfNeeded(xmlDescriptor.parentSerialName)
                            ?: throw XmlParsingException(input.locationInfo, "Missing type for polymorphic value")
                    }

                    isMixed && (input.eventType == EventType.TEXT ||
                            input.eventType == EventType.CDSECT)
                                                 -> "kotlin.String" // hardcode handling text input polymorphically

                    polyInfo == null             -> error("PolyInfo is null for a transparent polymorphic decoder")

                    else                         -> polyInfo.describedName
                }

                else -> when { // In a mixed context, pure text content doesn't need a wrapper
                    !xmlDescriptor.isTransparent ->
                        throw XmlSerialException("NonTransparent polymorphic values cannot have text content only")

                    isMixed                      -> input.consecutiveTextContent()

                    else                         -> super.decodeStringElement(descriptor, index)
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

            return SerialValueDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
                                                  ): T {
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

                else                                                     ->
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

private inline fun Int.ifUnknown(body: () -> Int) = if (this != CompositeDecoder.UNKNOWN_NAME) this else body()
