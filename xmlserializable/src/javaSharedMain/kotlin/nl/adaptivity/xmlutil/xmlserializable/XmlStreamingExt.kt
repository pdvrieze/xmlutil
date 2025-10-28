/*
 * Copyright (c) 2023-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.IXmlStreaming
import nl.adaptivity.xmlutil.xmlserializable.internal.javaCompat
import nl.adaptivity.xmlutil.xmlserializable.internal.kotlinCompat
import nl.adaptivity.xmlutil.xmlserializable.util.SerializationProvider
import java.util.*
import kotlin.reflect.KClass

public fun <T : Any> IXmlStreaming.deserializerFor(type: Class<T>): SerializationProvider.XmlDeserializerFun? {
    return deserializerFor(type.kotlinCompat)
}

@Suppress("UnusedReceiverParameter")
public fun <T : Any> IXmlStreaming.deserializerFor(klass: KClass<T>): SerializationProvider.XmlDeserializerFun? {
    return XmlStreamingExt.serializationProviders.mapNotNull { it.deSerializer(klass) }.firstOrNull()
}

internal object XmlStreamingExt {
    private val serializationLoader: ServiceLoader<SerializationProvider> by lazy {
        val service = SerializationProvider::class.javaCompat
        ServiceLoader.load(service, service.classLoader)
    }

    internal val serializationProviders: Sequence<SerializationProvider>
        get() = serializationLoader.asSequence()
}
