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

package nl.adaptivity.serialutil

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

fun <T : Any> KSerializer<T?>.nonNullSerializer(): KSerializer<T> {
    @Suppress("UNCHECKED_CAST")
    return (this as DeserializationStrategy<T>).nonNullSerializer() as KSerializer<T>
}

@OptIn(ExperimentalSerializationApi::class)
fun <T : Any> DeserializationStrategy<T?>.nonNullSerializer(): DeserializationStrategy<T> {
    @Suppress("UNCHECKED_CAST")
    if (!descriptor.isNullable) return this as KSerializer<T>

    val canary = SerializerCanary()
    try {
        deserialize(canary)
    } catch (e: SerializerCanary.FinishedException) {
        @Suppress("UNCHECKED_CAST")
        return canary.serializer as KSerializer<T>
    }
    error("Expected to exit through canary")
}

private class SerializerCanary : Decoder {
    var serializer: DeserializationStrategy<*>? = null

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = TODO("not implemented")

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
        throw UnsupportedOperationException()
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

    internal class FinishedException() : RuntimeException()
}
