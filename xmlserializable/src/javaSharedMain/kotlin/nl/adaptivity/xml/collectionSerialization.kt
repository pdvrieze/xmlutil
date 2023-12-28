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

package nl.adaptivity.xml

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.xmlserializable.SerializableContainer
import nl.adaptivity.xmlutil.xmlserializable.XmlSerializable
import nl.adaptivity.xmlutil.xmlserializable.SerializableCollection as NewSerializableCollection
import nl.adaptivity.xmlutil.xmlserializable.SerializableList as NewSerializableList
import nl.adaptivity.xmlutil.xmlserializable.asSerializable as newAsSerializable

/**
 * Created by pdvrieze on 27/04/16.
 */
@Deprecated("Moved type to xmlutil package")
public typealias BaseSerializableContainer<T> = nl.adaptivity.xmlutil.xmlserializable.BaseSerializableContainer<T>

@Deprecated("Moved type to xmlutil package")
public typealias SerializableContainer<T> = nl.adaptivity.xmlutil.xmlserializable.SerializableContainer<T>

@Deprecated(
    "Moved type to xmlutil package", ReplaceWith(
        "asSerializable(name)",
        "nl.adaptivity.xmlutil.xmlserializable.asSerializable"
    )
)
@Suppress("unused")
public fun <T : XmlSerializable> Iterable<T>.asSerializable(name: QName): SerializableContainer<T> =
    newAsSerializable(name)


@Deprecated("Moved type to xmlutil package")
public typealias SerializableCollection<T> = NewSerializableCollection<T>

@Deprecated(
    "Moved type to xmlutil package", ReplaceWith(
        "asSerializable(name)",
        "nl.adaptivity.xmlutil.xmlserializable.asSerializable"
    )
)
@Suppress("unused")
public fun <T : XmlSerializable> Collection<T>.asSerializable(name: QName): NewSerializableCollection<T> = newAsSerializable(name)

@Deprecated("Moved type to xmlutil package")
public typealias SerializableList<T> = NewSerializableList<T>

@Deprecated(
    "Moved type to xmlutil package", ReplaceWith(
        "asSerializable(name)",
        "nl.adaptivity.xmlutil.xmlserializable.asSerializable"
    )
)
@Suppress("unused")
public fun <T : XmlSerializable> List<T>.asSerializable(name: QName): NewSerializableList<T> = newAsSerializable(name)
