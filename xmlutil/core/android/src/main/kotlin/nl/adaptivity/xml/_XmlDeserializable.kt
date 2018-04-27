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

package nl.adaptivity.xml

import java.io.StringReader
import kotlin.reflect.KClass

/**
 * Utility method to deserialize a list of xml containing strings
 * @param input The strings to deserialize
 *
 * @param type The type that contains the factory to deserialize
 *
 * @param  The type
 *
 * @return A list of deserialized objects.
 *
 * @throws XmlException If deserialization fails anywhere.
 */
fun <T> Iterable<String>.deSerialize(type: Class<T>): List<T> {
  val deserializer = type.getAnnotation(XmlDeserializer::class.java)
                     ?: throw IllegalArgumentException("Types must be annotated with ${XmlDeserializer::class.java.name} to be deserialized automatically")
  val factory: XmlDeserializerFactory<*> = deserializer.value.java.newInstance() as XmlDeserializerFactory<*>

  return this.map { type.cast(factory.deserialize(XmlStreaming.newReader(StringReader(it)))) }
}