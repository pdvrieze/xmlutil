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

import kotlinx.serialization.KSerialLoader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializerByClass
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.serialization.XML
import kotlin.reflect.KClass

class KotlinxSerializationProvider : SerializationProvider {
    override fun <T : Any> serializer(type: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        return SerializerFun(type, serializerByClass(type))
    }

    override fun <T : Any> deSerializer(type: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        return DeserializerFun(type, serializerByClass(type))
    }

    private class SerializerFun<T : Any>(val kClass: KClass<T>,
                                         val serializer: KSerializer<T>) : SerializationProvider.XmlSerializerFun<T> {

        override fun invoke(output: XmlWriter, value: T) {
            XML().toXml(kClass, value, output, serializer = serializer)
        }
    }

    private class DeserializerFun<T: Any>(val kClass: KClass<T>, val serializer: KSerializer<T>): SerializationProvider.XmlDeserializerFun {
        override fun <U : Any> invoke(input: XmlReader, type: KClass<U>): U {
            @Suppress("UNCHECKED_CAST")
            val loader: KSerialLoader<U> = serializer as KSerializer<U>
            return XML.parse<U>(type, input, loader)
        }
    }

}