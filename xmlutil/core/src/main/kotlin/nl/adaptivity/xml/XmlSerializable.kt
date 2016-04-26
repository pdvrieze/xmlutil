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

@file:JvmName("XmlUtil")
@file:JvmMultifileClass

package nl.adaptivity.xml

import java.io.*
import java.util.*


interface XmlSerializable {

  /**
   * Write the object to an xml stream. The object is expected to write itself and its children.
   * @param out The stream to write to.
   * *
   * @throws XmlException When something breaks.
   */
  @Throws(XmlException::class)
  fun serialize(out: XmlWriter)

}


@Throws(XmlException::class)
fun XmlSerializable.toReader(): Reader {
  val buffer = CharArrayWriter()
  XmlStreaming.newWriter(buffer).use {
    serialize(it)

  }
  return CharArrayReader(buffer.toCharArray())
}

@Throws(XmlException::class)
fun XmlSerializable.serialize(writer: Writer) {
  XmlStreaming.newWriter(writer, true).use { serialize(it) }
}

private fun XmlSerializable.toString(flags: Int): String {
  return StringWriter().apply {
    XmlStreaming.newWriter(this, flags.and(FLAG_REPAIR_NS)==FLAG_REPAIR_NS).use { writer ->
      serialize(writer)
    }
  }.toString()
}

fun XmlSerializable.toString() = toString(DEFAULT_FLAGS)

/**
 * Do bulk toString conversion of a list. Note that this is serialization, not dropping tags.
 * @param serializables The source list.
 * *
 * @return A result list
 */
fun Iterable<XmlSerializable>.toString(): List<String> {
  return this.map { it.toString(DEFAULT_FLAGS) }
}

