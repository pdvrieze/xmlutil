/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class SimpleStringEncoder(override val serializersModule: SerializersModule = EmptySerializersModule): Encoder, List<String> {
    private val _strings = mutableListOf<String>()
    val strings: List<String> get() = _strings

    override val size: Int get() = _strings.size

    override fun contains(element: String): Boolean = _strings.contains(element)

    override fun containsAll(elements: Collection<String>): Boolean = _strings.containsAll(elements)

    override operator fun get(index: Int): String = _strings.get(index)

    override fun indexOf(element: String): Int = _strings.indexOf(element)

    override fun isEmpty(): Boolean = _strings.isEmpty()

    override fun iterator(): Iterator<String> = _strings.iterator()

    override fun lastIndexOf(element: String): Int = _strings.lastIndexOf(element)

    override fun listIterator(): ListIterator<String> = _strings.listIterator()

    override fun listIterator(index: Int): ListIterator<String> = _strings.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<String> = _strings.subList(fromIndex, toIndex)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        return ListEncoder()
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        throw UnsupportedOperationException("SimpleStringEncoder doesn't allow for structures")
    }

    @ExperimentalSerializationApi
    override fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder {
        return this
    }

    override fun encodeString(value: String) {
        _strings.add(value)
    }

    override fun encodeBoolean(value: Boolean) = encodeString(value.toString())

    override fun encodeByte(value: Byte) = encodeString(value.toString())

    override fun encodeChar(value: Char) = encodeString(value.toString())

    override fun encodeDouble(value: Double) = encodeString(value.toString())

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    override fun encodeFloat(value: Float) = encodeString(value.toString())

    override fun encodeInt(value: Int) = encodeString(value.toString())

    override fun encodeLong(value: Long) = encodeString(value.toString())

    @ExperimentalSerializationApi
    override fun encodeNull() = encodeString("null")

    override fun encodeShort(value: Short) = encodeString(value.toString())

    inner class ListEncoder: CompositeEncoder {
        override val serializersModule: SerializersModule
            get() = this@SimpleStringEncoder.serializersModule

        override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
            encodeBoolean(value)
        }

        override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
            encodeByte(value)
        }

        override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
            encodeChar(value)
        }

        override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
            encodeDouble(value)
        }

        override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
            encodeFloat(value)
        }

        @ExperimentalSerializationApi
        override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
            return this@SimpleStringEncoder
        }

        override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            encodeInt(value)
        }

        override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
            encodeLong(value)
        }

        @ExperimentalSerializationApi
        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            throw UnsupportedOperationException("Nullable list elements cannot be in simple string encoders")
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            encodeSerializableValue(serializer, value)
        }

        override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            encodeShort(value)
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            encodeString(value)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // do nothing
        }
    }
}
