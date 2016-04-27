/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 27/04/16.
 */

abstract class BaseSerializableContainer<T:XmlSerializable>(val name:QName): XmlSerializable {
  abstract val children: Iterable<T>

  override fun serialize(out: XmlWriter) {
    out.smartStartTag(name)
    out.writeChildren(children)
    out.endTag(name)
  }
}

class SerializableContainer<T:XmlSerializable>(name:QName, delegate: Iterable<T>):BaseSerializableContainer<T>(name), Iterable<T> by delegate {
  override val children:Iterable<T> get() = this
}

fun <T:XmlSerializable> Iterable<T>.asSerializable(name:QName) = SerializableContainer(name, this)

class SerializableCollection<T:XmlSerializable>(name:QName, delegate: Collection<T>):BaseSerializableContainer<T>(name), Collection<T> by delegate {
  override val children:Iterable<T> get() = this
}

fun <T:XmlSerializable> Collection<T>.asSerializable(name:QName) = SerializableCollection(name, this)

class SerializableList<T:XmlSerializable>(name:QName, delegate: List<T>):BaseSerializableContainer<T>(name), List<T> by delegate {
  override val children:Iterable<T> get() = this
}

fun <T:XmlSerializable> List<T>.asSerializable(name:QName) = SerializableList(name, this)
