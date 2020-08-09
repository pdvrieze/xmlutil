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

internal open class XmlEncoderBase internal constructor(
    context: SerialModule,
    config: XmlConfig,
    val target: XmlWriter
                                                       ) : XmlCodecBase(context, config) {

    override val namespaceContext: NamespaceContext
        get() = target.namespaceContext

    /**
     * Encoder class for all primitives (except for initial values). It does not handle attributes. This does not
     * implement XmlOutput as
     */
    internal open inner class XmlEncoder(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        val serializier: SerializationStrategy<*>,
        xmlDescriptor: XmlDescriptor,
        childDesc: SerialDescriptor? = serializier.descriptor
                                        ) :
        XmlCodec<XmlDescriptor>(xmlDescriptor, parentNamespace, parentDesc, elementIndex, childDesc), Encoder, XML.XmlOutput {

        override val target: XmlWriter get() = this@XmlEncoderBase.target

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
            val defaultValue = (xmlDescriptor as XmlValueDescriptor).default
            if (value == defaultValue) return

            when (xmlDescriptor.outputKind) {
                OutputKind.Element   -> { // This may occur with list values.
                    target.smartStartTag(serialName) { target.text(value) }
                }
                OutputKind.Attribute -> {
                    target.attribute(serialName.namespaceURI, serialName.localPart, serialName.prefix, value)
                }
                OutputKind.Mixed,
                OutputKind.Text      -> target.text(value)
            }
        }

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            // TODO allow policy to determine actually used constant, including @XmlSerialName with or without prefix/ns
            encodeString(enumDescriptor.getElementName(index))
        }

        override fun encodeNotNullMark() {
            // Not null is presence, no mark needed
        }

        override fun encodeUnit() {
            when (xmlDescriptor.outputKind) {
                OutputKind.Attribute -> target.writeAttribute(serialName, serialName.localPart)
                OutputKind.Mixed,
                OutputKind.Element   -> target.smartStartTag(serialName) { /*No content*/ }
                OutputKind.Text      -> target.text("Unit")
            }

        }

        override fun encodeNull() {
            // Null is absence, no mark needed
        }

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            serializer.serialize(this, value)
        }

        override fun beginStructure(
            descriptor: SerialDescriptor,
            vararg typeSerializers: KSerializer<*>
                                   ): CompositeEncoder {

            return beginEncodeCompositeImpl(
                parentNamespace,
                parentDesc,
                elementIndex,
                serializier,
                xmlDescriptor
                                           )
        }
    }

    fun beginEncodeCompositeImpl(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        serializer: SerializationStrategy<*>,
        xmlDescriptor: XmlDescriptor
                                ): CompositeEncoder {
        val isMixed = xmlDescriptor.outputKind == OutputKind.Mixed
        return when (xmlDescriptor.serialKind) {
            is PrimitiveKind    -> throw AssertionError("A primitive is not a composite")

            UnionKind.CONTEXTUAL, // TODO handle contextual in a more elegant way
            StructureKind.MAP,
            StructureKind.CLASS -> {
                TagEncoder(
                    parentNamespace,
                    parentDesc,
                    elementIndex,
                    serializer,
                    xmlDescriptor
                          ).apply { writeBegin() }
            }

            StructureKind.LIST  -> ListEncoder(
                parentNamespace,
                parentDesc,
                elementIndex,
                serializer,
                xmlDescriptor as XmlListDescriptor,
                isMixed
                                              ).apply { writeBegin() }

            StructureKind.OBJECT,
            UnionKind.ENUM_KIND -> TagEncoder(
                parentNamespace,
                parentDesc,
                elementIndex,
                serializer,
                xmlDescriptor
                                             ).apply { writeBegin() }
            is PolymorphicKind  ->
                PolymorphicEncoder(
                    parentNamespace,
                    parentDesc,
                    elementIndex,
                    serializer,
                    xmlDescriptor as XmlPolymorphicDescriptor
                                  ).apply { writeBegin() }
        }
    }

    internal open inner class TagEncoder<D: XmlDescriptor>(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        val serializer: SerializationStrategy<*>,
        xmlDescriptor: D,
        private var deferring: Boolean = true
                                                          ) :
        XmlTagCodec<D>(parentDesc, elementIndex, serializer.descriptor, parentNamespace, xmlDescriptor), CompositeEncoder, XML.XmlOutput {

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
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val encoder = XmlEncoder(
                serialName.toNamespace(),
                this@TagEncoder.desc,
                index,
                serializer,
                xmlDescriptor.getChildDescriptor(index, serializer)
                                    )
            defer(index, serializer.descriptor) { serializer.serialize(encoder, value) }
        }

        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
            return descriptor.getElementAnnotations(index).none { it is XmlDefault }
        }

        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            encodeStringElement(descriptor, index, "kotlin.Unit")
        }

        final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            encodeStringElement(descriptor, index, value.toString())
        }

        final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
            encodeStringElement(descriptor, index, value.toString())
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

        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
                                                                ) {
            if (value != null) {
                encodeSerializableElement(descriptor, index, serializer, value)
            }
            // Null is the absense of values, no need to do more
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            val defaultValue = descriptor.getElementAnnotations(index).firstOrNull<XmlDefault>()?.value
            if (value == defaultValue) return

            val kind = descriptor.outputKind(index, null)
            val requestedName = xmlDescriptor.getChildDescriptor(index, String.serializer()).name
            when (kind) {
                OutputKind.Element   -> defer(index, null) { target.smartStartTag(requestedName) { text(value) } }
                OutputKind.Attribute -> smartWriteAttribute(requestedName, value)
                OutputKind.Mixed,
                OutputKind.Text      -> defer(index, null) { target.text(value) }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            deferring = false
            for (deferred in deferredBuffer) {
                deferred()
            }
            target.endTag(serialName)
        }

        open fun doWriteAttribute(name: QName, value: String) {

            val actualAttrName: QName = when {
                name.getNamespaceURI().isEmpty() ||
                        (serialName.getNamespaceURI() == name.getNamespaceURI() &&
                                (serialName.prefix == name.prefix))
                     -> QName(name.localPart) // Breaks in android otherwise

                else -> name
            }

            smartWriteAttribute(actualAttrName, value)
        }


    }

    /**
     * Helper function that ensures writing the namespace attribute if needed.
     */
    private fun smartWriteAttribute(name: QName, value: String) {
        val needsNsAttr = name.namespaceURI.isNotEmpty() &&
                target.getPrefix(name.namespaceURI) == null

        if (needsNsAttr) target.namespaceAttr(name.prefix, name.namespaceURI)

        target.writeAttribute(name, value)
    }

    internal inner class PolymorphicEncoder(
        parentNamespace: Namespace,
        parentDesc: SerialDescriptor,
        elementIndex: Int,
        serializer: SerializationStrategy<*>,
        xmlDescriptor: XmlPolymorphicDescriptor
                                           ) :
        TagEncoder<XmlPolymorphicDescriptor>(parentNamespace, parentDesc, elementIndex, serializer, xmlDescriptor, false), XML.XmlOutput {
        val isMixed get() = xmlDescriptor.outputKind == OutputKind.Mixed

        override fun defer(index: Int, childDesc: SerialDescriptor?, deferred: CompositeEncoder.() -> Unit) {
            deferred()
        }

        override fun writeBegin() {
            // Don't write a tag if we are transparent
            if (xmlDescriptor.polyInfo == null) super.writeBegin()
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            if (xmlDescriptor.polyInfo != null) {
                if (index != 0) {
                    if (isMixed) {
                        target.text(value)
                    } else {
                        target.smartStartTag(serialName) { text(value) }
                    }
                }
            } else {
                if (index == 0) { // The attribute name is renamed to type and forced to attribute
                    assert(xmlDescriptor.parentSerialName == parentDesc.serialName) {
                        "parentSerialName ('${xmlDescriptor.parentSerialName}') should match the one in the encoder: '${parentDesc.serialName}'"
                    }
                    doWriteAttribute(QName("type"), value.tryShortenTypeName(xmlDescriptor.parentSerialName))
                } else {
                    super.encodeStringElement(descriptor, index, value)
                }

            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            if (xmlDescriptor.polyInfo != null) {
                assert(index > 0) // the first element is the type
                if (isMixed && serializer.descriptor.kind is PrimitiveKind) {
                    serializer.serialize(
                        XmlEncoder(
                            parentNamespace,
                            parentDesc,
                            elementIndex,
                            serializer,
                            xmlDescriptor.getChildDescriptor(index, serializer)
                                  ), value
                                        )
                } else {
                    // The name has been set when the type was "written"
                    val encoder = XmlEncoder(
                        parentNamespace,
                        descriptor,
                        index,
                        serializer,
                        xmlDescriptor.getChildDescriptor(index, serializer)
                                            )
                    serializer.serialize(encoder, value)
                }
            } else {
                val encoder = XmlEncoder(
//                    QName("value"),
                    parentNamespace,
                    descriptor,
                    index,
                    serializer,
                    xmlDescriptor.getChildDescriptor(index, serializer)
                                        )
                super.defer(index, serializer.descriptor) { serializer.serialize(encoder, value) }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Don't write anything if we're transparent
            if (xmlDescriptor.polyInfo == null) {
                super.endStructure(descriptor)
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
        xmlDescriptor: XmlListDescriptor,
        val isMixed: Boolean
                                    ) :
        TagEncoder<XmlListDescriptor>(
            parentNamespace,
            parentDesc,
            elementIndex,
            serializer,
            deferring = false,
            xmlDescriptor = xmlDescriptor
                                 ), XML.XmlOutput {

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
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
                                                  ) {
            val childDescriptor = xmlDescriptor.getChildDescriptor(0, serializer)
            if (childrenName != null) {
                serializer.serialize(
                    XmlEncoder(/*childrenName, */serialName.toNamespace(), descriptor, 0, serializer, childDescriptor),
                    value
                                    )
            } else { // Use the outer decriptor and element index
                serializer.serialize(
                    XmlEncoder(serialName.toNamespace(), parentDesc, elementIndex, serializer, childDescriptor), value
                                    )
            }
        }

        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            if (childrenName != null) {
                XmlEncoder(
                    serialName.toNamespace(),
                    descriptor,
                    index,
                    UnitSerializer(),
                    xmlDescriptor.getChildDescriptor(index, UnitSerializer())
                          ).encodeUnit()
            } else {
                target.smartStartTag(serialName) { } // Empty body to automatically get end tag
            }
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            if (index > 0) {
                when {
                    childrenName != null -> {
                        XmlEncoder(
                            serialName.toNamespace(),
                            descriptor,
                            index,
                            String.serializer(),
                            xmlDescriptor.getChildDescriptor(index, String.serializer())
                                  ).encodeString(
                            value
                                                )
                    }
                    isMixed              -> { // Mixed will be a list of strings and other stuff
                        target.text(value)
                    }
                    else                 -> { // The first element is the element count
                        // This must be as a tag with text content as we have a list, attributes cannot represent that
                        target.smartStartTag(serialName) { text(value) }
                    }
                }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (childrenName != null) {
                super.endStructure(descriptor)
            }
        }
    }
}
