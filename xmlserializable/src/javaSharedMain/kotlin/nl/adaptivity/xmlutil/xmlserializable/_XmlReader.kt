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

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlserializable.internal.javaCompat
import kotlin.reflect.KClass

/**
 * Extension functions for XmlReader that only work on Java
 */
public inline fun <reified T : Any> XmlReader.deSerialize(): T {
    return deSerialize(T::class)
}

public fun <T: Any> XmlReader.deSerialize(type: KClass<T>): T {
    val jType = type.javaCompat
    val deserializer = jType.getAnnotation(XmlDeserializer::class.javaCompat)
        ?: throw IllegalArgumentException("Types must be annotated with ${XmlDeserializer::class.javaCompat.name} to be deserialized automatically")

    return jType.cast(deserializer.value.javaCompat.getConstructor().newInstance().deserialize(this))
}

public fun <T> XmlReader.deSerialize(type: Class<T>): T {
    val deserializer = type.getAnnotation(XmlDeserializer::class.javaCompat)
        ?: throw IllegalArgumentException("Types must be annotated with ${XmlDeserializer::class.javaCompat.name} to be deserialized automatically")

    return type.cast(deserializer.value.javaCompat.getConstructor().newInstance().deserialize(this))
}



