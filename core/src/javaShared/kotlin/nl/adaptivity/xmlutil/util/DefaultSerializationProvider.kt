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

import kotlinx.serialization.ImplicitReflectionSerializer
import nl.adaptivity.xmlutil.XmlDeserializer
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlWriter
import kotlin.reflect.KClass

@UseExperimental(ImplicitReflectionSerializer::class)
class DefaultSerializationProvider: SerializationProvider {
    override fun <T : Any> serializer(type: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        @Suppress("UNCHECKED_CAST") // the system isn't smart enough that this means T is a subtype
        if (type is XmlSerializable) return (SerializableSerializer as SerializationProvider.XmlSerializerFun<T>)
        return null
    }

    override fun <T : Any> deSerializer(type: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        val a: XmlDeserializer? = type.java.getAnnotation(XmlDeserializer::class.java)
        return a?.let{ DeserializerFun }
    }

    private object DeserializerFun: SerializationProvider.XmlDeserializerFun {
        override fun <T : Any> invoke(input: XmlReader, type: KClass<T>): T {
            val factory = type.java.getAnnotation(
                XmlDeserializer::class.java).value.java.newInstance()!!

            return factory.deserialize(input)
        }
    }


    private object SerializableSerializer: SerializationProvider.XmlSerializerFun<XmlSerializable> {
        override fun invoke(output: XmlWriter, value: XmlSerializable) {
            value.serialize(output)
        }
    }

}