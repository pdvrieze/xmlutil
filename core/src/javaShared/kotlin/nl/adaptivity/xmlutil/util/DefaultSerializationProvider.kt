/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.util

import kotlinx.serialization.ImplicitReflectionSerializer
import nl.adaptivity.xmlutil.*
import kotlin.reflect.KClass

@UseExperimental(ImplicitReflectionSerializer::class)
class DefaultSerializationProvider : SerializationProvider {
    override fun <T : Any> serializer(type: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        @Suppress("UNCHECKED_CAST") // the system isn't smart enough that this means T is a subtype
        if (type is XmlSerializable) return (SerializableSerializer as SerializationProvider.XmlSerializerFun<T>)
        return null
    }

    override fun <T : Any> deSerializer(type: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        val a: XmlDeserializer? = type.java.getAnnotation(XmlDeserializer::class.java)
        return a?.let { DeserializerFun }
    }

    private object DeserializerFun : SerializationProvider.XmlDeserializerFun {
        override fun <T : Any> invoke(input: XmlReader, type: KClass<T>): T {
            @Suppress("UNCHECKED_CAST")
            val factory = type.java.getAnnotation(XmlDeserializer::class.java)
                .value.java.getConstructor().newInstance() as XmlDeserializerFactory<T>

            return factory.deserialize(input)
        }
    }


    private object SerializableSerializer : SerializationProvider.XmlSerializerFun<XmlSerializable> {
        override fun invoke(output: XmlWriter, value: XmlSerializable) {
            value.serialize(output)
        }
    }

}