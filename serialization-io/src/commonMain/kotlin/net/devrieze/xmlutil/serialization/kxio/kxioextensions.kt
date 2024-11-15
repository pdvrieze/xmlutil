/*
 * Copyright (c) 2024.
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

package net.devrieze.xmlutil.serialization.kxio

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.core.kxio.newGenericWriter
import nl.adaptivity.xmlutil.core.kxio.newReader
import nl.adaptivity.xmlutil.core.kxio.newWriter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlStreaming

public inline fun <reified T> XML.encodeToSink(sink: Sink, value: T, prefix: String? = null) {
    return encodeToSink(sink, serializer<T>(), value, prefix)
}

public fun <T> XML.encodeToSink(sink: Sink, serializer: SerializationStrategy<T>, value: T, prefix: String? = null) {
    when {
        config.defaultToGenericParser -> xmlStreaming.newGenericWriter(sink)
        else -> xmlStreaming.newWriter(sink)
    }.use { target ->
        encodeToWriter(target, serializer, value, prefix)
    }
}

public inline fun <reified T> XML.encodeToSink(sink: Sink, value: T, rootName: QName?) {
    return encodeToSink(sink, serializer<T>(), value, rootName)
}

public fun <T> XML.encodeToSink(sink: Sink, serializer: SerializationStrategy<T>, value: T, rootName: QName?) {
    when {
        config.defaultToGenericParser -> xmlStreaming.newGenericWriter(sink)
        else -> xmlStreaming.newWriter(sink)
    }.use { target ->
        encodeToWriter(target, serializer, value, rootName)
    }
}

public inline fun <reified T> XML.decodeFromSource(source: Source, rootName: QName? = null): T {
    return decodeFromSource(serializer<T>(), source, rootName)
}

public fun <T> XML.decodeFromSource(serializer: DeserializationStrategy<T>, source: Source, rootName: QName? = null): T {
    return decodeFromReader(serializer, xmlStreaming.newReader(source), rootName)
}
