/*
 * Copyright (c) 2019.
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

package nl.adaptivity.serialutil.encoders

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

private class HashEncoder(override val serializersModule: SerializersModule) : Encoder {
    var hash: Int = 1

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return CompositeHashEncoder(this)
    }

    override fun encodeBoolean(value: Boolean) {
        hash = value.hashCode()
    }

    override fun encodeByte(value: Byte) {
        hash = value.hashCode()
    }

    override fun encodeChar(value: Char) {
        hash = value.hashCode()
    }

    override fun encodeDouble(value: Double) {
        hash = value.hashCode()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        hash = enumDescriptor.getElementName(index).hashCode()
    }

    override fun encodeFloat(value: Float) {
        hash = value.hashCode()
    }

    override fun encodeInt(value: Int) {
        hash = value.hashCode()
    }

    override fun encodeLong(value: Long) {
        hash = value.hashCode()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun encodeNull() {
        hash = 0
    }

    override fun encodeShort(value: Short) {
        hash = value.hashCode()
    }

    override fun encodeString(value: String) {
        hash = value.hashCode()
    }

}


private class CompositeHashEncoder(val elementEncoder: HashEncoder) : CompositeEncoder {
    override val serializersModule: SerializersModule get() = elementEncoder.serializersModule

    var hash = 1
        private set

    private inline fun addHash(elementCode: () -> Int) {
        hash = hash * 31 + elementCode()
    }


    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        addHash { value.hashCode() }
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        addHash { value.hashCode() }
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        addHash { value.hashCode() }
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        addHash { value.hashCode() }
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        addHash { value.hashCode() }
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        addHash { value.hashCode() }
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        addHash { value.hashCode() }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun encodeNonSerializableElement(descriptor: SerialDescriptor, index: Int, value: Any) {
        addHash { value.hashCode() }
    }

    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
                                                            ) {
        if (value == null) {
            addHash { 0 }
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
        val subEnc = HashEncoder(serializersModule)
        serializer.serialize(subEnc, value)
        addHash { subEnc.hash }
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        addHash { value.hashCode() }
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        addHash { value.hashCode() }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        elementEncoder.hash = hash
    }
}

/**
 * A format that allows for using serialization to create a hash code of a data structure.
 */
@OptIn(ExperimentalSerializationApi::class)
class HashFormat constructor(override val serializersModule: SerializersModule) : SerialFormat {

    fun <T> hashCode(serializer: KSerializer<T>, obj: T): Int {
        return HashEncoder(serializersModule).also { enc -> serializer.serialize(enc, obj) }.hash
    }

    companion object : SerialFormat {
        private val defaultFormat = HashFormat(EmptySerializersModule)
        override val serializersModule: SerializersModule get() = defaultFormat.serializersModule

        /**
         * This function uses the default (empty) context to create a hashcode for the parameter.
         */
        fun <T> hashCode(serializer: KSerializer<T>, obj: T): Int {
            return defaultFormat.hashCode(serializer, obj)
        }
    }

}