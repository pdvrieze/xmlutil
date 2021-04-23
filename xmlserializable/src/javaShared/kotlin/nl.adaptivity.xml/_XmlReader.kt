/*
 * Copyright (c) 2021.
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
@file:JvmMultifileClass
@file:JvmName("XmlReaderUtil")

package nl.adaptivity.xml

import nl.adaptivity.xmlutil.XmlReader
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import nl.adaptivity.xmlutil.xmlserializable.deSerialize as newDeSerialize


/**
 * Inline function to deserialize a type based upon `@[XmlDeserializer]` annotation.
 */
@Deprecated("Moved to nl.adaptivity.xmlutil", ReplaceWith("deserialize()", "nl.adaptivity.xmlutil.deSerialize"))
inline fun <reified T : Any> XmlReader.deSerialize(): T {
    return newDeSerialize()
}

/**
 * Deserialize a type with `@[XmlDeserializer]` annotation.
 */
@Deprecated("Moved to nl.adaptivity.xmlutil", ReplaceWith(
    "deSerialize(type)",
    "nl.adaptivity.xmlutil.deSerialize"
                                                     )
           )
fun <T> XmlReader.deSerialize(type: Class<T>): T {
    return newDeSerialize(type)
}



