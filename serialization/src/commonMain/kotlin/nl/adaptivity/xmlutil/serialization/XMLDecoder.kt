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
import kotlinx.serialization.modules.SerialModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlListDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlPolymorphicDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlValueDescriptor
import kotlin.collections.set

internal open class XmlDecoderBase internal constructor(
    context: SerialModule,
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

        override val input: XmlBufferedReader get() = this@XmlDecoderBase.input

        override val context get() = this@XmlDecoderBase.serializersModule

        override val updateMode: UpdateMode get() = UpdateMode.BANNED

        override fun decodeNotNullMark(): Boolean {
            // No null values unless the entire document is empty (not sure the parser is happy with it)
            return input.eventType != EventType.END_DOCUMENT
        }

        override fun decodeNull(): Nothing? {
            // We don't write nulls, so if we know that we have a null we just return it
            return null
        }

        override fun decodeUnit() {
            UnitSerializer().deserialize(this)
        }

        override fun decodeBoolean(): Boolean = decodeStringImpl().toBoolean()

        override fun decodeByte(): Byte = decodeStringImpl().toByte()

        override fun decodeShort(): Short = decodeStringImpl().toShort()

        override fun decodeInt(): Int = decodeStringImpl().toInt()

        override fun decodeLong(): Long = decodeStringImpl().toLong()

        override fun decodeFloat(): Float = decodeStringImpl().toFloat()

        override fun decodeDouble(): Double = decodeStringImpl().toDouble()

        override fun decodeChar(): Char = decodeStringImpl().single()

        override fun decodeString(): String = decodeStringImpl(false)

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
            return enumDescriptor.getElementIndex(decodeStringImpl())
        }

        private fun decodeStringImpl(defaultOverEmpty: Boolean = true): String {
            val defaultString = (xmlDescriptor as? XmlValueDescriptor)?.default
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
                    OutputKind.Mixed -> input.consecutiveTextContent()//.also { input.next() } // Move to the next element
                    OutputKind.Text -> input.allText()
                }
            }
            return when {
                defaultOverEmpty && stringValue.isEmpty() && defaultString != null -> defaultString
                else                                                               -> stringValue
            }
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            throw AssertionError("This should not happen as decodeSerializableValue should be called first")
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            return deserializer.deserialize(SerialValueDecoder(xmlDescriptor, polyInfo, attrIndex))
        }

        override fun <T> updateSerializableValue(deserializer: DeserializationStrategy<T>, old: T): T {
            return deserializer.deserialize(SerialValueDecoder(xmlDescriptor, polyInfo, attrIndex))
        }
    }

    private open inner class SerialValueDecoder(
        xmlDescriptor: XmlDescriptor,
        polyInfo: PolyInfo?/* = null*/,
        attrIndex: Int/* = -1*/
                                               ) :
        XmlDecoder(xmlDescriptor, polyInfo, attrIndex) {

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
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

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): T {
            @Suppress("UNCHECKED_CAST")
            return (xmlDescriptor as? XmlValueDescriptor)?.defaultValue(deserializer) as T
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            return this
        }

        override fun endStructure(descriptor: SerialDescriptor) {}

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            when (descriptor.kind) {
                // Exception to allow for empty lists. They will read the index even if a 0 size was returned
                is StructureKind.MAP,
                is StructureKind.LIST -> return CompositeDecoder.READ_DONE
                else                  -> throw AssertionError("Null objects have no members")
            }
        }

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
                                                  ): T =
            throw AssertionError("Null objects have no members")

        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>
                                                                ): T? {
            throw AssertionError("Null objects have no members")
        }

        override fun <T : Any> updateNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            old: T?
                                                                ): T? {
            throw AssertionError("Null objects have no members")
        }

        override fun decodeNotNullMark() = false

        override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int): Unit =
            throw AssertionError("Null objects have no members")

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
    }

    internal open inner class TagDecoder<D : XmlDescriptor>(xmlDescriptor: D) :
        XmlTagCodec<D>(xmlDescriptor), CompositeDecoder, XML.XmlInput {

        override val updateMode: UpdateMode get() = UpdateMode.BANNED

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
                while (child is XmlListDescriptor && child.isListEluded) {
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

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): T {
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)
            val decoder = serialElementDecoder(descriptor, index, deserializer)
                ?: NullDecoder(childXmlDescriptor)

            val result = deserializer.deserialize(decoder)
            seenItems[index] = true
            return result
        }

        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>
                                                                ): T? {
            val decoder = serialElementDecoder(descriptor, index, deserializer)

            val result = decoder?.let { deserializer.deserialize(it) }
            seenItems[index] = true
            return result
        }

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
                                                  ): T {
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)
            val decoder = serialElementDecoder(descriptor, index, deserializer)
                ?: NullDecoder(childXmlDescriptor)

            val result = deserializer.patch(decoder, old)
            seenItems[index] = true
            return result
        }

        override fun <T : Any> updateNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            old: T?
                                                                ): T? {
            val decoder = serialElementDecoder(descriptor, index, deserializer)

            val result = decoder?.let { d ->
                deserializer.patch(d, old)
            }
            seenItems[index] = true
            return result
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
                    println("Looking for a match for attribute $name, empty ns prefix is: $emptyNsPrefix")
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

                if (nulledItemsIdx >= seenItems.size) return CompositeDecoder.READ_DONE

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
                        EventType.END_ELEMENT -> return readElementEnd(descriptor)

                        EventType.CDSECT,
                        EventType.TEXT -> {
                            // The android reader doesn't check whitespaceness. This code should throw
                            if (!input.isWhitespace()) {
                                return descriptor.getValueChild().ifUnknown {
                                    config.unknownChildHandler(input, InputKind.Text, QName("<CDATA>"), emptyList())
                                    decodeElementIndex(descriptor) // if this doesn't throw, recursively continue
                                }
                            }
                        }

                        EventType.ATTRIBUTE -> return indexOf(
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
            return CompositeDecoder.READ_DONE
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
                else                            -> CompositeDecoder.READ_DONE
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
                    ?: throw MissingFieldException("${descriptor.getElementName(index)}:$index")
            }

            return when (childDesc.outputKind) {
                OutputKind.Element -> input.readSimpleElement()

                OutputKind.Mixed,
                OutputKind.Text -> {
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

        override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            val location = input.locationInfo
            if (decodeStringElement(descriptor, index) != "kotlin.Unit")
                throw XmlParsingException(location, "Kotlin Unit not valid")
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

        override val updateMode: UpdateMode get() = UpdateMode.UPDATE

        private var finished: Boolean = false

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when {
                finished -> CompositeDecoder.READ_DONE
                else     -> { // lists are always decoded as single element lists
                    finished = true; 0
                }
            }
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): T {

            // This is an anonymous list decoder. The descriptor passed here is for a list, not the xml parent element.

            // Note that the child descriptor a list is always at index 0
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(0)

            val decoder = SerialValueDecoder(childXmlDescriptor, polyInfo, Int.MIN_VALUE)
            return deserializer.deserialize(decoder)
        }

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
                                                  ): T {
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)

            val decoder = SerialValueDecoder(childXmlDescriptor, polyInfo, Int.MIN_VALUE)

            return deserializer.patch(decoder, old)
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

        override val updateMode: UpdateMode get() = UpdateMode.UPDATE
        private var childCount = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when (input.nextTag()) {
                EventType.END_ELEMENT -> CompositeDecoder.READ_DONE
                else                  -> childCount++ // This is important to ensure appending in the list.
            }
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): T {
            // The index of the descriptor of list children is always at index 0
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(0)
            val decoder = SerialValueDecoder(childXmlDescriptor, super.currentPolyInfo, super.lastAttrIndex)

            return deserializer.deserialize(decoder)
        }

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
                                                  ): T {
            val childXmlDescriptor = xmlDescriptor.getElementDescriptor(index)
            val decoder = SerialValueDecoder(childXmlDescriptor, super.currentPolyInfo, super.lastAttrIndex)

            return deserializer.patch(decoder, old)
        }
    }

    private inner class PolymorphicDecoder(xmlDescriptor: XmlPolymorphicDescriptor, private val polyInfo: PolyInfo?) :
        TagDecoder<XmlPolymorphicDescriptor>(xmlDescriptor) {

        private var nextIndex = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = when {
            xmlDescriptor.isTransparent -> when (nextIndex) {
                    0, 1 -> nextIndex++
                    else -> CompositeDecoder.READ_DONE
                }
            else                        -> super.decodeElementIndex(descriptor)
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

            return when (index) {
                0 -> when {
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
                                             ): XmlDecoder? {

            val childXmlDescriptor = polyInfo?.descriptor
                ?: xmlDescriptor.getPolymorphicDescriptor(deserializer.descriptor.serialName)

            return SerialValueDecoder(childXmlDescriptor, currentPolyInfo, lastAttrIndex)
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): T {
            if (!xmlDescriptor.isTransparent) {
                input.require(EventType.START_ELEMENT, null, "value")
                return super.decodeSerializableElement(descriptor, index, deserializer)
            }

            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

            if (isMixed && deserializer.descriptor.kind is PrimitiveKind) {
                val childXmlDescriptor = xmlDescriptor.getPolymorphicDescriptor(deserializer.descriptor.serialName)
                return deserializer.deserialize(XmlDecoder(childXmlDescriptor))
            } else {
                return super.decodeSerializableElement(descriptor, index, deserializer)
            }
        }

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
                                                  ): T {
            if (!xmlDescriptor.isTransparent) {
                input.nextTag()
                input.require(EventType.START_ELEMENT, null, "value")
            }
            return super.updateSerializableElement(descriptor, index, deserializer, old)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!xmlDescriptor.isTransparent) {
                input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
            } else {
                val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed

                val autopoly = xmlDescriptor.isTransparent

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