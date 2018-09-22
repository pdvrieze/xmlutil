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
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.compat.*

open class XmlEncoderBase internal constructor(val context: SerialContext?,
                                               val target: XmlWriter) {

/*
    internal fun XML.XmlOutput.writeBegin(desc: KSerialClassDesc,
                                          useAnnotations: List<Annotation>,
                                          useTagName: QName?,
                                          childName: QName?,
                                          extInfo: ExtInfo): KOutput {
        val tagName = useTagName ?: desc.getAnnotationsForClass().getXmlSerialName(null) ?: desc.name.toQname()

        return when (desc.kind) {
            KSerialClassKind.LIST,
            KSerialClassKind.MAP,
            KSerialClassKind.SET    -> {
                val tname: String?
                if (childName != null) {
                    target.smartStartTag(tagName)

                    // If the child tag has a different namespace uri that requires a namespace declaration
                    // And we didn't just declare the prefix here already then we will declare it here rather
                    // than on each child
                    if (tagName.prefix != childName.prefix && target.getNamespaceUri(
                                    childName.prefix) != childName.namespaceURI) {
                        target.namespaceAttr(childName.prefix, childName.namespaceURI)
                    }
                    tname = desc.name
                } else {
                    tname = currentTypeName
                }

                ListWriter(tagName, childName, tname, useAnnotations, extInfo)
            }
            KSerialClassKind.POLYMORPHIC,
            KSerialClassKind.SEALED -> {
//                    val currentTypeName = (this as? XmlCommon<*>)?.myCurrentTag?.desc?.name
                val polyChildren = useAnnotations.firstOrNull<XmlPolyChildren>()
                val transparent = desc.associatedFieldsCount == 1 || polyChildren != null
                if (!transparent) {
                    target.smartStartTag(tagName)
                }

                PolymorphicWriter(serialName, tagName, transparent, currentTypeName, polyChildren, extInfo)
            }
            KSerialClassKind.CLASS,
            KSerialClassKind.OBJECT -> {
                target.smartStartTag(tagName)
                Base(tagName, childName, desc.associatedFieldsCount > 1, extInfo)
            }

            KSerialClassKind.ENTRY  -> TODO("Maps are not yet supported")//MapEntryWriter(currentTagOrNull)
            else                    -> throw SerializationException(
                    "Primitives are not supported at top-level")
        }
    }
*/

    inner open class Initial(override val serialName: QName,
                             val childName: QName?,
                             override val currentTypeName: String?,
                             val desc: SerialDescriptor) : Encoder, XML.XmlOutput {

        override val context: SerialContext? get() = this@XmlEncoderBase.context
        override val target: XmlWriter get() = this@XmlEncoderBase.target

        /**
         * Create an output for the element. We only need this as xml serialization must always start with an
         * element. We should not have any tag defined yet
         *
         * @param desc The description for the *new* element
         */
        override fun beginEncodeComposite(desc: SerialDescriptor): CompositeEncoder {
            val dummyParent = DummyParentDescriptor(serialName, desc)
            return beginEncodeCompositeImpl(dummyParent, 0, desc)
        }
    }

    /**
     * Encoder class for all primitives (except for initial values). It does not handle attributes. This does not
     * implement XmlOutput as
     */
    open inner class XmlEncoder(val parentDesc: SerialDescriptor, val elementIndex: Int) : Encoder {
        open val serialName: QName
            get() = parentDesc.requestedName(elementIndex)

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
            val outputKind = parentDesc.outputKind(elementIndex)
            when (outputKind) {
                OutputKind.Element   -> { // This may occur with list values.
                    target.smartStartTag(serialName) { target.text(value) }
                }
                OutputKind.Attribute -> {
                    target.attribute(serialName.namespaceURI, serialName.localPart, serialName.prefix, value)
                }
                OutputKind.Text      -> target.text(value)
                OutputKind.Unknown   -> throw SerializationException("Cannot serialize unknown output kind")
            }
        }

        override fun encodeNotNullMark() {
            // Not null is presence, no mark needed
        }

        override fun encodeNull() {
            // Null is absence, no mark needed
        }

        override fun beginEncodeComposite(desc: SerialDescriptor): CompositeEncoder {
            return beginEncodeCompositeImpl(parentDesc, elementIndex, desc)
        }
    }

    fun beginEncodeCompositeImpl(parentDesc: SerialDescriptor,
                                 elementIndex: Int,
                                 desc: SerialDescriptor): CompositeEncoder {
        return when (desc.extKind) {
            is PrimitiveKind      -> throw SerializationException("A primitive is not a composite")
            StructureKind.MAP,
            StructureKind.CLASS   -> TagEncoder(parentDesc, elementIndex, desc).apply { writeBegin() }
            StructureKind.LIST    -> ListEncoder(parentDesc, elementIndex, desc).apply { writeBegin() }
            /*{
            val childName = parentDesc.getElementAnnotations(elementIndex).getChildName()
            if (childName==null) { // We elide the outer tag, not needed in xml

            } else {
                RenamedTagEncoder(parentDesc, elementIndex)
            }
        }*/
            UnionKind.OBJECT,
            UnionKind.ENUM        -> TagEncoder(parentDesc, elementIndex, desc).apply { writeBegin() }
            UnionKind.SEALED,
            UnionKind.POLYMORPHIC -> PolymorphicEncoder(parentDesc, elementIndex, desc, transparent = false,
                                                        currentTypeName = null).apply { writeBegin() }
        }
    }

    inner class XmlAttrEncoder(parentDesc: SerialDescriptor, elementIndex: Int) : XmlEncoder(parentDesc, elementIndex) {
        override fun encodeNotNullMark() {} // Do nothing, omission is null

        override fun encodeNull() {} // Do nothing, omission is null

        override fun encodeUnit() {
            target.attribute(serialName.namespaceURI, serialName.localPart, serialName.prefix, serialName.localPart)
        }

        override fun encodeString(value: String) {
            val defaultValue = parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlDefault>()?.value
            if (value == defaultValue) return
            target.attribute(serialName.namespaceURI, serialName.localPart, serialName.prefix, value)
        }

        override fun beginEncodeComposite(desc: SerialDescriptor): CompositeEncoder {
            throw SerializationException("Composite types cannot be stored as attributes")
        }
    }

    internal inner open class TagEncoder(val parentDesc: SerialDescriptor,
                                         val elementIndex: Int,
                                         val desc: SerialDescriptor,
                                         private var deferring: Boolean = true) : CompositeEncoder, XML.XmlOutput {

        open val tagSerialName = parentDesc.requestedName(elementIndex)

        @Deprecated("Use tagSerialName", ReplaceWith("tagSerialName"))
        override val serialName: QName
            get() = tagSerialName
        override var currentTypeName: String? = null

        override val context: SerialContext? get() = this@XmlEncoderBase.context
        override val target: XmlWriter get() = this@XmlEncoderBase.target
        private val deferredBuffer = mutableListOf<Pair<Int, CompositeEncoder.() -> Unit>>()

        open fun writeBegin() {
            target.smartStartTag(tagSerialName)
        }

        open fun defer(index: Int, deferred: CompositeEncoder.() -> Unit) {
            if (!deferring) {
                deferred()
            } else {
                val outputKind = desc.outputKind(index)
                if (outputKind == OutputKind.Attribute) {
                    deferred()
                } else {
                    deferredBuffer.add(Pair(index, deferred))
                }
            }
        }

        override fun <T> encodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: SerializationStrategy<T>,
                                                   value: T) {
            val encoder = XmlEncoder(this@TagEncoder.desc, index)
            defer(index) { strategy.serialize(encoder, value) }
        }

        override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean {
            return desc.getElementAnnotations(index).none { it is XmlDefault }
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
            val kind = desc.outputKind(index)
            val requestedName = desc.requestedName(index)
            when (kind) {
                OutputKind.Element   -> defer(index) { target.smartStartTag(requestedName) { /*Empty tag*/ } }
                OutputKind.Attribute -> doWriteAttribute(requestedName, requestedName.getLocalPart())
                OutputKind.Text      -> throw SerializationException("Text cannot represent Unit values")
                OutputKind.Unknown   -> throw SerializationException(
                        "Don't know how to serialize ${desc.getElementName(index)}")
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

        override fun encodeNullElement(desc: SerialDescriptor, index: Int) {
            // null is the absense of values
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            val defaultValue = desc.getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
            if (value == defaultValue) return

            val kind = desc.outputKind(index)
            val requestedName = desc.requestedName(index)
            when (kind) {
                OutputKind.Element   -> defer(index) { target.smartStartTag(requestedName) { text(value) } }
                OutputKind.Attribute -> doWriteAttribute(requestedName, value)
                OutputKind.Text      -> defer(index) { target.text(value) }
                OutputKind.Unknown   -> throw SerializationException(
                        "Don't know how to serialize ${desc.getElementName(index)}")
            }
        }

        override fun endEncodeComposite(desc: SerialDescriptor) {
            deferring = false
            for (deferred in deferredBuffer) {
                TagSaver(deferred.first, this).apply(deferred.second)
            }
            target.endTag(tagSerialName)
        }

        open fun Any.doWriteAttribute(name: QName, value: String) {
            val actualAttrName: QName = when {
                name.getNamespaceURI().isEmpty() ||
                (tagSerialName.getNamespaceURI() == name.getNamespaceURI() &&
                 (tagSerialName.prefix == name.prefix)) -> QName(name.localPart) // Breaks in android otherwise

                else                                    -> name
            }

            target.writeAttribute(actualAttrName, value)
        }


    }

    private inner class RenamedEncoder(newName: QName,
                                       private val desc: SerialDescriptor,
                                       private val index: Int) : XmlEncoder(desc, index) {
        override val serialName: QName = newName

        override fun beginEncodeComposite(desc: SerialDescriptor): CompositeEncoder {
            return RenamedTagEncoder(serialName, parentDesc, index, desc).apply { writeBegin() }
        }
    }

    private final inner class RenamedTagEncoder(override val tagSerialName: QName,
                                                parentDesc: SerialDescriptor,
                                                elementIndex: Int,
                                                desc: SerialDescriptor,
                                                deferring: Boolean = true) : TagEncoder(parentDesc, elementIndex, desc,
                                                                                        deferring) {

    }

    private inner class PolymorphicEncoder(parentDesc: SerialDescriptor,
                                           elementIndex: Int,
                                           desc: SerialDescriptor,
                                           val transparent: Boolean,
                                           override var currentTypeName: String?) :
            TagEncoder(parentDesc, elementIndex, desc, false), XML.XmlOutput {

        override var tagSerialName: QName = when {
            transparent -> desc.requestedName(elementIndex)
            else        -> parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlSerialName>()?.toQName()
                           ?: parentDesc.getElementName(elementIndex).toQname()

        }

        fun polyTagName(parentTag: QName,
                        polyChild: String,
                        currentTypeName: String?,
                        itemIdx: Int): PolyInfo {
            val currentPkg = currentTypeName?.substringBeforeLast('.', "") ?: ""
            val eqPos = polyChild.indexOf('=')
            val pkgPos: Int
            val prefPos: Int
            val typeNameBase: String
            val prefix: String
            val localPart: String

            if (eqPos < 0) {
                typeNameBase = polyChild
                pkgPos = polyChild.lastIndexOf('.')
                prefPos = -1
                prefix = parentTag.prefix
                localPart = if (pkgPos < 0) polyChild else polyChild.substring(pkgPos + 1)
            } else {
                typeNameBase = polyChild.substring(0, eqPos).trim()
                pkgPos = polyChild.lastIndexOf('.', eqPos - 1)
                prefPos = polyChild.indexOf(':', eqPos + 1)

                if (prefPos < 0) {
                    prefix = parentTag.prefix
                    localPart = polyChild.substring(eqPos + 1).trim()
                } else {
                    prefix = polyChild.substring(eqPos + 1, prefPos).trim()
                    localPart = polyChild.substring(prefPos + 1).trim()
                }
            }

            val ns = if (prefPos >= 0) target.namespaceContext.getNamespaceURI(prefix)
                                       ?: parentTag.namespaceURI else parentTag.namespaceURI

            val typename = if (pkgPos >= 0 || currentPkg.isEmpty()) typeNameBase else "$currentPkg.$typeNameBase"

            val name = QName(ns, localPart, prefix)

            return PolyInfo(typename, name, itemIdx)
        }


        fun polyInfo(parentTag: QName,
                     currentTypeName: String?,
                     polyChildren: Array<String>): XmlNameMap {
            val result = XmlNameMap()

            for (polyChild in polyChildren) {
                val polyInfo = polyTagName(parentTag, polyChild, currentTypeName, -1)

                result.registerClass(polyInfo.tagName, polyInfo.kClass, polyChild.indexOf('=') >= 0)
            }

            return result
        }

        val polyChildren = parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlPolyChildren>()?.let {
            polyInfo(parentDesc.requestedName(elementIndex), currentTypeName, it.value)
        }

        override fun defer(index: Int, deferred: CompositeEncoder.() -> Unit) {
            deferred()
        }

        override fun writeBegin() {
            // Don't write a tag if we are transparent
            if (!transparent) super.writeBegin()
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            if (transparent) {
                if (index == 0) {
                    val regName = polyChildren?.lookupName(value)
                    tagSerialName = when (regName?.specified) {
                        true -> regName.name
                        else -> QName(value.substringAfterLast('.'))
                    }
                } else {
                    target.smartStartTag(tagSerialName) { text(value) }
                }
            } else {
                if (index == 0) { // The attribute name is renamed to type and forced to attribute
                    doWriteAttribute(QName("type"), value)
                } else {
                    super.encodeStringElement(desc, index, value)
                }

            }
        }

        override fun <T> encodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: SerializationStrategy<T>,
                                                   value: T) {
            if (transparent) {
                assert(index > 0) // the first element is the type
                val encoder = RenamedEncoder(QName("type"), desc, index)
                strategy.serialize(encoder, value)
            } else {
                val encoder = RenamedEncoder(QName("value"), desc, index)
                super.defer(index) { strategy.serialize(encoder, value) }
            }
        }

        override fun endEncodeComposite(desc: SerialDescriptor) {
            // Don't write anything if we're transparent
            if (!transparent) {
                super.endEncodeComposite(desc)
            }
        }

    }


    @Deprecated("Doesn't seem to do much")
    private inner class TagSaver(val tag: Int,
                                 val delegate: TagEncoder) : CompositeEncoder, XML.XmlOutput {
        override val context: SerialContext?
            get() = this@XmlEncoderBase.context

        override val serialName: QName get() = delegate.serialName

        override val target: XmlWriter get() = delegate.target

        override val currentTypeName: String? get() = delegate.desc.getElementName(tag)

        override fun <T> encodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: SerializationStrategy<T>,
                                                   value: T) {
            throw UnsupportedOperationException("Nesting saving of tags is unsupported")
        }

        override fun endEncodeComposite(desc: SerialDescriptor) {
            delegate.endEncodeComposite(desc)
        }

        override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) {
            delegate.encodeBooleanElement(desc, index, value)
        }

        override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
            delegate.encodeByteElement(desc, index, value)
        }

        override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) {
            delegate.encodeShortElement(desc, index, value)
        }

        override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
            delegate.encodeIntElement(desc, index, value)
        }

        override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
            delegate.encodeLongElement(desc, index, value)
        }

        override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) {
            delegate.encodeFloatElement(desc, index, value)
        }

        override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) {
            delegate.encodeDoubleElement(desc, index, value)
        }

        override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) {
            delegate.encodeCharElement(desc, index, value)
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            delegate.encodeStringElement(desc, index, value)
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
            delegate.encodeUnitElement(desc, index)
        }

        override fun encodeUnknownValue(desc: SerialDescriptor, index: Int, value: Any?) {
            delegate.encodeUnknownValue(desc, index, value)
        }

    }

    /** Writer that does not actually write an outer tag unless a childName is specified */
    private inner class ListEncoder(parentDesc: SerialDescriptor, elementIndex: Int, desc: SerialDescriptor) :
            TagEncoder(parentDesc, elementIndex, desc, deferring = false), XML.XmlOutput {

        val childrenName = parentDesc.getElementAnnotations(elementIndex).getChildName()

        override fun defer(index: Int, deferred: CompositeEncoder.() -> Unit) {
            deferred()
        }

        override fun writeBegin() {
            if (childrenName != null) { // Do the default thing if the children name has been specified
                super.writeBegin()
                val childName = childrenName
                val tagName = tagSerialName

                //declare namespaces on the parent rather than the children of the list.
                if (tagName.prefix != childName.prefix && target.getNamespaceUri(
                                childName.prefix) != childName.namespaceURI) {
                    target.namespaceAttr(childName.prefix, childName.namespaceURI)
                }

            }

        }

        override fun <T> encodeSerializableElement(desc: SerialDescriptor,
                                                   index: Int,
                                                   strategy: SerializationStrategy<T>,
                                                   value: T) {
            if (childrenName != null) {
                strategy.serialize(RenamedEncoder(childrenName, desc, index), value)
            } else { // Use the outer decriptor and element index
                strategy.serialize(XmlEncoder(parentDesc, elementIndex), value)
            }
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
            if (childrenName != null) {
                RenamedEncoder(childrenName, desc, index).encodeUnit()
            } else {
                target.smartStartTag(tagSerialName) { } // Empty body to automatically get end tag
            }
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            if (index > 0) {
                if (childrenName != null) {
                    RenamedEncoder(childrenName, desc, index).encodeString(value)
                } else { // The first element is the element count
                    // This must be as a tag with text content as we have a list, attributes cannot represent that
                    target.smartStartTag(tagSerialName) { text(value) }
                }
            }
        }

        override fun endEncodeComposite(desc: SerialDescriptor) {
            if (childrenName != null) {
                super.endEncodeComposite(desc)
            }
        }
    }

    internal fun SerialDescriptor.requestedName(index: Int): QName {
        getElementAnnotations(index).firstOrNull<XmlSerialName>()?.run { return toQName() }
        when (outputKind(index)) {
            OutputKind.Attribute -> { // Attribute will take name from use
                val elementDesc = getElementDescriptor(index)
                elementDesc.getEntityAnnotations().firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                return getElementName(index).toQname()
            }
            OutputKind.Text      -> return getElementName(index).toQname() // Will be ignored anyway
            else                 -> { // Not an attribute, will take name from type
                if (elementsCount > 0) {
                    val elementDesc = getElementDescriptor(index)
                    elementDesc.getEntityAnnotations().firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                    // elementDesc.name is normally the type name. We don't want dotted names anyway so strip those
                    if (elementDesc.extKind is PrimitiveKind) {
                        return getElementName(index).toQname()
                    } else {
                        return elementDesc.name.substringAfterLast('.').toQname()
                    }
                } else if (index == 0) { // We are in a list or something that has a confused descriptor
                    return QName(getElementName(0))
                } else { // index >0
                    val elementDesc = getElementDescriptor(1)
                    if (elementDesc.extKind is PrimitiveKind) {
                        return getElementName(index).toQname()
                    } else {
                        elementDesc.getEntityAnnotations().firstOrNull<XmlSerialName>()?.let { return it.toQName() }
                        // elementDesc.name is normally the type name. We don't want dotted names anyway so strip those
                        return elementDesc.name.substringAfterLast('.').toQname()
                    }
                }
            }
        }
    }

    internal fun SerialDescriptor.requestedChildName(index: Int): QName? {
        return getElementAnnotations(index).firstOrNull<XmlChildrenName>()?.toQName()
    }
}

private fun SerialDescriptor.outputKind(index: Int): OutputKind {
    // The children of these are always elements
    when (extKind) {
        StructureKind.LIST,
        StructureKind.MAP,
        UnionKind.POLYMORPHIC,
        UnionKind.SEALED -> return OutputKind.Element
    }
    if (index < associatedFieldsCount) {// This can be false for lists, they are always elements anyway
        for (annotation in getElementAnnotations(index)) {
            when (annotation) {
                is XmlChildrenName -> return OutputKind.Element
                is XmlElement      -> return if (annotation.value) OutputKind.Element else OutputKind.Attribute
                is XmlValue        -> if (annotation.value) return OutputKind.Text
            }
        }
    }
    // Lists are always elements
    val elementDesc = getElementDescriptor(index.coerceAtMost(elementsCount - 1))
    if (elementDesc.elementsCount > 1) return OutputKind.Element

    return when (elementDesc.extKind) {
        is PrimitiveKind -> OutputKind.Attribute
        else             -> OutputKind.Element
    }
}