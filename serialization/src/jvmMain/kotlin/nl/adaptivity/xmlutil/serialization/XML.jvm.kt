/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.*
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.xml.namespace.QName


private fun XML.getWriter(
    target: OutputStream,
    encoding: String
): XmlWriter {
    val c = config
    val writer = when {
        c.defaultToGenericParser -> xmlStreaming.newGenericWriter(
            OutputStreamWriter(target, encoding),
            c.repairNamespaces,
            c.xmlDeclMode,
            c.xmlVersion
        )

        else -> xmlStreaming.newWriter(
            OutputStreamWriter(target, encoding),
            c.repairNamespaces,
            c.xmlDeclMode,
            c.xmlVersion
        )
    }
    return writer
}

public inline fun <reified T> XML.XmlCompanion<*>.encodeToStream(
    target: OutputStream,
    value: T,
    prefix: String? = null
) {
    instance.encodeToStream(target, value, prefix)
}

public inline fun <reified T> XML.encodeToStream(target: OutputStream, value: T, prefix: String? = null) {
    encodeToStream(target, serializer<T>(), value, prefix)
}

public fun <T> XML.XmlCompanion<*>.encodeToStream(
    target: OutputStream,
    serializer: SerializationStrategy<T>,
    value: T,
    prefix: String? = null
) {
    instance.encodeToStream(target, serializer, value, prefix)
}

public fun <T> XML.encodeToStream(
    target: OutputStream,
    serializer: SerializationStrategy<T>,
    value: T,
    prefix: String? = null
) {
    encodeToStream(target, "UTF-8", serializer, value, prefix)
}

public fun <T> XML.XmlCompanion<*>.encodeToStream(
    target: OutputStream,
    encoding: String,
    serializer: SerializationStrategy<T>,
    value: T,
    prefix: String? = null
) {
    instance.encodeToStream(target, encoding, serializer, value, prefix)
}

public fun <T> XML.encodeToStream(
    target: OutputStream,
    encoding: String,
    serializer: SerializationStrategy<T>,
    value: T,
    prefix: String? = null
) {
    encodeToWriter(getWriter(target, encoding), serializer, value, prefix)
}

public inline fun <reified T> XML.XmlCompanion<*>.encodeToStream(target: OutputStream, value: T, rootName: QName) {
    instance.encodeToStream(target, value, rootName)
}

public inline fun <reified T> XML.encodeToStream(target: OutputStream, value: T, rootName: QName) {
    encodeToStream(target, serializer<T>(), value, rootName)
}

public fun <T> XML.XmlCompanion<*>.encodeToStream(
    target: OutputStream,
    serializer: SerializationStrategy<T>,
    value: T,
    rootName: QName
) {
    instance.encodeToStream(target, serializer, value, rootName)
}

public fun <T> XML.encodeToStream(
    target: OutputStream,
    serializer: SerializationStrategy<T>,
    value: T,
    rootName: QName
) {
    encodeToStream(target, "UTF-8", serializer, value, rootName)
}

public fun <T> XML.XmlCompanion<*>.encodeToStream(
    target: OutputStream,
    encoding: String,
    serializer: SerializationStrategy<T>,
    value: T,
    rootName: QName
) {
    instance.encodeToStream(target, encoding, serializer, value, rootName)
}

public fun <T> XML.encodeToStream(
    target: OutputStream,
    encoding: String,
    serializer: SerializationStrategy<T>,
    value: T,
    rootName: QName
) {
    val c = config
    val writer = when {
        c.defaultToGenericParser -> xmlStreaming.newGenericWriter(
            OutputStreamWriter(target, encoding),
            c.repairNamespaces,
            c.xmlDeclMode,
            c.xmlVersion
        )

        else -> xmlStreaming.newWriter(
            OutputStreamWriter(target, encoding),
            c.repairNamespaces,
            c.xmlDeclMode,
            c.xmlVersion
        )
    }
    encodeToWriter(writer, serializer, value, rootName)
}


public inline fun <reified T> XML.XmlCompanion<*>.decodeFromStream(source: InputStream, rootName: QName): T {
    return instance.decodeFromStream(source, rootName)
}

public inline fun <reified T> XML.decodeFromStream(source: InputStream, rootName: QName): T {
    return decodeFromStream(serializer<T>(), source, rootName)
}

public fun <T> XML.XmlCompanion<*>.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    source: InputStream,
    rootName: QName
): T {
    return instance.decodeFromStream(deserializer, source, rootName)
}

public fun <T> XML.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    source: InputStream,
    rootName: QName
): T {
    val reader = when (config.defaultToGenericParser) {
        true -> xmlStreaming.newGenericReader(source)

        else -> xmlStreaming.newReader(source)
    }
    return decodeFromReader(deserializer, reader, rootName)
}

public fun <T> XML.XmlCompanion<*>.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    source: InputStream,
    encoding: String,
    rootName: QName
): T {
    return instance.decodeFromStream(deserializer, source, encoding, rootName)
}

public fun <T> XML.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    source: InputStream,
    encoding: String,
    rootName: QName
): T {
    val reader = when (config.defaultToGenericParser) {
        true -> xmlStreaming.newGenericReader(source, encoding)

        else -> xmlStreaming.newReader(source, encoding)
    }
    return decodeFromReader(deserializer, reader, rootName)
}
