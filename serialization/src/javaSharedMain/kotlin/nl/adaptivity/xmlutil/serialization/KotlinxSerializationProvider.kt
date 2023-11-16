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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.XML.Companion.decodeFromReader
import nl.adaptivity.xmlutil.util.SerializationProvider
import kotlin.reflect.KClass

@InternalSerializationApi
public class KotlinxSerializationProvider : SerializationProvider {
    override fun <T : Any> serializer(type: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        return getSerializer(type)?.let { SerializerFun(it) }
    }

    override fun <T : Any> deSerializer(type: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        return getSerializer(type)?.let { DeserializerFun(it) }
    }

    private class SerializerFun<T : Any>(val serializer: KSerializer<T>) : SerializationProvider.XmlSerializerFun<T> {

        override fun invoke(output: XmlWriter, value: T) {
            XML().encodeToWriter(target = output, serializer = serializer, value = value)
        }
    }

    private class DeserializerFun<T : Any>(val serializer: KSerializer<T>) : SerializationProvider.XmlDeserializerFun {
        override fun <U : Any> invoke(input: XmlReader, type: KClass<U>): U {
            @Suppress("UNCHECKED_CAST")
            val loader: DeserializationStrategy<U> = serializer as KSerializer<U>
            return decodeFromReader(loader, input)
        }
    }

    private companion object {

        private val serializers = mutableMapOf<KClass<*>, KSerializer<*>>()

        @InternalSerializationApi
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getSerializer(type: KClass<T>): KSerializer<T>? {

            return serializers[type] as? KSerializer<T> ?: try {
                type.serializer().also { serializers[type] = it }
            } catch (e: SerializationException) {
                null
            }
        }
    }

}
