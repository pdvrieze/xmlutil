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

import kotlinx.serialization.*
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.internal.EnumDescriptor
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.assert

internal open class XmlEncoderBase internal constructor(
    context: SerialContext,
    val target: XmlWriter
                                                       ) : XmlCodecBase(context) {

    /**
     * Encoder class for all primitives (except for initial values). It does not handle attributes. This does not
     * implement XmlOutput as
     */
    internal open inner class XmlEncoder(parentDesc: SerialDescriptor, elementIndex: Int, childDesc: SerialDescriptor?) :
        XmlCodec(parentDesc, elementIndex, childDesc), Encoder {

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
            val defaultValue = parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlDefault>()?.value
            if (value == defaultValue) return
            val outputKind = parentDesc.outputKind(elementIndex, childDesc)
            when (outputKind) {
                OutputKind.Element   -> { // This may occur with list values.
                    target.smartStartTag(serialName) { target.text(value) }
                }
                OutputKind.Attribute -> {
                    target.attribute(serialName.namespaceURI, serialName.localPart, serialName.prefix, value)
                }
                OutputKind.Text      -> target.text(value)
            }
        }

        override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
            encodeString(enumDescription.getElementName(ordinal))
        }

        override fun encodeNotNullMark() {
            // Not null is presence, no mark needed
        }

        override fun encodeUnit() {
            val outputKind = parentDesc.outputKind(elementIndex, childDesc)
            when (outputKind) {
                OutputKind.Attribute -> target.writeAttribute(serialName, serialName.localPart)
                OutputKind.Element -> target.smartStartTag(serialName) { /*No content*/}
                else -> target.text("Unit")
            }

        }

        override fun encodeNull() {
            // Null is absence, no mark needed
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            return beginEncodeCompositeImpl(parentDesc, elementIndex, desc)
        }
    }

    fun beginEncodeCompositeImpl(
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        desc: SerialDescriptor
                                ): CompositeEncoder {
        return when (desc.kind) {
            is PrimitiveKind      -> throw SerializationException("A primitive is not a composite")

            StructureKind.MAP,
            StructureKind.CLASS   -> TagEncoder(parentDesc, elementIndex, desc).apply { writeBegin() }

            StructureKind.LIST    -> ListEncoder(parentDesc, elementIndex, desc).apply { writeBegin() }

            UnionKind.OBJECT,
            UnionKind.ENUM_KIND   -> TagEncoder(parentDesc, elementIndex, desc).apply { writeBegin() }

            UnionKind.SEALED,
            UnionKind.POLYMORPHIC ->
                PolymorphicEncoder(parentDesc, elementIndex, desc).apply { writeBegin() }
        }
    }

    internal open inner class TagEncoder(
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        desc: SerialDescriptor,
        private var deferring: Boolean = true
                                        ) :
        XmlTagCodec(parentDesc, elementIndex, desc), CompositeEncoder, XML.XmlOutput {


        override val target: XmlWriter get() = this@XmlEncoderBase.target
        override val namespaceContext: NamespaceContext get() = this@XmlEncoderBase.target.namespaceContext

        private val deferredBuffer = mutableListOf<CompositeEncoder.() -> Unit>()

        open fun writeBegin() {
            target.smartStartTag(serialName)
        }

        open fun defer(index: Int, childDesc: SerialDescriptor?, deferred: CompositeEncoder.() -> Unit) {
            if (!deferring) {
                deferred()
            } else {
                val outputKind = desc.outputKind(index, childDesc)
                if (outputKind == OutputKind.Attribute) {
                    deferred()
                } else {
                    deferredBuffer.add(deferred)
                }
            }
        }

        override fun <T> encodeSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            saver: SerializationStrategy<T>,
            value: T
                                                  ) {
            val encoder = XmlEncoder(this@TagEncoder.desc, index, saver.descriptor)
            defer(index, saver.descriptor) { saver.serialize(encoder, value) }
        }

        override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean {
            return desc.getElementAnnotations(index).none { it is XmlDefault }
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
            val kind = desc.outputKind(index, null)
            val requestedName = desc.requestedName(index, null)
            when (kind) {
                OutputKind.Element   -> defer(index, null) { target.smartStartTag(requestedName) { /*Empty tag*/ } }
                OutputKind.Attribute -> doWriteAttribute(requestedName, requestedName.getLocalPart())
                OutputKind.Text      -> throw SerializationException("Text cannot represent Unit values")
            }
        }

        final override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) {
            encodeStringElement(desc, index, value.toString())
        }

        final override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
            encodeStringElement(desc, index, value.toString())
        }

        final override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) {
            encodeStringElement(desc, index, value.toString())
        }

        final override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
            encodeStringElement(desc, index, value.toString())
        }

        final override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
            encodeStringElement(desc, index, value.toString())
        }

        final override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) {
            encodeStringElement(desc, index, value.toString())
        }

        final override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) {
            encodeStringElement(desc, index, value.toString())
        }

        final override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) {
            encodeStringElement(desc, index, value.toString())
        }

        override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) {
            throw SerializationException("Unable to serialize element ${desc.getElementName(index)}: $value")
        }

        override fun <T : Any> encodeNullableSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            saver: SerializationStrategy<T>,
            value: T?
                                                                ) {
            if (value != null) {
                encodeSerializableElement(desc, index, saver, value)
            }
            // Null is the absense of values, no need to do more
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            val defaultValue = desc.getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
            if (value == defaultValue) return

            val kind = desc.outputKind(index, null)
            val requestedName = desc.requestedName(index, null)
            when (kind) {
                OutputKind.Element   -> defer(index, null) { target.smartStartTag(requestedName) { text(value) } }
                OutputKind.Attribute -> doWriteAttribute(requestedName, value)
                OutputKind.Text      -> defer(index, null) { target.text(value) }
            }
        }

        override fun endStructure(desc: SerialDescriptor) {
            deferring = false
            for (deferred in deferredBuffer) {
                deferred()
            }
            target.endTag(serialName)
        }

        open fun Any.doWriteAttribute(name: QName, value: String) {
            val actualAttrName: QName = when {
                name.getNamespaceURI().isEmpty() ||
                        (serialName.getNamespaceURI() == name.getNamespaceURI() &&
                                (serialName.prefix == name.prefix))
                     -> QName(name.localPart) // Breaks in android otherwise

                else -> name
            }

            target.writeAttribute(actualAttrName, value)
        }


    }

    private inner class RenamedEncoder(override val serialName: QName, desc: SerialDescriptor, index: Int, childDesc: SerialDescriptor?) :
        XmlEncoder(desc, index, childDesc) {

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            return RenamedTagEncoder(serialName, parentDesc, elementIndex, desc).apply { writeBegin() }
        }
    }

    private inner class RenamedTagEncoder(
        override val serialName: QName,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        desc: SerialDescriptor
                                         ) :
        TagEncoder(parentDesc, elementIndex, desc, true)

    private inner class PolymorphicEncoder(
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        desc: SerialDescriptor
                                          ) :
        TagEncoder(parentDesc, elementIndex, desc, false), XML.XmlOutput {

        val polyChildren = parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlPolyChildren>()?.let {
            polyInfo(parentDesc.requestedName(elementIndex, desc), it.value)
        }

        override var serialName: QName = when (polyChildren) {
            null -> parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlSerialName>()?.toQName()
                ?: parentDesc.getElementName(elementIndex).toQname()
            else -> desc.requestedName(elementIndex, desc)
        }

        override fun defer(index: Int, childDesc: SerialDescriptor?, deferred: CompositeEncoder.() -> Unit) {
            deferred()
        }

        override fun writeBegin() {
            // Don't write a tag if we are transparent
            if (polyChildren == null) super.writeBegin()
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            if (polyChildren != null) {
                if (index == 0) {
                    val regName = polyChildren.lookupName(value)
                    serialName = when (regName?.specified) {
                        true -> regName.name
                        else -> QName(value.substringAfterLast('.'))
                    }
                } else {
                    target.smartStartTag(serialName) { text(value) }
                }
            } else {
                if (index == 0) { // The attribute name is renamed to type and forced to attribute
                    doWriteAttribute(QName("type"), value)
                } else {
                    super.encodeStringElement(desc, index, value)
                }

            }
        }

        override fun <T> encodeSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            saver: SerializationStrategy<T>,
            value: T
                                                  ) {
            if (polyChildren != null) {
                assert(index > 0) // the first element is the type
                // The name has been set when the type was "written"
                val encoder = RenamedEncoder(serialName, desc, index, saver.descriptor)
                saver.serialize(encoder, value)
            } else {
                val encoder = RenamedEncoder(QName("value"), desc, index, saver.descriptor)
                super.defer(index, saver.descriptor) { saver.serialize(encoder, value) }
            }
        }

        override fun endStructure(desc: SerialDescriptor) {
            // Don't write anything if we're transparent
            if (polyChildren == null) {
                super.endStructure(desc)
            }
        }

    }

    /**
     * Writer that does not actually write an outer tag unless a [XmlChildrenName] is specified. In XML children don't need to
     * be wrapped inside a list (unless it is the root). If [XmlChildrenName] is not specified it will determine tag names
     * as if the list was not present and there was a single value.
     */
    private inner class ListEncoder(parentDesc: SerialDescriptor, elementIndex: Int, desc: SerialDescriptor) :
        TagEncoder(parentDesc, elementIndex, desc, deferring = false), XML.XmlOutput {

        val childrenName = parentDesc.requestedChildName(elementIndex)

        override fun defer(index: Int, childDesc: SerialDescriptor?, deferred: CompositeEncoder.() -> Unit) {
            deferred()
        }

        override fun writeBegin() {
            if (childrenName != null) { // Do the default thing if the children name has been specified
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
            desc: SerialDescriptor,
            index: Int,
            saver: SerializationStrategy<T>,
            value: T
                                                  ) {
            if (childrenName != null) {
                saver.serialize(RenamedEncoder(childrenName, desc, index, saver.descriptor), value)
            } else { // Use the outer decriptor and element index
                saver.serialize(XmlEncoder(parentDesc, elementIndex, saver.descriptor), value)
            }
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
            if (childrenName != null) {
                RenamedEncoder(childrenName, desc, index, null).encodeUnit()
            } else {
                target.smartStartTag(serialName) { } // Empty body to automatically get end tag
            }
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            if (index > 0) {
                if (childrenName != null) {
                    RenamedEncoder(childrenName, desc, index, null).encodeString(value)
                } else { // The first element is the element count
                    // This must be as a tag with text content as we have a list, attributes cannot represent that
                    target.smartStartTag(serialName) { text(value) }
                }
            }
        }

        override fun endStructure(desc: SerialDescriptor) {
            if (childrenName != null) {
                super.endStructure(desc)
            }
        }
    }
}
