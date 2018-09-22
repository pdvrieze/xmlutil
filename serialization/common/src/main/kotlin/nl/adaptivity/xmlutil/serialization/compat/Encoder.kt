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

package nl.adaptivity.xmlutil.serialization.compat

import kotlinx.serialization.SerializationException

interface Encoder {
    fun encodeNotNullMark() = encodeUnknownValue(null)
    fun encodeNull() = encodeUnknownValue(null)

    fun encodeUnit() = encodeUnknownValue(Unit)
    fun encodeBoolean(value: Boolean) = encodeUnknownValue(value)
    fun encodeByte(value: Byte) = encodeUnknownValue(value)
    fun encodeShort(value: Short) = encodeUnknownValue(value)
    fun encodeInt(value: Int) = encodeUnknownValue(value)
    fun encodeLong(value: Long) = encodeUnknownValue(value)
    fun encodeFloat(value: Float) = encodeUnknownValue(value)
    fun encodeDouble(value: Double) = encodeUnknownValue(value)
    fun encodeChar(value: Char) = encodeUnknownValue(value)
    fun encodeString(value: String) = encodeUnknownValue(value)

    // Encoder is allowed to override this default implementation
    fun <T : Any?> encodeSerializable(strategy: SerializationStrategy<T>, value: T) =
            strategy.serialize(this, value)

    fun encodeUnknownValue(value: Any?): Unit = throw SerializationException("Value serialization not supported for $value")

    fun beginEncodeComposite(desc: SerialDescriptor): CompositeEncoder
}

interface CompositeEncoder {
    // delimiter
    fun endEncodeComposite(desc: SerialDescriptor)

    // Invoked if an element equals its default value, making it possible to omit it in the output stream
    fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean = true

    fun encodeUnitElement(desc: SerialDescriptor, index: Int) = encodeUnknownValue(desc, index, Unit)
    fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) = encodeUnknownValue(desc, index, value)
    fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) = encodeUnknownValue(desc, index, value)
    fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) = encodeUnknownValue(desc, index, value)
    fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) = encodeUnknownValue(desc, index, value)
    fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) = encodeUnknownValue(desc, index, value)
    fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) = encodeUnknownValue(desc, index, value)
    fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) = encodeUnknownValue(desc, index, value)
    fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) = encodeUnknownValue(desc, index, value)
    fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) = encodeUnknownValue(desc, index, value)

    fun <T> encodeSerializableElement(desc: SerialDescriptor, index: Int, strategy: SerializationStrategy<T>, value: T)

    fun encodeUnknownValue(desc: SerialDescriptor, index: Int, value: Any?): Unit = throw SerializationException("Value serialization not supported for element ${desc.getElementName(index)} with value $value")

    /**
     * Temporary interface due to lack of overridability of nullable values
     */
    fun encodeNullElement(desc: SerialDescriptor, index: Int) = encodeUnknownValue(desc, index, null)
}