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


package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
class SimpleStringDecoder(
    val value: String,
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : Decoder {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = when (descriptor.kind) {
        is StructureKind.OBJECT -> ObjectDecoder(descriptor)
        is PolymorphicKind.SEALED -> SealedDecoder()
        else -> throw SerializationException("Structures are not supported by the simple string decoder: decoding $value")
    }

    private inner class ObjectDecoder(private val descriptor: SerialDescriptor) : AbstractDecoder() {
        override val serializersModule: SerializersModule
            get() = this@SimpleStringDecoder.serializersModule

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return CompositeDecoder.DECODE_DONE
        }
    }

    private inner class SealedDecoder : AbstractDecoder() {
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            val s = decodeString()
            return (0 until descriptor.elementsCount).first { descriptor.getElementName(it) == s }
        }

        override fun decodeString(): String {
            return value
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T {
            return deserializer.deserialize(ObjectDecoder(deserializer.descriptor))
        }

        override val serializersModule: SerializersModule
            get() = this@SimpleStringDecoder.serializersModule
    }

    @ExperimentalSerializationApi
    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder = this

    override fun decodeString(): String = value

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean = false

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing {
        throw UnsupportedOperationException("Can never be null")
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return (0 until enumDescriptor.elementsCount).firstOrNull { enumDescriptor.getElementName(it) == value }
            ?: throw SerializationException("Could not find enum constant for ${enumDescriptor.serialName}.$value")
    }

    override fun decodeBoolean(): Boolean = value.toBoolean()

    override fun decodeByte(): Byte = value.toByte()

    override fun decodeChar(): Char = value.single()

    override fun decodeDouble(): Double = value.toDouble()

    override fun decodeFloat(): Float = value.toFloat()

    override fun decodeInt(): Int = value.toInt()

    override fun decodeLong(): Long = value.toLong()

    override fun decodeShort(): Short = value.toShort()
}

@OptIn(ExperimentalSerializationApi::class)
class SimpleStringListDecoder(
    val values: List<String>,
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : Decoder {
    private var pos: Int = 0

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (descriptor.kind != StructureKind.LIST) throw IllegalArgumentException("Only lists can be decoded")
        return listCompositeDecoder
    }

    @ExperimentalSerializationApi
    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder {
        return this
    }

    override fun decodeBoolean(): Boolean =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeByte(): Byte =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeChar(): Char =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeDouble(): Double =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeFloat(): Float =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeInt(): Int =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeLong(): Long =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeShort(): Short =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    override fun decodeString(): String =
        throw UnsupportedOperationException("List need to be decoded as lists, not elements")

    private val listCompositeDecoder = object : CompositeDecoder {
        override val serializersModule: SerializersModule
            get() = this@SimpleStringListDecoder.serializersModule

        @ExperimentalSerializationApi
        override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
            return SimpleStringDecoder(values[index])
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            return deserializer.deserialize(SimpleStringDecoder(values[index]))
        }

        @ExperimentalSerializationApi
        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            previousValue: T?
        ): T? {
            return decodeSerializableElement(descriptor, index, deserializer, previousValue)
        }

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            return values[index]
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = values.size

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = pos++

        @ExperimentalSerializationApi
        override fun decodeSequentially(): Boolean = true

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
            return decodeStringElement(descriptor, index).toBoolean()
        }

        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
            return decodeStringElement(descriptor, index).toByte()
        }

        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
            return decodeStringElement(descriptor, index).single()
        }

        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
            return decodeStringElement(descriptor, index).toDouble()
        }

        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
            return decodeStringElement(descriptor, index).toFloat()
        }

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
            return decodeStringElement(descriptor, index).toInt()
        }

        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
            return decodeStringElement(descriptor, index).toLong()
        }

        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
            return decodeStringElement(descriptor, index).toShort()
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // does nothing
        }
    }
}
