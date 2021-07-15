/*
 * Copyright (c) 2021. 
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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Simple class that decodes a statically provided value (or throws an exception when the types are incorrect)
 */
public class DummyDecoder(public val value: Any?) : Decoder {
    override val serializersModule: SerializersModule get() = EmptySerializersModule

    override fun decodeBoolean(): Boolean = value as Boolean

    override fun decodeByte(): Byte = value as Byte

    override fun decodeChar(): Char = value as Char

    override fun decodeDouble(): Double = value as Double

    override fun decodeFloat(): Float = value as Float

    override fun decodeInt(): Int = value as Int

    override fun decodeLong(): Long = value as Long

    override fun decodeShort(): Short = value as Short

    override fun decodeString(): String = value as String

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return (value as Enum<*>).ordinal
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return value as T
    }

    @ExperimentalSerializationApi
    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder = this

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean = value == null

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? = value as Nothing?

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        throw UnsupportedOperationException("Explicit decoding of elements of hardcoded values is not supported")
    }


}
