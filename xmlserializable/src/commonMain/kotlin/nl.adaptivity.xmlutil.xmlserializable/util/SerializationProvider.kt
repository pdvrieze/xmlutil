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
import kotlin.reflect.KClass
import nl.adaptivity.xmlutil.util.SerializationProvider as SerializationProviderCompat

public interface SerializationProvider : SerializationProviderCompat {
    public fun interface XmlSerializerFun<in T : Any> : SerializationProviderCompat.XmlSerializerFun<T> {
        public override operator fun invoke(output: XmlWriter, value: T)
    }

    public interface XmlDeserializerFun : SerializationProviderCompat.XmlDeserializerFun {
        public override operator fun <T : Any> invoke(input: XmlReader, type: KClass<T>): T
    }

    public override fun <T : Any> serializer(type: KClass<T>): XmlSerializerFun<T>?
    public override fun <T : Any> deSerializer(type: KClass<T>): XmlDeserializerFun?
}

internal class SerializationProviderWrapper(private val delegate: SerializationProviderCompat): SerializationProvider {
    override fun <T : Any> serializer(type: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        return when(val delegateSerializer = delegate.serializer(type)) {
            null -> null
            else -> SerializationProvider.XmlSerializerFun { out, value ->
                delegateSerializer.invoke(out, value)
            }
        }
    }

    override fun <T : Any> deSerializer(type: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        return when(val delegateDeserializer = delegate.deSerializer(type)) {
            null -> null
            else -> object: SerializationProvider.XmlDeserializerFun {
                override fun <T : Any> invoke(input: XmlReader, type: KClass<T>): T {
                    return delegateDeserializer(input, type)
                }
            }
        }
    }
}
