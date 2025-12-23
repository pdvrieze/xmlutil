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

import nl.adaptivity.xmlutil.xmlStreaming
import nl.adaptivity.xmlutil.xmlserializable.internal.javaCompat
import java.io.StringReader
import kotlin.reflect.KClass
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader as MPReader

/**
 * Utility method to deserialize a list of xml containing strings
 * @receiver The strings to deserialize
 *
 * @param type The type that contains the factory to deserialize
 *
 * @return A list of deserialized objects.
 */
public fun <T> Iterable<String>.deSerialize(type: Class<T>): List<T> {
    val deserializer = type.getAnnotation(XmlDeserializer::class.javaCompat)
        ?: throw IllegalArgumentException("Types must be annotated with ${XmlDeserializer::class.javaCompat.name} to be deserialized automatically")

    @Suppress("DEPRECATION")
    val factory: XmlDeserializerFactory<*> = deserializer.value.javaCompat.getConstructor().newInstance() as XmlDeserializerFactory<*>

    return this.map { type.cast(factory.deserialize(xmlStreaming.newReader(StringReader(it) as MPReader))) }
}

/**
 * Utility method to deserialize a list of xml containing strings
 * @receiver The strings to deserialize
 *
 * @param type The type that contains the factory to deserialize
 *
 * @return A list of deserialized objects.
 */
public fun <T: Any> Iterable<String>.deSerialize(type: KClass<T>): List<T> {
    return deSerialize(type.javaCompat)
}
