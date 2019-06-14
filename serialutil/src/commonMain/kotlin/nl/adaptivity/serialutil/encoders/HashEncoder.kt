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

import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule

private class HashEncoder(override val context: SerialModule) : Encoder {
    internal var hash: Int = 1

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
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

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
        hash = enumDescription.getElementName(ordinal).hashCode()
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

    override fun encodeNotNullMark() {}

    override fun encodeNull() {
        hash = 0
    }

    override fun encodeShort(value: Short) {
        hash = value.hashCode()
    }

    override fun encodeString(value: String) {
        hash = value.hashCode()
    }

    override fun encodeUnit() {
        hash = 1 // to distinguish it from a null value
    }


}


private class CompositeHashEncoder(val elementEncoder: HashEncoder) : CompositeEncoder {
    override val context: SerialModule get() = elementEncoder.context

    var hash = 1
        private set

    private inline fun addHash(elementCode: () -> Int) {
        hash = hash * 31 + elementCode()
    }


    override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) {
        addHash { value.hashCode() }
    }

    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) {
        addHash { value.hashCode() }
    }

    override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) {
        addHash { value.hashCode() }
    }

    override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) {
        addHash { value.hashCode() }
    }

    override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) {
        addHash { value.hashCode() }
    }

    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) {
        addHash { value.hashCode() }
    }

    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) {
        addHash { value.hashCode() }
    }

    override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) {
        addHash { value.hashCode() }
    }

    override fun <T : Any> encodeNullableSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
                                                            ) {
        if (value == null) {
            addHash { 0 }
        } else {
            encodeSerializableElement(desc, index, serializer, value)
        }
    }

    override fun <T> encodeSerializableElement(
        desc: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
                                              ) {
        val subEnc = HashEncoder(context)
        serializer.serialize(subEnc, value)
        addHash { subEnc.hash }
    }

    override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) {
        addHash { value.hashCode() }
    }

    override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) {
        addHash { value.hashCode() }
    }

    override fun encodeUnitElement(desc: SerialDescriptor, index: Int) {}

    override fun endStructure(desc: SerialDescriptor) {
        elementEncoder.hash = hash
    }
}

/**
 * A format that allows for using serialization to create a hash code of a data structure.
 */
class HashFormat(override val context: SerialModule) : SerialFormat {

    fun <T> hashCode(serializer: KSerializer<T>, obj: T):Int {
        return HashEncoder(context).also { enc -> serializer.serialize(enc, obj) }.hash
    }

    companion object : SerialFormat {
        private val defaultFormat = HashFormat(EmptyModule)
        override val context: SerialModule get() = defaultFormat.context

        /**
         * This function uses the default (empty) context to create a hashcode for the parameter.
         */
        fun <T> hashCode(serializer: KSerializer<T>, obj: T):Int {
            return defaultFormat.hashCode(serializer, obj)
        }
    }

}