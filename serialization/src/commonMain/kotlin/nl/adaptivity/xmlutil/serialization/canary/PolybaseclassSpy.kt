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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerialModuleCollector
import kotlin.reflect.KClass

private class PolybaseclassSpy: Encoder, Decoder, SerialModule {
    override val context: SerialModule get() = this
    var baseClass: KClass<*>? = null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPolymorphic(baseClass: KClass<T>, value: T): KSerializer<out T>? {
        this.baseClass = baseClass

        // This is nasty, but it works because it is merely a hack to retrieve the base class.
        return StringSerializer as KSerializer<out T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPolymorphic(baseClass: KClass<T>, serializedClassName: String): KSerializer<out T>? {
        this.baseClass = baseClass
        return StringSerializer as KSerializer<out T>
    }

    override fun <T : Any> getContextual(kclass: KClass<T>): KSerializer<T>?  = throw UnsupportedOperationException()

    override fun dumpTo(collector: SerialModuleCollector) = throw UnsupportedOperationException()

    override val updateMode: UpdateMode get() = UpdateMode.OVERWRITE

    override fun decodeBoolean(): Boolean = throw UnsupportedOperationException()

    override fun decodeByte(): Byte = throw UnsupportedOperationException()

    override fun decodeChar(): Char = throw UnsupportedOperationException()

    override fun decodeDouble(): Double = throw UnsupportedOperationException()

    override fun decodeEnum(enumDescription: EnumDescriptor): Int = throw UnsupportedOperationException()

    override fun decodeFloat(): Float = throw UnsupportedOperationException()

    override fun decodeInt(): Int = throw UnsupportedOperationException()

    override fun decodeLong(): Long = throw UnsupportedOperationException()

    override fun decodeNotNullMark(): Boolean = throw UnsupportedOperationException()

    override fun decodeNull(): Nothing? = throw UnsupportedOperationException()

    override fun decodeShort(): Short = throw UnsupportedOperationException()

    override fun decodeString(): String = throw UnsupportedOperationException()

    override fun decodeUnit() = throw UnsupportedOperationException()

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>) = throw UnsupportedOperationException()

    override fun encodeBoolean(value: Boolean) = throw UnsupportedOperationException()

    override fun encodeByte(value: Byte) = throw UnsupportedOperationException()

    override fun encodeChar(value: Char) = throw UnsupportedOperationException()

    override fun encodeDouble(value: Double) = throw UnsupportedOperationException()

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) = throw UnsupportedOperationException()

    override fun encodeFloat(value: Float) = throw UnsupportedOperationException()

    override fun encodeInt(value: Int) = throw UnsupportedOperationException()

    override fun encodeLong(value: Long) = throw UnsupportedOperationException()

    override fun encodeNotNullMark() = throw UnsupportedOperationException()

    override fun encodeNull() = throw UnsupportedOperationException()

    override fun encodeShort(value: Short) = throw UnsupportedOperationException()

    override fun encodeString(value: String) = throw UnsupportedOperationException()

    override fun encodeUnit() = throw UnsupportedOperationException()
}

fun PolymorphicSerializer<*>.getBaseClass(): KClass<*> {
    val spy = PolybaseclassSpy()
    this.findPolymorphicSerializer(spy, "ANY")
    return spy.baseClass ?: throw IllegalArgumentException("The serializer does not seem to have a base class")
}