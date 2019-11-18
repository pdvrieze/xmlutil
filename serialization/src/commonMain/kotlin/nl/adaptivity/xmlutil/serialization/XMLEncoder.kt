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
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.UnitSerializer
import kotlinx.serialization.modules.SerialModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.canary.getBaseClass
import nl.adaptivity.xmlutil.serialization.impl.ChildCollector

internal open class XmlEncoderBase internal constructor(
    context: SerialModule,
    config: XmlConfig,
    val target: XmlWriter
                                                       ) : XmlCodecBase(context, config) {

    /**
     * Encoder class for all primitives (except for initial values). It does not handle attributes. This does not
     * implement XmlOutput as
     */
    internal open inner class XmlEncoder(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        val serializier: SerializationStrategy<*>,
        childDesc: SerialDescriptor? = serializier.descriptor
                                        ) :
        XmlCodec(parentNamespace, parentDesc, elementIndex, childDesc), Encoder {

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
                OutputKind.Element   -> target.smartStartTag(serialName) { /*No content*/ }
                else                 -> target.text("Unit")
            }

        }

        override fun encodeNull() {
            // Null is absence, no mark needed
        }

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            serializer.serialize(this, value)
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            return beginEncodeCompositeImpl(parentNamespace, parentDesc, elementIndex, serializier, typeParams)
        }
    }

    fun beginEncodeCompositeImpl(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        serializer: SerializationStrategy<*>,
        otherSerializers: Array<out KSerializer<*>>
                                ): CompositeEncoder {
        val desc = serializer.descriptor
        return when (desc.kind) {
            is PrimitiveKind      -> throw AssertionError("A primitive is not a composite")

            StructureKind.MAP,
            StructureKind.CLASS   -> TagEncoder(
                parentNamespace,
                parentDesc,
                elementIndex,
                serializer
                                               ).apply { writeBegin() }

            StructureKind.LIST    -> ListEncoder(
                parentNamespace,
                parentDesc,
                elementIndex,
                serializer,
                otherSerializers.singleOrNull()
                                                ).apply { writeBegin() }

            UnionKind.OBJECT,
            UnionKind.ENUM_KIND   -> TagEncoder(
                parentNamespace,
                parentDesc,
                elementIndex,
                serializer
                                               ).apply { writeBegin() }

            UnionKind.SEALED,
            UnionKind.POLYMORPHIC ->
                PolymorphicEncoder(parentNamespace, parentDesc, elementIndex, serializer).apply { writeBegin() }
        }
    }

    internal open inner class TagEncoder(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        val serializer: SerializationStrategy<*>,
        private var deferring: Boolean = true
                                        ) :
        XmlTagCodec(parentDesc, elementIndex, serializer.descriptor, parentNamespace), CompositeEncoder, XML.XmlOutput {

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
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val encoder = XmlEncoder(serialName.toNamespace(), this@TagEncoder.desc, index, serializer)
            defer(index, serializer.descriptor) { serializer.serialize(encoder, value) }
        }

        override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean {
            return desc.getElementAnnotations(index).none { it is XmlDefault }
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
            encodeStringElement(desc, index, "kotlin.Unit")
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
            throw XmlSerialException("Unable to serialize element ${desc.getElementName(index)}: $value")
        }

        override fun <T : Any> encodeNullableSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
                                                                ) {
            if (value != null) {
                encodeSerializableElement(desc, index, serializer, value)
            }
            // Null is the absense of values, no need to do more
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            val defaultValue = desc.getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
            if (value == defaultValue) return

            val kind = desc.outputKind(index, null)
            val requestedName = desc.requestedName(serialName.toNamespace(), index, null)
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

    internal inner class RenamedEncoder(
        override val serialName: QName,
        parentNamespace: Namespace,
        desc: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<*>
                                       ) :
        XmlEncoder(parentNamespace, desc, index, serializer) {

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            return RenamedTagEncoder(
                serialName,
                parentNamespace,
                parentDesc,
                elementIndex,
                serializier
                                    ).apply { writeBegin() }
        }
    }

    internal inner class RenamedTagEncoder(
        override val serialName: QName,
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        serializer: SerializationStrategy<*>
                                         ) :
        TagEncoder(parentNamespace, parentDesc, elementIndex, serializer, true)

    internal inner class PolymorphicEncoder(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        serializer: SerializationStrategy<*>
                                          ) :
        TagEncoder(parentNamespace, parentDesc, elementIndex, serializer, false), XML.XmlOutput {

        val polyChildren: XmlNameMap?

        init {
            val xmlPolyChildren = parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlPolyChildren>()
            polyChildren =
                when {
                    xmlPolyChildren != null
                         -> polyInfo(
                        parentDesc.requestedName(parentNamespace, elementIndex, null),
                        xmlPolyChildren.value
                                    )
                    config.autoPolymorphic &&
                    serializer is PolymorphicSerializer<*>
                         -> {
                        val baseClass = serializer.baseClass
                        val childCollector = ChildCollector(baseClass)
                        context.dumpTo(childCollector)
                        childCollector.getPolyInfo(
                            this,
                            parentDesc.requestedName(parentNamespace, elementIndex, null)
                                                  )
                    }

                    else -> null
                }
        }

        override var serialName: QName = when (polyChildren) {
            null -> parentDesc.getElementAnnotations(elementIndex).firstOrNull<XmlSerialName>()?.toQName()
                ?: parentDesc.getElementName(elementIndex).toQname(parentNamespace)
            else -> parentDesc.requestedName(parentNamespace, elementIndex, parentDesc.getElementDescriptor(elementIndex))
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
                    doWriteAttribute(QName("type"), value.tryShortenTypeName(parentDesc.name))
                } else {
                    super.encodeStringElement(desc, index, value)
                }

            }
        }

        override fun <T> encodeSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            if (polyChildren != null) {
                assert(index > 0) // the first element is the type
                // The name has been set when the type was "written"
                val encoder = RenamedEncoder(serialName, parentNamespace, desc, index, serializer)
                serializer.serialize(encoder, value)
            } else {
                val encoder = RenamedEncoder(QName("value"), parentNamespace, desc, index, serializer)
                super.defer(index, serializer.descriptor) { serializer.serialize(encoder, value) }
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
    internal inner class ListEncoder(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        serializer: SerializationStrategy<*>,
        val typeParam: KSerializer<*>?
                                   ) :
        TagEncoder(parentNamespace, parentDesc, elementIndex, serializer, deferring = false), XML.XmlOutput {

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
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            if (childrenName != null) {
                serializer.serialize(
                    RenamedEncoder(childrenName, serialName.toNamespace(), desc, index, serializer),
                    value
                                    )
            } else { // Use the outer decriptor and element index
                serializer.serialize(
                    XmlEncoder(serialName.toNamespace(), parentDesc, elementIndex, serializer), value
                                    )
            }
        }

        override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {
            if (childrenName != null) {
                RenamedEncoder(childrenName, serialName.toNamespace(), desc, index, UnitSerializer).encodeUnit()
            } else {
                target.smartStartTag(serialName) { } // Empty body to automatically get end tag
            }
        }

        override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
            if (index > 0) {
                if (childrenName != null) {
                    RenamedEncoder(childrenName, serialName.toNamespace(), desc, index, StringSerializer).encodeString(
                        value
                                                                                                                      )
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
