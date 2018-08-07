/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */
@file:JvmName("XmlSerializableExt")

package nl.adaptivity.xmlutil

import java.io.*

/**
 * Create a character array containing a serialized version of the element.
 */
fun XmlSerializable.toCharArray(): CharArray {
    val caw = CharArrayWriter()
    XmlStreaming.newWriter(caw).use { writer ->
        serialize(writer)
    }
    return caw.toCharArray()
}


/**
 * Create a reader that can be used to read the xml serialization of the element.
 */
@Throws(XmlException::class)
fun XmlSerializable.toReader(): Reader {
    val buffer = CharArrayWriter()
    XmlStreaming.newWriter(buffer).use {
        serialize(it)

    }
    return CharArrayReader(buffer.toCharArray())
}

/**
 * Serialize the object to XML
 */
@Throws(XmlException::class)
fun XmlSerializable.serialize(writer: Writer) {
    XmlStreaming.newWriter(writer, true).use { serialize(it) }
}

fun XmlSerializable.toString(flags: Int): String {
    return StringWriter().apply {
        XmlStreaming.newWriter(this, flags.and(
                FLAG_REPAIR_NS) == FLAG_REPAIR_NS).use { writer ->
            serialize(writer)
        }
    }.toString()
}

fun toString(serializable: XmlSerializable) = serializable.toString(
        DEFAULT_FLAGS)

/**
 * Do bulk toString conversion of a list. Note that this is serialization, not dropping tags.
 * @receiver The source list.
 *
 * @return A result list
 */
@JvmName("toString")
fun Iterable<XmlSerializable>.toSerializedStrings(): List<String> {
    return this.map { it.toString(DEFAULT_FLAGS) }
}

