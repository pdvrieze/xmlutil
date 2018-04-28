/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */
@file:JvmMultifileClass
@file:JvmName("XmlReaderUtil")
package nl.adaptivity.xml

import java.io.CharArrayWriter

/**
 * Extension functions for XmlReader that only work on Java
 */


inline fun <reified T : Any> XmlReader.deSerialize(): T {
    return deSerialize(T::class.java)
}


fun <T> XmlReader.deSerialize(type: Class<T>): T {
    val deserializer = type.getAnnotation(XmlDeserializer::class.java) ?: throw IllegalArgumentException("Types must be annotated with " + XmlDeserializer::class.java.name + " to be deserialized automatically")

    return type.cast(deserializer.value.java.newInstance().deserialize(this))
}


@Throws(XmlException::class)
fun XmlReader.toCharArrayWriter(): CharArrayWriter
{
  return CharArrayWriter().apply {
    XmlStreaming.newWriter(this).use { out ->
      while (hasNext()) {
        writeCurrent(out)
      }
    }
  }
}


