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

import kotlinx.io.*
import kotlinx.serialization.ImplicitReflectionSerializer
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlStreamingFactory
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.SerializationProvider
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source
import kotlin.reflect.KClass

@UseExperimental(ImplicitReflectionSerializer::class)
abstract class XmlStreamingJavaCommon {
    @ImplicitReflectionSerializer
    private val serializationLoader: ServiceLoader<SerializationProvider> by lazy {
        val service = SerializationProvider::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    fun newWriter(result: Result): XmlWriter = newWriter(result, false)

    abstract fun newWriter(result: Result, repairNamespaces: Boolean = false): XmlWriter

    fun newWriter(outputStream: OutputStream, encoding: String) = newWriter(outputStream, encoding, false)

    abstract fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = false): XmlWriter

    fun newWriter(writer: Writer) = newWriter(writer, false)
    abstract fun newWriter(writer: Writer, repairNamespaces: Boolean = false, omitXmlDecl: Boolean = false): XmlWriter

    abstract fun newWriter(output: Appendable, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter

    abstract fun newReader(inputStream: InputStream, encoding: String): XmlReader

    abstract fun newReader(reader: Reader): XmlReader

    abstract fun newReader(source: Source): XmlReader

    abstract fun newReader(input: CharSequence): XmlReader

    abstract fun setFactory(factory: XmlStreamingFactory?)

    fun <T : Any> deserializerFor(type: Class<T>) = deserializerFor(type.kotlin)
    fun <T : Any> deserializerFor(klass: KClass<T>): SerializationProvider.XmlDeserializerFun? {
        for (candidate in serializationLoader) {
            candidate.deSerializer(klass)?.let { return it }
        }
        return null
    }

    fun <T : Any> serializerFor(type: Class<T>) = serializerFor(type.kotlin)
    fun <T : Any> serializerFor(klass: KClass<T>): SerializationProvider.XmlSerializerFun<T>? {
        for (candidate in serializationLoader) {
            candidate.serializer(klass)?.let { return it }
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
                "No deserializer for $kClass (${serializationLoader.joinToString { it.javaClass.name }})")
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

    fun toString(value: XmlSerializable): String {
        return StringWriter().apply {
            val w = newWriter(this@apply)
            try {
                value.serialize(w)
            } finally {
                w.close()
            }
        }.toString()
    }
}