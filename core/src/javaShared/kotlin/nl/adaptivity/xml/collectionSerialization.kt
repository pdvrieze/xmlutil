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

import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.writeChildren
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 27/04/16.
 */

abstract class BaseSerializableContainer<T : XmlSerializable>(val name: QName) : XmlSerializable {
    abstract val children: Iterable<T>

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(name) {
            writeChildren(children)
        }
    }
}

class SerializableContainer<T : XmlSerializable>(name: QName, delegate: Iterable<T>) :
    BaseSerializableContainer<T>(name), Iterable<T> by delegate {
    override val children: Iterable<T> get() = this
}

@Suppress("unused")
fun <T : XmlSerializable> Iterable<T>.asSerializable(name: QName) = SerializableContainer(name, this)

class SerializableCollection<T : XmlSerializable>(name: QName, delegate: Collection<T>) :
    BaseSerializableContainer<T>(name), Collection<T> by delegate {
    override val children: Iterable<T> get() = this
}

@Suppress("unused")
fun <T : XmlSerializable> Collection<T>.asSerializable(name: QName) = SerializableCollection(name, this)

class SerializableList<T : XmlSerializable>(name: QName, delegate: List<T>) : BaseSerializableContainer<T>(name),
                                                                              List<T> by delegate {
    override val children: Iterable<T> get() = this
}

@Suppress("unused")
fun <T : XmlSerializable> List<T>.asSerializable(name: QName) = SerializableList(name, this)
