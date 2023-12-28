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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.xmlserializable.util

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.xmlserializable.XmlDeserializer
import nl.adaptivity.xmlutil.xmlserializable.XmlSerializable
import nl.adaptivity.xmlutil.xmlserializable.internal.javaCompat
import kotlin.reflect.KClass
import nl.adaptivity.xmlutil.XmlSerializable as XmlSerializableCompat

public class DefaultSerializationProvider : SerializationProvider {
    override fun <T : Any> serializer(type: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        @Suppress("UNCHECKED_CAST") // the system isn't smart enough that this means T is a subtype
        return when {
            XmlSerializable::class.javaCompat.isAssignableFrom(type.javaCompat) ->
                (SerializableSerializer as SerializationProvider.XmlSerializerFun<T>)

            XmlSerializableCompat::class.javaCompat.isAssignableFrom(type.javaCompat) ->
                (CompatSerializableSerializer as SerializationProvider.XmlSerializerFun<T>)

            else -> return null
        }
    }

    override fun <T : Any> deSerializer(type: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        val a = type.javaCompat.getAnnotation(XmlDeserializer::class.javaCompat)
        return a?.let { DeserializerFun }
    }

    private object DeserializerFun : SerializationProvider.XmlDeserializerFun {
        override fun <T : Any> invoke(input: XmlReader, type: KClass<T>): T {
            val a = type.javaCompat.getAnnotation(XmlDeserializer::class.javaCompat)
            val factoryClass = a.value.javaCompat
            val factory = factoryClass.getConstructor().newInstance()

            @Suppress("UNCHECKED_CAST")
            return factory.deserialize(input) as T
        }
    }


    private object SerializableSerializer : SerializationProvider.XmlSerializerFun<XmlSerializable> {
        override fun invoke(output: XmlWriter, value: XmlSerializable) {
            value.serialize(output)
        }
    }

    private object CompatSerializableSerializer : SerializationProvider.XmlSerializerFun<XmlSerializableCompat> {
        override fun invoke(output: XmlWriter, value: XmlSerializableCompat) {
            value.serialize(output)
        }
    }
}
