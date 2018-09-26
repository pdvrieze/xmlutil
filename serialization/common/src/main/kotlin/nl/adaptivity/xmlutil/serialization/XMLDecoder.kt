/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.KInput
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerialContext
import kotlinx.serialization.SerializationException
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.compat.*
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.collections.set

open class XmlDecoderBase internal constructor(context: SerialContext?,
                                               val input: XmlReader) : XmlCodecBase(context) {

    var skipRead = false

    internal open inner class XmlDecoder(parentDesc: SerialDescriptor,
                                         elementIndex: Int,
                                         private val polyInfo: PolyInfo? = null,
                                         private val attrIndex: Int = -1) :
            XmlCodec(parentDesc, elementIndex), Decoder {
        override fun decodeNotNullMark(): Boolean {
            // No null values unless the entire document is empty (not sure the parser is happy with it)
            return input.eventType != EventType.END_DOCUMENT
        }

        override fun decodeNull(): Nothing? {
            // We don't write nulls, so if we know that we have a null we just return it
            return null
        }

        override fun decodeUnit() {
            if (decodeString(true) != "kotlin.Unit") throw SerializationException("Did not find Unit where expected")
        }

        override fun decodeBoolean(): Boolean = decodeString(true).toBoolean()

        override fun decodeByte(): Byte = decodeString(true).toByte()

        override fun decodeShort(): Short {
            return decodeString(true).toShort()
        }

        override fun decodeInt(): Int {
            return decodeString(true).toInt()
        }

        override fun decodeLong(): Long {
            return decodeString(true).toLong()
        }

        override fun decodeFloat(): Float {
            return decodeString(true).toFloat()
        }

        override fun decodeDouble(): Double {
            return decodeString(true).toDouble()
        }

        override fun decodeChar(): Char {
            return decodeString(true).single()
        }

        override fun decodeString(): String {
            return decodeString(false)
        }

        private fun decodeString(defaultOverEmpty: Boolean): String {
            val defaultString = parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlDefault>()?.value

            val stringValue = if (attrIndex >= 0) {
                input.getAttributeValue(attrIndex)
            } else when (parentDesc.outputKind(elementIndex)) {
                OutputKind.Element   -> { // This may occur with list values.
                    input.require(EventType.START_ELEMENT, serialName.namespaceURI, serialName.localPart)
                    input.readSimpleElement()
                }
                OutputKind.Attribute -> throw SerializationException(
                        "Attribute parsing without a concrete index is unsupported")
                OutputKind.Text      -> input.allText()
                OutputKind.Unknown   -> throw SerializationException("Cannot deserialize unknown input kind")
            }
            return when {
                defaultOverEmpty && stringValue.isEmpty() && defaultString != null -> defaultString
                else                                                               -> stringValue
            }
        }

        override fun beginDecodeComposite(desc: SerialDescriptor): CompositeDecoder {
            if (desc.isNullable) return TagDecoder(serialName, parentDesc, elementIndex, desc)
            return when (desc.extKind) {
                is PrimitiveKind      -> throw SerializationException("A primitive is not a composite")
                StructureKind.MAP,
                StructureKind.CLASS   -> TagDecoder(serialName, parentDesc, elementIndex, desc)
                StructureKind.LIST    -> {
                    val childName = parentDesc.requestedChildName(elementIndex)
                    if (childName != null) {
                        NamedListDecoder(serialName, parentDesc, elementIndex, desc, childName)
                    } else {
                        AnonymousListDecoder(serialName, parentDesc, elementIndex, desc, polyInfo)
                    }
                }
                UnionKind.OBJECT,
                UnionKind.ENUM        -> TagDecoder(serialName, parentDesc, elementIndex, desc)
                UnionKind.SEALED,
                UnionKind.POLYMORPHIC -> PolymorphicDecoder(serialName, parentDesc, elementIndex, desc, polyInfo)
            }
        }
    }

    internal fun <T> DeserializationStrategy<T>.parseDefaultValue(desc: SerialDescriptor,
                                                                  index: Int,
                                                                  default: String): T {
        val decoder = XmlDecoderBase(context, CompactFragment(default).getXmlReader()).XmlDecoder(desc, index)
        return deserialize(decoder, null)

    }

    /**
     * Special class that handles null values that are not mere primitives. Nice side-effect is that XmlDefault values
     * are actually parsed as XML and can be complex
     */
    internal inner class NullDecoder(parentDesc: SerialDescriptor, elementIndex: Int) : XmlDecoder(parentDesc,
                                                                                                   elementIndex), CompositeDecoder {
        override fun decodeNotNullMark() = false

        override fun <T> decodeSerializable(strategy: DeserializationStrategy<T>): T {
            val default = parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlDefault>()?.value
            @Suppress("UNCHECKED_CAST")
            return when (default) {
                null -> null as T
                else -> {
                    val decoder = XmlDecoderBase(context, CompactFragment(default).getXmlReader())
                            .XmlDecoder(parentDesc, elementIndex)
                    strategy.deserialize(decoder, null)
                }
            }
        }

        override fun beginDecodeComposite(desc: SerialDescriptor): CompositeDecoder {
            return this
        }

        override fun endDecodeComposite(desc: SerialDescriptor) {}

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            return CompositeDecoder.READ_ALL // Let the reader worry about position (and only read the size: 0)
        }

        override fun decodeElementAgain(desc: SerialDescriptor, index: Int) {}

        override fun decodeUnitElement(desc: SerialDescriptor, index: Int): Unit =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int = 0 // Size of map/list

        override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char =
                throw UnsupportedOperationException("Null objects have no members")

        override fun decodeStringElement(desc: SerialDescriptor, index: Int): String =
                throw UnsupportedOperationException("Null objects have no members")

        override fun <T> decodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: DeserializationStrategy<T>,
                                                   oldValue: T?,
                                                   wasRead: Boolean): T {
            throw UnsupportedOperationException("Null objects have no members")
        }
    }

    internal inner class RenamedDecoder(override val serialName: QName,
                                        parentDesc: SerialDescriptor,
                                        elementIndex: Int,
                                        polyInfo: PolyInfo? = null,
                                        attrIndex: Int = Int.MIN_VALUE) : XmlDecoder(parentDesc, elementIndex, polyInfo,
                                                                                     attrIndex)

    internal open inner class TagDecoder(override val serialName: QName,
                                         parentDesc: SerialDescriptor,
                                         elementIndex: Int,
                                         desc: SerialDescriptor) :
            XmlTagCodec(parentDesc, elementIndex, desc), CompositeDecoder, XML.XmlInput {

        private var nameToMembers: Map<QName, Int>
        private var polyChildren: Map<QName, PolyInfo>

        private val seenItems = BooleanArray(desc.elementsCount)

        private var nulledItemsIdx = -1

        // We need to do this here so we can move up when reading (allowing for repeated queries for nullable values)
        protected var lastAttrIndex: Int = -1
            private set

        var currentPolyInfo: PolyInfo? = null

        init {
            val polyMap: MutableMap<QName, PolyInfo> = mutableMapOf()
            val nameMap: MutableMap<QName, Int> = mutableMapOf()

            for (idx in 0 until desc.elementsCount) {
                desc.getElementAnnotations(idx).firstOrNull<XmlPolyChildren>().let { xmlPolyChildren ->
                    if (xmlPolyChildren != null) {
                        for (child in xmlPolyChildren.value) {
                            val polyInfo = polyTagName(serialName, child, idx)
                            polyMap[polyInfo.tagName.normalize()] = polyInfo
                        }
                    } else {
                        val tagName = when (desc.getElementDescriptor(idx).extKind) {
                            UnionKind.SEALED, // For now sealed is treated like polymorphic. We can't enumerate elements yet.
                            UnionKind.POLYMORPHIC -> desc.getElementName(idx).toQname()
                            else                  -> desc.requestedName(idx)
                        }
                        nameMap[tagName.normalize()] = idx
                    }

                }
            }
            polyChildren = polyMap
            nameToMembers = nameMap

        }

        override val input: XmlReader get() = this@XmlDecoderBase.input

        override val namespaceContext: NamespaceContext get() = input.namespaceContext

        override fun <T> decodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: DeserializationStrategy<T>,
                                                   oldValue: T?,
                                                   wasRead: Boolean): T {

            val decoder = when {
                nulledItemsIdx >= 0 -> NullDecoder(desc, index)
                else                -> XmlDecoder(desc, index, currentPolyInfo, lastAttrIndex)
            }

            return strategy.deserialize(decoder, oldValue).also {
                seenItems[index] = true
            }
        }


        open fun indexOf(name: QName, attr: Boolean): Int {
            currentPolyInfo = null

            val polyMap = polyChildren
            val nameMap = nameToMembers

            val normalName = name.normalize()
            nameMap[normalName]?.let { return it }
            polyMap[normalName]?.let {
                currentPolyInfo = it
                return it.index
            }

            if (attr && name.namespaceURI.isEmpty()) {
                val attrName = normalName.copy(namespaceURI = serialName.namespaceURI)
                nameMap[attrName]?.let { return it }
                polyMap[normalName.copy(namespaceURI = serialName.namespaceURI)]?.let { return it.index }
            }

            throw SerializationException("Could not find a field for name $name\n  candidates " +
                                         "were: ${(nameMap.keys + polyMap.keys).joinToString()}")
        }

        override fun decodeElementAgain(desc: SerialDescriptor, index: Int) {
            // Just ignore for now
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            /* Decoding works in 3 stages: attributes, child content, null values.
             * Attribute decoding is finished when attrIndex<0
             * child content is finished when nulledItemsIdx >=0
             * Fully finished when the nulled items returns a DONE value
             */
            if (nulledItemsIdx >= 0) {
                input.require(EventType.END_ELEMENT, null, null)

                if (nulledItemsIdx >= seenItems.size) return KInput.READ_DONE
                val i = nulledItemsIdx
                nextNulledItemsIdx()
                return i
            }
            lastAttrIndex++

            if (lastAttrIndex >= 0 && lastAttrIndex < input.attributeCount) {

                val name = input.getAttributeName(lastAttrIndex)

                return if (name.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI ||
                           name.prefix == XMLConstants.XMLNS_ATTRIBUTE ||
                           (name.prefix.isEmpty() && name.localPart == XMLConstants.XMLNS_ATTRIBUTE)) {
                    // Ignore namespace decls
                    decodeElementIndex(desc)
                } else {
                    indexOf(name, true)
                }
            }
            lastAttrIndex = Int.MIN_VALUE // Ensure to reset here, this should not practically get bigger than 0


            if (skipRead) { // reading values will already move to the end tag.
                assert(input.isEndElement())
                skipRead = false
                return readElementEnd(desc)
            }

            for (eventType in input) {
                if (!eventType.isIgnorable) {
                    // The android reader doesn't check whitespaceness

                    when (eventType) {
                        EventType.END_ELEMENT   -> return readElementEnd(desc)
                        EventType.TEXT          -> if (!input.isWhitespace()) return parentDesc.getValueChild()
                        EventType.ATTRIBUTE     -> return indexOf(input.name, true)
                        EventType.START_ELEMENT -> return indexOf(input.name, false)
                        else                    -> throw SerializationException("Unexpected event in stream")
                    }
                }
            }
            return KInput.READ_DONE
        }

        private fun nextNulledItemsIdx() {
            for (i in (nulledItemsIdx + 1) until seenItems.size) {
                if (!seenItems[i]) {
                    val default = desc.getElementAnnotations(i).firstOrNull<XmlDefault>()

                    val childDesc = desc.getElementDescriptor(i)
                    val kind = childDesc.extKind
                    val defaultOrList = childDesc.isNullable || default != null || when (kind) {
                        StructureKind.LIST,
                        StructureKind.MAP -> true
                        else              -> false
                    }
                    if (defaultOrList) {
                        nulledItemsIdx = i
                        return
                    }
                }
            }
            nulledItemsIdx = seenItems.size
        }

        override fun endDecodeComposite(desc: SerialDescriptor) {
            // TODO record the tag name used to be able to validate leaving
//            input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
            input.require(EventType.END_ELEMENT, null, null)
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

        override fun decodeStringElement(desc: SerialDescriptor, index: Int): String {
            seenItems[index] = true
            val isAttribute = lastAttrIndex >= 0
            if (isAttribute) {
                return doReadAttribute(desc, index)
            } else if (nulledItemsIdx >= 0) { // Now reading nulls
                return desc.getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
                       ?: throw MissingFieldException("${desc.getElementName(index)}:$index")
            }

            val outputKind = desc.outputKind(index)
            return when (outputKind) {
                OutputKind.Element   -> input.readSimpleElement()
                OutputKind.Text      -> {
                    skipRead = true
                    input.allText()
                }
                OutputKind.Attribute -> error("Attributes should already be read now")
                OutputKind.Unknown   -> error("Unknown output kinds should be able to not occur here")
            }
        }

        override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int {
            when (desc.extKind) {
                is StructureKind.MAP,
                is StructureKind.LIST -> if (index == 0) {
                    seenItems[0] = true; return 1
                }// Always read a list element by element
            }
            return decodeStringElement(desc, index).toInt()
        }

        override fun decodeUnitElement(desc: SerialDescriptor, index: Int) {
            if (decodeStringElement(desc, index) != "kotlin.Unit") throw SerializationException("Kotlin Unit not valid")
        }

        override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean {
            return decodeStringElement(desc, index).toBoolean()
        }

        override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte {
            return decodeStringElement(desc, index).toByte()
        }

        override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short {
            return decodeStringElement(desc, index).toShort()
        }

        override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long {
            return decodeStringElement(desc, index).toLong()
        }

        override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float {
            return decodeStringElement(desc, index).toFloat()
        }

        override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double {
            return decodeStringElement(desc, index).toDouble()
        }

        override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char {
            return decodeStringElement(desc, index).single()
        }


    }

    internal inner class AnonymousListDecoder(serialName: QName,
                                              parentDesc: SerialDescriptor,
                                              elementIndex: Int,
                                              desc: SerialDescriptor,
                                              private val polyInfo: PolyInfo?)
        : TagDecoder(serialName, parentDesc, elementIndex, desc) {

        private var finished: Boolean = false

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            return when {
                finished -> KInput.READ_DONE
                else     -> {
                    finished = true; 1
                }
            }
        }

        override fun <T> decodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: DeserializationStrategy<T>,
                                                   oldValue: T?,
                                                   wasRead: Boolean): T {

            val childName = polyInfo?.tagName ?: parentDesc.requestedChildName(elementIndex) ?: serialName

            val decoder = RenamedDecoder(childName, desc, index, polyInfo, Int.MIN_VALUE)
            return strategy.deserialize(decoder, oldValue)
        }
    }

    internal inner class NamedListDecoder(serialName: QName,
                                          parentDesc: SerialDescriptor,
                                          elementIndex: Int,
                                          desc: SerialDescriptor,
                                          private val childName: QName) :
            TagDecoder(serialName, parentDesc, elementIndex, desc) {
        private var childCount = 0

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            return when (input.nextTag()) {
                EventType.END_ELEMENT -> KInput.READ_DONE
                else                  -> ++childCount // This is important to ensure appending in the list.
            }
        }

        override fun <T> decodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: DeserializationStrategy<T>,
                                                   oldValue: T?,
                                                   wasRead: Boolean): T {
            val decoder = RenamedDecoder(childName, desc, index, super.currentPolyInfo, super.lastAttrIndex)
            return strategy.deserialize(decoder, oldValue)
        }
    }

    internal inner class PolymorphicDecoder(
            override val serialName: QName,
            parentDesc: SerialDescriptor,
            elementIndex: Int,
            desc: SerialDescriptor,
            private val polyInfo: PolyInfo?)
        : TagDecoder(serialName, parentDesc, elementIndex, desc) {

        private val transparent get() = polyInfo != null

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            return CompositeDecoder.READ_ALL // We don't need housekeeping this way
        }

        override fun decodeStringElement(desc: SerialDescriptor, index: Int): String {
            return when (index) {
                0    -> when (polyInfo) {
                    null -> input.getAttributeValue(null, "type")
                            ?: throw SerializationException("Missing type for polymorphic value")
                    else -> polyInfo.kClass
                }
                else -> super.decodeStringElement(desc, index)
            }
        }

        override fun doReadAttribute(desc: SerialDescriptor, index: Int): String {
            return if (!transparent) {
                input.getAttributeValue(null, "type")
                ?: throw SerializationException("Missing type for polymorphic value")
            } else {
                polyInfo?.kClass ?: input.name.localPart // Likely to fail unless the tagname matches the type
            }
        }

        override fun indexOf(name: QName, attr: Boolean): Int {
            return if (name.namespaceURI == "" && name.localPart == "type") 0 else 1
        }

        override fun <T> decodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: DeserializationStrategy<T>,
                                                   oldValue: T?,
                                                   wasRead: Boolean): T {
            if (!transparent) {
                input.nextTag()
                input.require(EventType.START_ELEMENT, null, "value")
            }
            return super.decodeSerializableElement(desc, index, strategy, oldValue, wasRead)
        }

        override fun endDecodeComposite(desc: SerialDescriptor) {
            if (!transparent) {
                input.nextTag()
                input.require(EventType.END_ELEMENT, serialName.namespaceURI, serialName.localPart)
            }
            super.endDecodeComposite(desc)
        }
    }

}
