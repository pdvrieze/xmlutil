/*
 * Copyright (c) 2024-2026.
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

@file:MustUseReturnValues

package net.devrieze.xmlutil.serialization.kxio

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.core.kxio.newGenericWriter
import nl.adaptivity.xmlutil.core.kxio.newReader
import nl.adaptivity.xmlutil.core.kxio.newWriter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.xmlStreaming

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param value The value to encode to XML
 * @param prefix The prefix to use for the namespace
 */
public inline fun <reified T> XML.XmlCompanion<*>.encodeToSink(sink: Sink, value: T, prefix: String? = null) {
    return encodeToSink(sink, serializer<T>(), value, prefix)
}

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param value The value to encode to XML
 * @param rootName The name of the root tag to use
 */
public inline fun <reified T> XML.XmlCompanion<*>.encodeToSink(sink: Sink, value: T, rootName: QName) {
    return encodeToSink(sink, serializer<T>(), value, rootName)
}

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param serializer The serializer to be used
 * @param value The value to encode to XML
 * @param prefix The prefix to use for the namespace
 */
public fun <T> XML.XmlCompanion<*>.encodeToSink(sink: Sink, serializer: SerializationStrategy<T>, value: T, prefix: String? = null) {
    return instance.encodeToSink(sink, serializer, value, prefix)
}

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param serializer The serializer to be used
 * @param value The value to encode to XML
 * @param rootName The name of the root tag to use
 */
public fun <T> XML.XmlCompanion<*>.encodeToSink(sink: Sink, serializer: SerializationStrategy<T>, value: T, rootName: QName) {
    return instance.encodeToSink(sink, serializer, value, rootName)
}

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param value The value to encode to XML
 * @param prefix The prefix to use for the namespace
 */
public inline fun <reified T> XML.encodeToSink(sink: Sink, value: T, prefix: String? = null) {
    return encodeToSink(sink, serializer<T>(), value, prefix)
}

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param value The value to encode to XML
 * @param rootName The name of the root tag to use
 */
public inline fun <reified T> XML.encodeToSink(sink: Sink, value: T, rootName: QName) {
    return encodeToSink(sink, serializer<T>(), value, rootName)
}

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param serializer The serializer to be used
 * @param value The value to encode to XML
 * @param prefix The prefix to use for the namespace
 */
public fun <T> XML.encodeToSink(sink: Sink, serializer: SerializationStrategy<T>, value: T, prefix: String? = null) {
    when {
        config.defaultToGenericParser -> xmlStreaming.newGenericWriter(sink)
        else -> xmlStreaming.newWriter(sink)
    }.use { target ->
        encodeToWriter(target, serializer, value, prefix)
    }
}

/**
 * Encode the value to the given sink.
 *
 * @param sink The receiver of the XML document
 * @param serializer The serializer to be used
 * @param value The value to encode to XML
 * @param rootName The name of the root tag to use
 */
public fun <T> XML.encodeToSink(sink: Sink, serializer: SerializationStrategy<T>, value: T, rootName: QName) {
    when {
        config.defaultToGenericParser -> xmlStreaming.newGenericWriter(sink)
        else -> xmlStreaming.newWriter(sink)
    }.use { target ->
        encodeToWriter(target, serializer, value, rootName)
    }
}

/**
 * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
 * though the reified function.
 *
 * @param source An [XmlReader] that contains the XML from which to read the object
 * @param rootName The QName to use for the root tag, if `null` it will be automatically detected.
 */
public inline fun <reified T> XML.XmlCompanion<*>.decodeFromSource(source: Source, rootName: QName? = null): T =
    instance.decodeFromSource(source, rootName)

/**
 * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
 * though the reified function.
 *
 * @param source An [XmlReader] that contains the XML from which to read the object
 * @param rootName The QName to use for the root tag, if `null` it will be automatically detected.
 * @param serializer The loader to use to read the object
 */
public fun <T> XML.XmlCompanion<*>.decodeFromSource(serializer: DeserializationStrategy<T>, source: Source, rootName: QName? = null): T =
    instance.decodeFromSource(serializer, source, rootName)

/**
 * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
 * though the reified function.
 *
 * @param source An [XmlReader] that contains the XML from which to read the object
 * @param rootName The QName to use for the root tag, if `null` it will be automatically detected.
 */
public inline fun <reified T> XML.decodeFromSource(source: Source, rootName: QName? = null): T {
    return decodeFromSource(serializer<T>(), source, rootName)
}

/**
 * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
 * though the reified function.
 *
 * @param source An [XmlReader] that contains the XML from which to read the object
 * @param rootName The QName to use for the root tag, if `null` it will be automatically detected.
 * @param serializer The loader to use to read the object
 */
public fun <T> XML.decodeFromSource(serializer: DeserializationStrategy<T>, source: Source, rootName: QName? = null): T {
    return decodeFromReader(serializer, xmlStreaming.newReader(source), rootName)
}

/**
 * Decode the sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * There are two modes: a sequence of elements, and as a wrapped collection.
 *
 * Wrapped collections function as expected, and read first the wrapper element, then the
 * elements.
 *
 * For sequence of elements, parsing will either stop on end of the reader, or on an end element.
 * It is the responsbility of the caller to handle (push back) the end of element event where
 * it occurs.
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param source The source used to read from
 * @param wrapperName The name of the wrapping element. Setting this value triggers wrapper mode.
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public inline fun <reified T> XML.XmlCompanion<*>.decodeToSequenceFromSource(source: Source, wrapperName: QName?, elementName: QName? = null): Sequence<T> =
    instance.decodeToSequenceFromSource(source, wrapperName, elementName)

/**
 * Decode the sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * There are two modes: a sequence of elements, and as a wrapped collection.
 *
 * Wrapped collections function as expected, and read first the wrapper element, then the
 * elements.
 *
 * For sequence of elements, parsing will either stop on end of the reader, or on an end element.
 * It is the responsbility of the caller to handle (push back) the end of element event where
 * it occurs.
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param deserializer The deserializer to decode the elements.
 * @param source The source used to read from
 * @param wrapperName The name of the wrapping element. Setting this value triggers wrapper mode.
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public fun <T> XML.XmlCompanion<*>.decodeToSequenceFromSource(deserializer: DeserializationStrategy<T>, source: Source, wrapperName: QName?, elementName: QName? = null): Sequence<T> =
    instance.decodeToSequenceFromSource(deserializer, source, wrapperName, elementName)

/**
 * Decode a wrapped sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * This function will assume an unspecified wrapper element. It will read this element and
 * return a sequence of the child elements,
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param source The source used to read from
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public inline fun <reified T> XML.XmlCompanion<*>.decodeWrappedToSequenceFromSource(source: Source, elementName: QName? = null): Sequence<T> =
    instance.decodeWrappedToSequenceFromSource(source, elementName)

/**
 * Decode a wrapped sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * This function will assume an unspecified wrapper element. It will read this element and
 * return a sequence of the child elements,
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param deserializer The deserializer to decode the elements.
 * @param source The source used to read from
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public fun <T> XML.XmlCompanion<*>.decodeWrappedToSequenceFromSource(deserializer: DeserializationStrategy<T>, source: Source, elementName: QName?): Sequence<T> =
    instance.decodeWrappedToSequenceFromSource(deserializer, source, elementName)

/**
 * Decode the sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * There are two modes: a sequence of elements, and as a wrapped collection.
 *
 * Wrapped collections function as expected, and read first the wrapper element, then the
 * elements.
 *
 * For sequence of elements, parsing will either stop on end of the reader, or on an end element.
 * It is the responsbility of the caller to handle (push back) the end of element event where
 * it occurs.
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param source The source used to read from
 * @param wrapperName The name of the wrapping element. Setting this value triggers wrapper mode.
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public inline fun <reified T> XML.decodeToSequenceFromSource(source: Source, wrapperName: QName?, elementName: QName? = null): Sequence<T> {
    return decodeToSequenceFromSource(serializer<T>(), source, wrapperName, elementName)
}

/**
 * Decode the sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * There are two modes: a sequence of elements, and as a wrapped collection.
 *
 * Wrapped collections function as expected, and read first the wrapper element, then the
 * elements.
 *
 * For sequence of elements, parsing will either stop on end of the reader, or on an end element.
 * It is the responsbility of the caller to handle (push back) the end of element event where
 * it occurs.
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param deserializer The deserializer to decode the elements.
 * @param source The source used to read from
 * @param wrapperName The name of the wrapping element. Setting this value triggers wrapper mode.
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public fun <T> XML.decodeToSequenceFromSource(deserializer: DeserializationStrategy<T>, source: Source, wrapperName: QName?, elementName: QName? = null): Sequence<T> {
    return decodeToSequence(deserializer, xmlStreaming.newReader(source), wrapperName, elementName)
}

/**
 * Decode a wrapped sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * This function will assume an unspecified wrapper element. It will read this element and
 * return a sequence of the child elements,
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param source The source used to read from
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public inline fun <reified T> XML.decodeWrappedToSequenceFromSource(source: Source, elementName: QName? = null): Sequence<T> {
    return decodeWrappedToSequenceFromSource(serializer<T>(), source, elementName)
}

/**
 * Decode a wrapped sequence of elements of type `T` incrementally. The elements are required to not
 * be primitives (that encode to/parse from text).
 *
 * This function will assume an unspecified wrapper element. It will read this element and
 * return a sequence of the child elements,
 *
 * Note that when the element name is not provided, it is detected on the first element.
 * Subsequent elements must have the same name (namespace, localname).
 *
 * @param deserializer The deserializer to decode the elements.
 * @param source The source used to read from
 * @param elementName The name of the element. If null, automatically detected on content.
 */
public fun <T> XML.decodeWrappedToSequenceFromSource(deserializer: DeserializationStrategy<T>, source: Source, elementName: QName? = null): Sequence<T> {
    return decodeWrappedToSequence(deserializer, xmlStreaming.newReader(source), elementName)
}
