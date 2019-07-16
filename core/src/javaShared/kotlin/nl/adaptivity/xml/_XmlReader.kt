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
@file:JvmMultifileClass
@file:JvmName("XmlReaderUtil")

package nl.adaptivity.xml

import nl.adaptivity.xmlutil.XmlDeserializer
import nl.adaptivity.xmlutil.XmlReader

/**
 * Extension functions for XmlReader that only work on Java
 */


inline fun <reified T : Any> XmlReader.deSerialize(): T {
    return deSerialize(T::class.java)
}


fun <T> XmlReader.deSerialize(type: Class<T>): T {
    val deserializer = type.getAnnotation(XmlDeserializer::class.java)
        ?: throw IllegalArgumentException("Types must be annotated with ${XmlDeserializer::class.java.name} to be deserialized automatically")

    return type.cast(deserializer.value.java.getConstructor().newInstance().deserialize(this))
}



