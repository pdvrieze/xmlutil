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

package nl.adaptivity.xmlutil.util

import kotlinx.serialization.*
import kotlinx.serialization.context.MutableSerialContextImpl
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.reflect.KClass

@ImplicitReflectionSerializer
class KotlinxSerializationProvider : SerializationProvider {
    override fun <T : Any> serializer(type: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        return getSerializer(type)?.let { SerializerFun(type, it) }
    }

    override fun <T : Any> deSerializer(type: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        return getSerializer(type)?.let { DeserializerFun(type, it) }
    }

    private class SerializerFun<T : Any>(val kClass: KClass<T>,
                                         val serializer: KSerializer<T>) : SerializationProvider.XmlSerializerFun<T> {

        override fun invoke(output: XmlWriter, value: T) {
            XML().toXml(target = output, serializer = serializer, obj = value)
        }
    }

    private class DeserializerFun<T : Any>(val kClass: KClass<T>,
                                           val serializer: KSerializer<T>) : SerializationProvider.XmlDeserializerFun {
        override fun <U : Any> invoke(input: XmlReader, type: KClass<U>): U {
            @Suppress("UNCHECKED_CAST")
            val loader: DeserializationStrategy<U> = serializer as KSerializer<U>
            return XML.parse<U>(input, type, loader)
        }
    }

    private companion object {

        private val serializers = MutableSerialContextImpl()

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getSerializer(type: KClass<T>): KSerializer<T>? {
            val result =  serializers.get(type) ?: run {
                try {
                    type.serializer().also { serializers.registerSerializer(type, it) }
                } catch (e: SerializationException) {
                    null
                }
            }

            return result
        }
    }

}
