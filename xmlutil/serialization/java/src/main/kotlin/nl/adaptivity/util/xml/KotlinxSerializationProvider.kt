/*
 * Copyright (c) 2018.
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

package nl.adaptivity.util.xml

import kotlinx.serialization.*
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.serialization.XML
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

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
            XML().toXml(kClass, value, output, serializer = serializer)
        }
    }

    private class DeserializerFun<T : Any>(val kClass: KClass<T>,
                                           val serializer: KSerializer<T>) : SerializationProvider.XmlDeserializerFun {
        override fun <U : Any> invoke(input: XmlReader, type: KClass<U>): U {
            @Suppress("UNCHECKED_CAST")
            val loader: KSerialLoader<U> = serializer as KSerializer<U>
            return XML.parse<U>(type, input, loader)
        }
    }

    private companion object {
        private val NULL_SER = object : KSerializer<Any> {
            override val serialClassDesc: Nothing get() = throw UnsupportedOperationException("Leaking NULL serializer")

            override fun load(input: KInput): Nothing = throw UnsupportedOperationException("Leaking NULL serializer")

            override fun save(output: KOutput, obj: Any) =
                throw UnsupportedOperationException("Leaking NULL serializer")
        }

        val serializers = ConcurrentHashMap<KClass<*>, KSerializer<*>?>()

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getSerializer(type: KClass<T>): KSerializer<T>? {
            val result =  serializers.getOrPut(type) {
                try {
                    serializerByClass<T>(type)
                } catch (e: SerializationException) {
                    NULL_SER
                }
            } as KSerializer<T>?

            return if (result === NULL_SER) null else result
        }
    }

}
