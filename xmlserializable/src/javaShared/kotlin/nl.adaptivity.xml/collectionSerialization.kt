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

package nl.adaptivity.xml

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.xmlserializable.asSerializable as newAsSerializable

/**
 * Created by pdvrieze on 27/04/16.
 */
@Deprecated("Moved type to xmlutil package")
typealias BaseSerializableContainer<T> = nl.adaptivity.xmlutil.xmlserializable.BaseSerializableContainer<T>

@Deprecated("Moved type to xmlutil package")
typealias SerializableContainer<T> = nl.adaptivity.xmlutil.xmlserializable.SerializableContainer<T>

@Deprecated("Moved type to xmlutil package", ReplaceWith(
    "asSerializable(name)",
    "nl.adaptivity.xmlutil.xmlserializable.asSerializable"
                                                    )
           )
@Suppress("unused")
fun <T : XmlSerializable> Iterable<T>.asSerializable(name: QName) = newAsSerializable(name)


@Deprecated("Moved type to xmlutil package")
typealias SerializableCollection<T> = nl.adaptivity.xmlutil.xmlserializable.SerializableCollection<T>

@Deprecated("Moved type to xmlutil package", ReplaceWith(
    "asSerializable(name)",
    "nl.adaptivity.xmlutil.xmlserializable.asSerializable"
                                                        )
           )
@Suppress("unused")
fun <T : XmlSerializable> Collection<T>.asSerializable(name: QName) = newAsSerializable(name)

@Deprecated("Moved type to xmlutil package")
typealias SerializableList<T> = nl.adaptivity.xmlutil.xmlserializable.SerializableList<T>

@Deprecated("Moved type to xmlutil package", ReplaceWith(
    "asSerializable(name)",
    "nl.adaptivity.xmlutil.xmlserializable.asSerializable"
                                                        )
           )
@Suppress("unused")
fun <T : XmlSerializable> List<T>.asSerializable(name: QName) = newAsSerializable(name)
