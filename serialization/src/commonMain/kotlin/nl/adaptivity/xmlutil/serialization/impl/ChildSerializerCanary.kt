/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.serialization.impl

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

internal fun DeserializationStrategy<*>.findChildSerializer(index: Int, serializersModule: SerializersModule): DeserializationStrategy<*> {
    val canary = ChildSerializerCanary(index, serializersModule)
    try {
        deserialize(canary)
    } catch (e: ChildSerializerCanary.FinishedException) {
        return checkNotNull(canary.serializer)
    }
    error("No child serializer found")
}

/**
 * Helper class that allows finding out the deserializer actually used for deserializing a class.
 */
private class ChildSerializerCanary(val index: Int, override val serializersModule: SerializersModule) : Decoder, CompositeDecoder {
    var serializer: DeserializationStrategy<*>? = null
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this

    override fun decodeBoolean(): Boolean {
        check(serializer == null) { "serializer already set" }
        serializer = Boolean.serializer()
        throw FinishedException()
    }

    override fun decodeByte(): Byte {
        check(serializer == null) { "serializer already set" }
        serializer = Byte.serializer()
        throw FinishedException()
    }

    override fun decodeChar(): Char {
        check(serializer == null) { "serializer already set" }
        serializer = Char.serializer()
        throw FinishedException()

    }

    override fun decodeDouble(): Double {
        check(serializer == null) { "serializer already set" }
        serializer = Double.serializer()
        throw FinishedException()
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        throw UnsupportedOperationException()
    }

    override fun decodeFloat(): Float {
        check(serializer == null) { "serializer already set" }
        serializer = Boolean.serializer()
        throw FinishedException()
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        return this
    }

    override fun decodeInt(): Int {
        check(serializer == null) { "serializer already set" }
        serializer = Int.serializer()
        throw FinishedException()
    }

    override fun decodeLong(): Long {
        check(serializer == null) { "serializer already set" }
        serializer = Long.serializer()
        throw FinishedException()
    }

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean {
        return true
    }

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? {
        error("Null should not be decoded in this class")
    }

    override fun decodeShort(): Short {
        check(serializer == null) { "serializer already set" }
        serializer = Short.serializer()
        throw FinishedException()

    }

    override fun decodeString(): String {
        check(serializer == null) { "serializer already set" }
        serializer = String.serializer()
        throw FinishedException()
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableValue(deserializer: DeserializationStrategy<T?>): T? {
        return super.decodeNullableSerializableValue(deserializer)
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        check(serializer == null) { "serializer already set" }
        if (deserializer.descriptor.isNullable) {
            deserializer.deserialize(this)
            check(serializer != null) { "This should have " }
        } else {
            serializer = deserializer as KSerializer<*>
            throw FinishedException()
        }
        return super.decodeSerializableValue(deserializer)
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        return decodeString()
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        return decodeShort()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        if (deserializer.descriptor.kind == SerialKind.CONTEXTUAL) {
            return deserializer.deserialize(this)
        } else {
            check(serializer == null) { "serializer already set" }
            serializer = deserializer
            throw FinishedException()
        }
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        return deserializer.deserialize(this)
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return decodeLong()
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        return decodeInt()
    }

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        return decodeInline(descriptor)
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        return decodeFloat()
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return index
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return decodeDouble()
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return decodeChar()
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return decodeByte()
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return decodeBoolean()
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        error("This should not be reached")
    }

    class FinishedException : RuntimeException()
}
