/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xmlschema.test

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.structure.*
import javax.xml.namespace.QName

class AttributeViewer(
    val serializersModule: SerializersModule = EmptySerializersModule(),
) {

    private val _elementInfos = mutableMapOf<String, Array<ElementInfo>>()

    fun structInfo(descriptor: XmlDescriptor): Array<ElementInfo> {
        val info = _elementInfos.getOrPut(
            descriptor.serialDescriptor.serialName,
            {
                Array(
                    maxOf(
                        descriptor.elementsCount,
                        descriptor.serialDescriptor.elementsCount
                    )
                ) { ElementInfo(descriptor.getElementDescriptor(it)) }
            })
        return info
    }

    fun elementInfo(descriptor: XmlDescriptor, index: Int): ElementInfo {
        return structInfo(descriptor)[index]
    }

    fun <T> encode(serializer: KSerializer<T>, value: T, rootDescriptor: XmlRootDescriptor) {
        val elementDescriptor = rootDescriptor.getElementDescriptor(0)
        val encoder = AttrEncoder(elementDescriptor, ElementInfo(elementDescriptor))
        encoder.encodeSerializableValue(serializer, value)
    }

    inner class AttrEncoder(val xmlDescriptor: XmlDescriptor, val elementInfo: ElementInfo) : Encoder {
        override val serializersModule: SerializersModule
            get() = this@AttributeViewer.serializersModule

        private fun recordElementPresent() {
            if (!elementInfo.seen) {
                elementInfo.seen = true
            }
        }

        @ExperimentalSerializationApi
        override fun encodeNull() {
            if (!elementInfo.hasBeenAbsent) {
                elementInfo.hasBeenAbsent = true
            }
        }

        override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
            elementInfo.hasBeenAbsent = elementInfo.hasBeenAbsent || collectionSize == 0
            return super.beginCollection(descriptor, collectionSize)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            return when (xmlDescriptor) {
                is XmlPolymorphicDescriptor -> {
                    recordElementPresent()
                    PolymorphicEncoder(xmlDescriptor)
                }
                is XmlListDescriptor -> ListEncoder(xmlDescriptor, elementInfo)
                else -> {
                    recordElementPresent()
                    CompositeAttrEncoder(xmlDescriptor)
                }
            }

        }

        override fun encodeInline(descriptor: SerialDescriptor): Encoder {
            recordElementPresent()
            val childDescriptor= when {
                xmlDescriptor.serialDescriptor == descriptor ->
                    xmlDescriptor.getElementDescriptor(0)

                else -> xmlDescriptor
            }

            return AttrEncoder(childDescriptor, this.elementInfo)
        }

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            val effectiveSerializer = serializer.getEffectiveSerializer(xmlDescriptor)

            effectiveSerializer.serialize(this, value)
        }

        override fun encodeString(value: String) = recordElementPresent()

        override fun encodeBoolean(value: Boolean) = recordElementPresent()

        override fun encodeByte(value: Byte) = recordElementPresent()

        override fun encodeChar(value: Char) = recordElementPresent()

        override fun encodeDouble(value: Double) = recordElementPresent()

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = recordElementPresent()

        override fun encodeFloat(value: Float) = recordElementPresent()

        override fun encodeInt(value: Int) = recordElementPresent()

        override fun encodeLong(value: Long) = recordElementPresent()

        override fun encodeShort(value: Short) = recordElementPresent()
    }

    inner abstract class AbstractCompositeEncoder : CompositeEncoder {
        abstract val xmlDescriptor: XmlDescriptor

        override val serializersModule: SerializersModule
            get() = this@AttributeViewer.serializersModule

        abstract fun encodePrimiteElement(descriptor: SerialDescriptor, index: Int, value: Any)

        override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            encodePrimiteElement(descriptor, index, value)
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            encodePrimiteElement(descriptor, index, value)
        }
    }

    inner class PolymorphicEncoder(
        override val xmlDescriptor: XmlPolymorphicDescriptor
    ) : AbstractCompositeEncoder() {

        val structInfo = _elementInfos.getOrPut(
            xmlDescriptor.serialDescriptor.serialName,
            {
                xmlDescriptor.polyInfo.values.map { desc -> ElementInfo(desc) }.toTypedArray()
            })

        var type: String? = null

        fun elementInfo(childDescriptor: XmlDescriptor): ElementInfo {
            return structInfo.first { it.name == childDescriptor.tagName }
        }

        override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
            require(index == 1)
            val childDescriptor = xmlDescriptor.getPolymorphicDescriptor(requireNotNull(type))
            return AttrEncoder(childDescriptor, elementInfo(childDescriptor))
        }

        @ExperimentalSerializationApi
        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            throw UnsupportedOperationException("Not valid")
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            require(index == 1)
            val childDescriptor = xmlDescriptor.getPolymorphicDescriptor(requireNotNull(type))
            val encoder = AttrEncoder(childDescriptor, elementInfo(childDescriptor))
            encoder.encodeSerializableValue(serializer, value)
        }

        override fun endStructure(descriptor: SerialDescriptor) {}

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            if (index ==0) {
                type = value
            }
        }

        override fun encodePrimiteElement(descriptor: SerialDescriptor, index: Int, value: Any) {
            /** Ignore, this is just the type name */
        }
    }

    inner class CompositeAttrEncoder(
        override val xmlDescriptor: XmlDescriptor
    ) : AbstractCompositeEncoder() {

        private val baseStructInfo = structInfo(xmlDescriptor)

        val structInfo: Array<ElementInfo> = Array(baseStructInfo.size) { ElementInfo(baseStructInfo[it].name) }

        private fun recordElementPresent(idx: Int) {
            val elementInfo = structInfo[idx]
            elementInfo.seen = true
        }

        override fun encodePrimiteElement(descriptor: SerialDescriptor, index: Int, value: Any) {
            recordElementPresent(index)
        }

        override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
            return AttrEncoder(xmlDescriptor.getElementDescriptor(index), structInfo[index])
        }

        @ExperimentalSerializationApi
        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            if (value == null) {
                val elementInfo = structInfo[index]
                val childDescriptor = xmlDescriptor.getElementDescriptor(index)

                val encoder = AttrEncoder(childDescriptor, elementInfo)
                encoder.encodeNull()
            } else {
                encodeSerializableElement(descriptor, index, serializer, value)
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            val encoder = when (descriptor.kind) {
                StructureKind.LIST -> {
                    val elementInfo = structInfo[0]
                    val childDescriptor = xmlDescriptor.getElementDescriptor(0)
                    AttrEncoder(childDescriptor, elementInfo)
                }
                StructureKind.MAP -> {
                    val childDescriptor = xmlDescriptor.getElementDescriptor(index % 2)
                    if (childDescriptor.outputKind != OutputKind.Element) return
                    if (index %2 ==1) {
                        val elementInfo = structInfo[1]
                        AttrEncoder(childDescriptor, elementInfo)
                    } else return
                }
                else -> {
                    val elementInfo = structInfo[index]
                    val childDescriptor = xmlDescriptor.getElementDescriptor(index)
                    AttrEncoder(childDescriptor, elementInfo)
                }
            }

            val effectiveSerializer = serializer.getEffectiveSerializer(xmlDescriptor)
            encoder.encodeSerializableValue(effectiveSerializer, value)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            for (i in 0 until xmlDescriptor.elementsCount) {
                val info = structInfo[i]
                if (!(structInfo[i].seen || info.hasBeenAbsent)) {
                    info.hasBeenAbsent = true
                }
                baseStructInfo[i] += info
            }
        }
    }

    inner class ListEncoder(override val xmlDescriptor: XmlListLikeDescriptor, val elementInfo: ElementInfo): AbstractCompositeEncoder() {
        var seenHere = false
        override fun encodePrimiteElement(descriptor: SerialDescriptor, index: Int, value: Any) {
            elementInfo.seen = true
            seenHere = true
        }

        override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
            return AttrEncoder(xmlDescriptor.getElementDescriptor(0), elementInfo)
        }

        @ExperimentalSerializationApi
        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            if (value == null) {
                elementInfo.hasBeenAbsent = true
            } else {
                encodeSerializableElement(descriptor, index, serializer, value)
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            val childDescriptor = xmlDescriptor.getElementDescriptor(0)
            val encoder = AttrEncoder(childDescriptor, elementInfo)
            val effectiveSerializer = serializer.getEffectiveSerializer(childDescriptor)
            encoder.encodeSerializableValue(effectiveSerializer, value)
            seenHere = true
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            if (! seenHere) elementInfo.hasBeenAbsent = true
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun <T> SerializationStrategy<T>.getEffectiveSerializer(
        elementDescriptor: XmlDescriptor
    ): SerializationStrategy<T> {
        val overriddenSerializer = elementDescriptor.overriddenSerializer
        @Suppress("UNCHECKED_CAST")
        return when {
            this.javaClass.name == "nl.adaptivity.xmlutil.serialization.impl.XmlQNameSerializer" ||
                    this.descriptor.serialName == "javax.xml.namespace.QName"
            -> SimpleQNameSerializer

//            overriddenSerializer != null
//            -> overriddenSerializer

            else -> this
        } as SerializationStrategy<T>
    }
}

object SimpleQNameSerializer : KSerializer<QName> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("nl.adaptivity.xmlutil.serialization.test.QName", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): QName {
        throw UnsupportedOperationException("Deserializing not supported")
    }

    override fun serialize(encoder: Encoder, value: QName) {
        encoder.encodeString(value.localPart) // No need to do special
    }
}
