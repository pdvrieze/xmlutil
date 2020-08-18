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
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.collections.set

internal open class XmlDecoderBase internal constructor(
    context: SerialModule,
    config: XmlConfig,
    input: XmlReader
                                                       ) : XmlCodecBase(context, config) {

    val input = XmlBufferedReader(input)
    var skipRead = false

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

        val deserializer = String.serializer()

        override val input: XmlBufferedReader get() = this@XmlDecoderBase.input

        override val context get() = this@XmlDecoderBase.context

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

    internal open inner class SerialValueDecoder(
        xmlDescriptor: XmlDescriptor,
        polyInfo: PolyInfo?/* = null*/,
        attrIndex: Int/* = -1*/
                                                ) : XmlDecoder(
        xmlDescriptor,
        polyInfo,
        attrIndex
                                                              ) {

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            if (descriptor.isNullable) return TagDecoder(xmlDescriptor)

            val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed
            return when {
                xmlDescriptor.kind is PrimitiveKind
                     -> throw AssertionError("A primitive is not a composite")

                xmlDescriptor is XmlPolymorphicDescriptor
                     -> PolymorphicDecoder(deserializer, xmlDescriptor, polyInfo)

                xmlDescriptor is XmlListDescriptor
                     -> {
                    if (xmlDescriptor.isAnonymous) {
                        AnonymousListDecoder(deserializer, xmlDescriptor, polyInfo)
                    } else {
                        NamedListDecoder(deserializer, xmlDescriptor)
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
    internal inner class NullDecoder(xmlDescriptor: XmlDescriptor) :
        XmlDecoder(xmlDescriptor), CompositeDecoder {

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): T {
            val default = (xmlDescriptor as? XmlValueDescriptor)?.default
            @Suppress("UNCHECKED_CAST")
            return when (default) {
                null -> null as T
                else -> {
                    val decoder = XmlDecoderBase(context, config, CompactFragment(default).getXmlReader())
                        .XmlDecoder(xmlDescriptor)
                    deserializer.deserialize(decoder)
                }
            }
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

        private val seenItems = BooleanArray(xmlDescriptor.elementsCount)

        private var nulledItemsIdx = -1

        // We need to do this here so we can move up when reading (allowing for repeated queries for nullable values)
        protected var lastAttrIndex: Int = -1
            private set

        var currentPolyInfo: PolyInfo? = null

        init {
            val polyMap: MutableMap<QName, PolyInfo> = mutableMapOf()
            val nameMap: MutableMap<QName, Int> = mutableMapOf()

            for (idx in 0 until xmlDescriptor.elementsCount) {
                var child = xmlDescriptor.getElementDescriptor(idx)
                while (child is XmlListDescriptor && child.isAnonymous) {
                    child = child.getElementDescriptor(0)
                }

                if (child is XmlPolymorphicDescriptor && child.transparent) {
                    for ((typeName, childDescriptor) in child.polyInfo) {
                        val tagName = childDescriptor.tagName
                        polyMap[tagName] = PolyInfo(tagName, idx, childDescriptor)
//                        nameMap[tagName] = idx
                    }
                } else {
                    nameMap[child.tagName] = idx
                }
            }
            polyChildren = polyMap
            nameToMembers = nameMap

        }

        override val input: XmlBufferedReader get() = this@XmlDecoderBase.input

        override val namespaceContext: NamespaceContext get() = input.namespaceContext

        internal open fun <T> serialElementDecoder(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): XmlDecoder? {
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(index, deserializer)
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
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(index, deserializer)
            val decoder = serialElementDecoder(descriptor, index, deserializer) ?: NullDecoder(
                childXmlDescriptor
                                                                                              )

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
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(index, deserializer)
            val decoder = serialElementDecoder(descriptor, index, deserializer) ?: NullDecoder(
                childXmlDescriptor
                                                                                              )

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

        open fun indexOf(name: QName, isNameOfAttr: Boolean): Int {
            currentPolyInfo = null

            val polyMap = polyChildren
            val nameMap = nameToMembers

            val normalizedName = name.normalize()
            nameMap[normalizedName]?.let { return it }

            // attributes without namespace would with the new approach be registered properly
            nameMap[name]?.let { return it }
            polyMap[normalizedName]?.let {
                currentPolyInfo = it
                return it.index
            }

            val containingNamespaceUri = serialName.namespaceURI
            // Allow attributes in the null namespace to match candidates with a name that is that of the parent tag
            if (isNameOfAttr) {
                if (name.namespaceURI.isEmpty()) {
                    val attrName = normalizedName.copy(namespaceURI = containingNamespaceUri)
                    nameMap[attrName]?.let { return it }
                    polyMap[attrName]?.let {
                        currentPolyInfo = it
                        return it.index
                    }
                }

                if (name.prefix.isEmpty()) {
                    val emptyNsPrefix = input.getNamespaceURI("")
                    println("Looking for a match for attribute $name, empty ns prefix is: $emptyNsPrefix")
                    if (emptyNsPrefix != null) {
                        polyMap[normalizedName.copy(namespaceURI = emptyNsPrefix)]?.let { return it.index }
                    }
                }
            }

            // If the parent namespace uri is the same as the namespace uri of the element, try looking for an element
            // with a null namespace instead
            if (containingNamespaceUri.isNotEmpty() && containingNamespaceUri == name.namespaceURI) {
                nameMap[QName(name.getLocalPart())]?.let { return it }
            }

            // Hook that will normally throw an exception on an unknown name.
            config.unknownChildHandler(
                input,
                isNameOfAttr,
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
                input.require(EventType.END_ELEMENT, null, null)

                if (nulledItemsIdx >= seenItems.size) return CompositeDecoder.READ_DONE
                val i = nulledItemsIdx
                nextNulledItemsIdx()
                return i
            }
            lastAttrIndex++

            if (lastAttrIndex >= 0 && lastAttrIndex < input.attributeCount) {

                val name = input.getAttributeName(lastAttrIndex)

                return if (name.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI ||
                    name.prefix == XMLConstants.XMLNS_ATTRIBUTE ||
                    (name.prefix.isEmpty() && name.localPart == XMLConstants.XMLNS_ATTRIBUTE)
                ) {
                    // Ignore namespace decls
                    decodeElementIndex(descriptor)
                } else {
                    return indexOf(name, true).ifNegative { decodeElementIndex(descriptor) }
                }
            }
            lastAttrIndex = Int.MIN_VALUE // Ensure to reset here, this should not practically get bigger than 0


            if (skipRead) { // reading values will already move to the end tag.
                assert(input.isEndElement())
                skipRead = false
                return readElementEnd(descriptor)
            }

            for (eventType in input) {
                if (!eventType.isIgnorable) {
                    // The android reader doesn't check whitespaceness

                    when (eventType) {
                        EventType.END_ELEMENT -> return readElementEnd(descriptor)
                        EventType.CDSECT,
                        EventType.TEXT -> {
                            @Suppress("DEPRECATION")
                            if (!input.isWhitespace()) return descriptor.getValueChildOrThrow()
                        }
                        EventType.ATTRIBUTE -> return indexOf(
                            input.name,
                            true
                                                             ).ifNegative { decodeElementIndex(descriptor) }
                        EventType.START_ELEMENT -> when (val i = indexOf(input.name, false)) {
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
                if (!(seenItems[i] || xmlDescriptor.serialDescriptor.isElementOptional(i))) {
                    val default = xmlDescriptor.serialDescriptor.getElementAnnotations(i).firstOrNull<XmlDefault>()

                    val defaultOrList = when {
                        default != null -> true
                        else            -> {

                            val childDesc = try {
                                xmlDescriptor.getElementDescriptor(i)
                            } catch (e: Exception) {
                                null
                            }
                            val kind = childDesc?.kind
                            // If there is no child descriptor and it is missing this can only be because it is
                            // either a list or nullable. It can be treated as such.
                            childDesc == null || childDesc.isNullable || when (kind) {
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
            input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
        }

        open fun readElementEnd(desc: SerialDescriptor): Int {
            nextNulledItemsIdx()
            return when {
                nulledItemsIdx < seenItems.size -> nulledItemsIdx
                else                            -> CompositeDecoder.READ_DONE
            }
        }

        open fun doReadAttribute(desc: SerialDescriptor, index: Int): String {
            return input.getAttributeValue(lastAttrIndex)
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            seenItems[index] = true
            val isAttribute = lastAttrIndex >= 0
            if (isAttribute) {
                return doReadAttribute(descriptor, index)
            } else if (nulledItemsIdx >= 0) { // Now reading nulls
                return descriptor.getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
                    ?: throw MissingFieldException("${descriptor.getElementName(index)}:$index")
            }

            return when (descriptor.outputKind(index, null)) {
                OutputKind.Element -> input.readSimpleElement()
                OutputKind.Mixed,
                OutputKind.Text -> {
                    skipRead = true
                    input.allText()
                }
                OutputKind.Attribute -> error("Attributes should already be read now")
            }
        }

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
            return decodeStringElement(descriptor, index).toInt()
        }

        override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            val location = input.locationInfo
            if (decodeStringElement(descriptor, index) != "kotlin.Unit") throw XmlParsingException(
                location,
                "Kotlin Unit not valid"
                                                                                                  )
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

    internal inner class AnonymousListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor,
        private val polyInfo: PolyInfo?
                                             ) :
        TagDecoder<XmlListDescriptor>(xmlDescriptor) {

        override val updateMode: UpdateMode get() = UpdateMode.UPDATE

        private var finished: Boolean = false

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when {
                finished -> CompositeDecoder.READ_DONE
                else     -> {
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
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(0, deserializer)

            val decoder =
                SerialValueDecoder(
                    childXmlDescriptor,
                    polyInfo,
                    Int.MIN_VALUE
                                  )
            return deserializer.deserialize(decoder)
        }

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
                                                  ): T {
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(index, deserializer)

            val decoder = SerialValueDecoder(
                childXmlDescriptor, polyInfo, Int.MIN_VALUE
                                            )
            return deserializer.patch(decoder, old)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Do nothing. There are no tags. Verifying the presence of an end tag here is invalid
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
            return 1
        }
    }

    internal inner class NamedListDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlListDescriptor
                                         ) :
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
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(0, deserializer)
            val decoder =
                SerialValueDecoder(
                    childXmlDescriptor, super.currentPolyInfo, super.lastAttrIndex
                                  )
            return deserializer.deserialize(decoder)
        }

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
                                                  ): T {
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(index, deserializer)
            val decoder =
                SerialValueDecoder(
                    childXmlDescriptor, super.currentPolyInfo, super.lastAttrIndex
                                  )
            return deserializer.patch(decoder, old)
        }
    }

    internal inner class PolymorphicDecoder(
        deserializer: DeserializationStrategy<*>,
        xmlDescriptor: XmlPolymorphicDescriptor,
        private val polyInfo: PolyInfo?
                                           ) :
        TagDecoder<XmlPolymorphicDescriptor>(xmlDescriptor) {

        private val isMixed get() = xmlDescriptor.outputKind == OutputKind.Mixed

        private val transparent get() = xmlDescriptor.transparent
        private var nextIndex = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when (nextIndex) {
                0, 1 -> nextIndex++
                else -> CompositeDecoder.READ_DONE
            }
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {

            return when (index) {
                0 -> when {
                    isMixed && (input.eventType == EventType.TEXT ||
                            input.eventType == EventType.CDSECT)
                                     -> "kotlin.String"

                    polyInfo == null -> input.getAttributeValue(null, "type")
                        ?.expandTypeNameIfNeeded(xmlDescriptor.parentSerialName)
                        ?: throw XmlParsingException(input.locationInfo, "Missing type for polymorphic value")
                    else             -> polyInfo.describedName
                }
                else -> when { // In a mixed context, pure text content doesn't need a wrapper
                    isMixed -> input.consecutiveTextContent()
                    else    -> {
                        super.decodeStringElement(descriptor, index)
                    }
                }
            }
        }

        override fun doReadAttribute(desc: SerialDescriptor, index: Int): String {
            return if (!transparent) {
                input.getAttributeValue(null, "type")
                    ?: throw XmlParsingException(input.locationInfo, "Missing type for polymorphic value")
            } else {
                polyInfo?.describedName ?: input.name.localPart // Likely to fail unless the tagname matches the type
            }
        }

        override fun indexOf(name: QName, isNameOfAttr: Boolean): Int {
            return if (name.namespaceURI == "" && name.localPart == "type") 0 else 1
        }

        override fun <T> serialElementDecoder(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                             ): XmlDecoder? {
            val childXmlDescriptor = xmlDescriptor.getChildDescriptor(index, deserializer)

            return SerialValueDecoder(
                childXmlDescriptor,
                currentPolyInfo,
                lastAttrIndex
                                     )
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
                                                  ): T {
            if (!transparent) {
                input.nextTag()
                input.require(EventType.START_ELEMENT, null, "value")
                return super.decodeSerializableElement(descriptor, index, deserializer)
            }
            if (isMixed && deserializer.descriptor.kind is PrimitiveKind) {
                val childXmlDescriptor = xmlDescriptor.getPolymorphicDescriptor(deserializer.descriptor.serialName)
                return deserializer.deserialize(
                    XmlDecoder(
                        childXmlDescriptor
                              )
                                               )
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
            if (!transparent) {
                input.nextTag()
                input.require(EventType.START_ELEMENT, null, "value")
            }
            return super.updateSerializableElement(descriptor, index, deserializer, old)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (!transparent) {
                input.nextTag()
                input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
            } else if (!isMixed || !config.autoPolymorphic) { // Don't check in mixed mode as we could have just had raw text
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

inline fun Int.ifNegative(body: () -> Int) = if (this >= 0) this else body()