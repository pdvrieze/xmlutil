/*
 * Copyright (c) 2020.
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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule

internal open class CanaryBase : Decoder,
                                 CompositeDecoder,
                                 Encoder,
                                 CompositeEncoder {

    override val context: SerialModule get() = EmptyModule
    override val updateMode: UpdateMode get() = UpdateMode.BANNED
    lateinit var actualDeserializer: DeserializationStrategy<*>
    lateinit var actualSerializer: SerializationStrategy<*>

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        actualDeserializer = deserializer
        // We're doing nasty stuff here anyway
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
                                              ): T {
        // We're not actually decoding so share the code.
        return decodeSerializableValue(deserializer)
    }

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>
                                                            ): T? {
        return decodeSerializableElement(descriptor, index, deserializer)
    }

    override fun <T : Any> updateNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        old: T?
                                                            ): T? {
        return updateSerializableElement(descriptor, index, deserializer, old)
    }

    override fun <T> updateSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        old: T
                                              ): T {
        return decodeSerializableElement(descriptor, index, deserializer)
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
                                              ) {
        actualSerializer = serializer
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
                                                            ) {
        actualSerializer = serializer
    }

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CanaryBase {
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {}

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        throw UnsupportedOperationException("Not valid here")
    }

    override fun decodeBoolean(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeBoolean(value: Boolean) = throw UnsupportedOperationException("Not valid here")

    override fun decodeByte(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeByte(value: Byte) = throw UnsupportedOperationException("Not valid here")

    override fun decodeChar(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeChar(value: Char) = throw UnsupportedOperationException("Not valid here")

    override fun decodeDouble(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeDouble(value: Double) = throw UnsupportedOperationException("Not valid here")


    override fun decodeEnum(enumDescriptor: SerialDescriptor): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeFloat(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeFloat(value: Float) = throw UnsupportedOperationException("Not valid here")

    override fun decodeInt(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeInt(value: Int) = throw UnsupportedOperationException("Not valid here")

    override fun decodeLong(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeLong(value: Long) = throw UnsupportedOperationException("Not valid here")

    override fun decodeNotNullMark(): Boolean = true

    override fun encodeNull() = throw UnsupportedOperationException("Not valid here")

    override fun decodeNull(): Nothing = throw UnsupportedOperationException("Not valid here")
    override fun decodeShort(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeShort(value: Short) = throw UnsupportedOperationException("Not valid here")

    override fun decodeString(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeString(value: String) = throw UnsupportedOperationException("Not valid here")

    override fun decodeUnit(): Nothing = throw UnsupportedOperationException("Not valid here")

    override fun encodeUnit() = throw UnsupportedOperationException("Not valid here")

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")

    override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int): Nothing =
        throw UnsupportedOperationException("Not valid here")
}