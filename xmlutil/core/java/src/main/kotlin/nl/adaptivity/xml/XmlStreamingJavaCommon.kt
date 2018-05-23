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

import kotlinx.io.StringReader
import kotlinx.io.StringWriter
import nl.adaptivity.util.xml.SerializationProvider
import java.util.*

abstract class XmlStreamingJavaCommon {
    private val serializationLoader: java.util.ServiceLoader<nl.adaptivity.util.xml.SerializationProvider> by lazy {
        val service = SerializationProvider::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    abstract fun newWriter(result: javax.xml.transform.Result, repairNamespaces: kotlin.Boolean = false): nl.adaptivity.xml.XmlWriter

    abstract fun newWriter(outputStream: java.io.OutputStream, encoding: kotlin.String, repairNamespaces: kotlin.Boolean = false): nl.adaptivity.xml.XmlWriter

    abstract fun newWriter(writer: java.io.Writer, repairNamespaces: kotlin.Boolean = false): nl.adaptivity.xml.XmlWriter

    abstract fun newWriter(output: kotlin.text.Appendable, repairNamespaces: kotlin.Boolean, omitXmlDecl: kotlin.Boolean): nl.adaptivity.xml.XmlWriter

    abstract fun newReader(inputStream: java.io.InputStream, encoding: kotlin.String): nl.adaptivity.xml.XmlReader

    abstract fun newReader(reader: java.io.Reader): nl.adaptivity.xml.XmlReader

    abstract fun newReader(source: javax.xml.transform.Source): nl.adaptivity.xml.XmlReader

    abstract fun newReader(input: kotlin.CharSequence): nl.adaptivity.xml.XmlReader

    abstract fun setFactory(factory: nl.adaptivity.xml.XmlStreamingFactory?)

    fun <T : kotlin.Any> deserializerFor(type: java.lang.Class<T>) = deserializerFor(type.kotlin)
    fun <T : kotlin.Any> deserializerFor(klass: kotlin.reflect.KClass<T>): nl.adaptivity.util.xml.SerializationProvider.XmlDeserializerFun? {
        for (candidate in serializationLoader) {
            candidate.deSerializer(klass)?.let { return it }
        }
        return null
    }

    fun <T : kotlin.Any> serializerFor(type: java.lang.Class<T>) = serializerFor(type.kotlin)
    fun <T : kotlin.Any> serializerFor(klass: kotlin.reflect.KClass<T>): nl.adaptivity.util.xml.SerializationProvider.XmlSerializerFun<T>? {
        for (candidate in serializationLoader) {
            candidate.serializer(klass)?.let { return it }
        }
        return null
    }

    inline fun <reified T: kotlin.Any> serializeAs(target: nl.adaptivity.xml.XmlWriter, value: T) {
        serialize(T::class, target, value)
    }

    fun <T: kotlin.Any> serialize(target: nl.adaptivity.xml.XmlWriter, value: T) {
        @kotlin.Suppress("UNCHECKED_CAST") // The serializer is for the actual type even when serializers
        // may not be valid for children
        val kClass = value::class as kotlin.reflect.KClass<T>
        serialize(kClass, target, value)
    }

    fun <T : kotlin.Any> serialize(kClass: kotlin.reflect.KClass<T>, target: nl.adaptivity.xml.XmlWriter, value: T) {
        val serializer = serializerFor(kClass) ?: throw IllegalArgumentException("No serializer for $kClass found")
        serializer(target, value)
    }

    fun <T : kotlin.Any> deSerialize(input: java.io.InputStream, type: java.lang.Class<T>) = deSerialize(input, type.kotlin)

    fun <T : kotlin.Any> deSerialize(input: java.io.InputStream, kClass: kotlin.reflect.KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")
        return deserializer(newReader(input, "UTF-8"), kClass)
    }

    fun <T : kotlin.Any> deSerialize(input: java.io.Reader, type: java.lang.Class<T>) = deSerialize(input, type.kotlin)

    fun <T : kotlin.Any> deSerialize(input: java.io.Reader, kClass: kotlin.reflect.KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")
        return deserializer(newReader(input), kClass)
    }

    fun <T : kotlin.Any> deSerialize(input: kotlin.String, type: java.lang.Class<T>) = deSerialize(input, type.kotlin)

    fun <T : kotlin.Any> deSerialize(input: kotlin.String, kClass: kotlin.reflect.KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")
        return deserializer(newReader(StringReader(input)), kClass)
    }

    fun <T : kotlin.Any> deSerialize(inputs: kotlin.collections.Iterable<kotlin.String>, type: java.lang.Class<T>) = deSerialize(inputs, type.kotlin)

    fun <T : kotlin.Any> deSerialize(inputs: kotlin.collections.Iterable<kotlin.String>, kClass: kotlin.reflect.KClass<T>): kotlin.collections.List<T> {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")
        return inputs.map { input -> deserializer(newReader(StringReader(input)), kClass) }
    }

    inline fun <reified T : kotlin.Any> deSerialize(input: kotlin.String): T {
        return deSerialize(input, T::class)
    }

    fun <T: kotlin.Any> deSerialize(reader: javax.xml.transform.Source, type: java.lang.Class<T>): T {
        return deSerialize(reader, type.kotlin)
    }

    fun <T : kotlin.Any> deSerialize(reader: javax.xml.transform.Source, kClass: kotlin.reflect.KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException("No deserializer for $kClass")

        return deserializer(newReader(reader), kClass)
    }

    abstract fun toCharArray(content: javax.xml.transform.Source): CharArray

    abstract fun toString(source: javax.xml.transform.Source): String

    fun toString(value: nl.adaptivity.xml.XmlSerializable): kotlin.String {
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