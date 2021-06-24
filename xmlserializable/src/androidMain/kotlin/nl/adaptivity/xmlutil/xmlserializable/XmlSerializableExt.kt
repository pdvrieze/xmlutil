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
@file:JvmName("XmlSerializableExt")

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.*
import java.io.*


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
    XmlStreaming.newWriter(writer, repairNamespaces = true, xmlDeclMode = XmlDeclMode.None).use { serialize(it) }
}

fun XmlSerializable.toString(flags: Int): String {
    return StringWriter().apply {
        XmlStreaming.newWriter(
            this,
            flags.and(FLAG_REPAIR_NS) == FLAG_REPAIR_NS,
            XmlDeclMode.from(flags.and(FLAG_OMIT_XMLDECL) == FLAG_OMIT_XMLDECL)
                              ).use { writer ->
            serialize(writer)
        }
    }.toString()
}

fun toString(serializable: XmlSerializable) = serializable.toString(
    DEFAULT_FLAGS
                                                                   )

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

