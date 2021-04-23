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

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.SerializationProvider
import nl.adaptivity.xmlutil.util.SerializationProvider.XmlDeserializerFun
import nl.adaptivity.xmlutil.util.SerializationProvider.XmlSerializerFun
import java.io.*
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source
import kotlin.reflect.KClass

/**
 * Common base for [XmlStreaming] that provides common additional methods available on
 * jvm platforms that work with Java library types such as [OutputStream],
 * [Writer], [Reader], [InputStream], etc..
 */
abstract class XmlStreamingJavaCommon {

    private val serializationLoader: ServiceLoader<SerializationProvider> by lazy {
        val service = SerializationProvider::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    fun newWriter(result: Result): XmlWriter = newWriter(result, false)

    abstract fun newWriter(result: Result, repairNamespaces: Boolean = false): XmlWriter

    fun newWriter(outputStream: OutputStream, encoding: String) = newWriter(outputStream, encoding, false)

    abstract fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = false): XmlWriter

    fun newWriter(writer: Writer) = newWriter(writer, false)

    @Deprecated("Use version that takes XmlDeclMode")
    final fun newWriter(writer: Writer, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    abstract fun newWriter(writer: Writer, repairNamespaces: Boolean = false, xmlDeclMode: XmlDeclMode = XmlDeclMode.None): XmlWriter

    @Deprecated("Use version that takes XmlDeclMode")
    final fun newWriter(output: Appendable, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter =
        newWriter(output, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    abstract fun newWriter(output: Appendable, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode = XmlDeclMode.None): XmlWriter

    abstract fun newReader(inputStream: InputStream, encoding: String): XmlReader

    abstract fun newReader(reader: Reader): XmlReader

    abstract fun newReader(source: Source): XmlReader

    abstract fun newReader(input: CharSequence): XmlReader

    open fun newReader(inputStr: String): XmlReader = newReader(input=inputStr as CharSequence)

    abstract fun setFactory(factory: XmlStreamingFactory?)

    fun <T : Any> deserializerFor(type: Class<T>) = deserializerFor(type.kotlin)
    fun <T : Any> deserializerFor(klass: KClass<T>): XmlDeserializerFun? {
        for (candidate in serializationLoader) {
            val deSerializer: XmlDeserializerFun? = candidate.deSerializer(klass)
            deSerializer?.let { return it }
        }
        return null
    }

    fun <T : Any> serializerFor(type: Class<T>) = serializerFor(type.kotlin)
    fun <T : Any> serializerFor(klass: KClass<T>): XmlSerializerFun<T>? {
        for (candidate in serializationLoader) {
            val serializer: XmlSerializerFun<T>? = candidate.serializer(klass)
            serializer?.let { return it }
        }
        return null
    }

    inline fun <reified T : Any> serializeAs(target: XmlWriter, value: T) {
        serialize(T::class, target, value)
    }

    fun <T : Any> serialize(target: XmlWriter, value: T) {
        @kotlin.Suppress("UNCHECKED_CAST") // The serializer is for the actual type even when serializers
        // may not be valid for children
        val kClass = value::class as KClass<T>
        serialize(kClass, target, value)
    }

    fun <T : Any> serialize(kClass: KClass<T>, target: XmlWriter, value: T) {
        val serializer = serializerFor(kClass) ?: throw IllegalArgumentException("No serializer for $kClass found")
        serializer(target, value)
    }

    fun <T : Any> deSerialize(input: InputStream, type: Class<T>) = deSerialize(input, type.kotlin)

    fun <T : Any> deSerialize(input: InputStream, kClass: KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")
        return deserializer(newReader(input, "UTF-8"), kClass)
    }

    fun <T : Any> deSerialize(input: Reader, type: Class<T>) = deSerialize(input, type.kotlin)

    fun <T : Any> deSerialize(input: Reader, kClass: KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException(
            "No deserializer for $kClass (${serializationLoader.joinToString { it.javaClass.name }})"
                                                                                    )
        return deserializer(newReader(input), kClass)
    }

    fun <T : Any> deSerialize(input: String, type: Class<T>) = deSerialize(input, type.kotlin)

    fun <T : Any> deSerialize(input: String, kClass: KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")
        return deserializer(newReader(StringReader(input)), kClass)
    }

    fun <T : Any> deSerialize(inputs: Iterable<String>, type: Class<T>) = deSerialize(inputs, type.kotlin)

    fun <T : Any> deSerialize(inputs: Iterable<String>, kClass: KClass<T>): List<T> {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")
        return inputs.map { input -> deserializer(newReader(StringReader(input)), kClass) }
    }

    inline fun <reified T : Any> deSerialize(input: String): T {
        return deSerialize(input, T::class)
    }

    fun <T : Any> deSerialize(reader: Source, type: Class<T>): T {
        return deSerialize(reader, type.kotlin)
    }

    fun <T : Any> deSerialize(reader: Source, kClass: KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")

        return deserializer(newReader(reader), kClass)
    }

    abstract fun toCharArray(content: Source): CharArray

    abstract fun toString(source: Source): String
}