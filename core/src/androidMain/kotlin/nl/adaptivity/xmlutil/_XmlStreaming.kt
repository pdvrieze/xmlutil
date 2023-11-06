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
@file:JvmName("XmlStreamingAndroidKt")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.XmlStreaming.deSerialize
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.XmlStreamingJavaCommon
import nl.adaptivity.xmlutil.util.SerializationProvider
import java.io.*
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source
import kotlin.reflect.KClass


public actual object XmlStreaming : XmlStreamingJavaCommon() {

    override val serializationLoader: ServiceLoader<SerializationProvider> by lazy {
        val service = SerializationProvider::class.java
        ServiceLoader.load(service, service.classLoader)
    }


    private val serviceLoader: ServiceLoader<XmlStreamingFactory> by lazy {
        val service = XmlStreamingFactory::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    @Suppress("ObjectPropertyName")
    private var _factory: XmlStreamingFactory? = AndroidStreamingFactory()

    private val factory: XmlStreamingFactory
        get() {
            return _factory ?: serviceLoader.first().apply { _factory = this }
        }

    override fun newWriter(result: Result, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(result, repairNamespaces)
    }

    override fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(outputStream, encoding, repairNamespaces)
    }

    public actual override fun newWriter(writer: Writer, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        return factory.newWriter(writer, repairNamespaces, xmlDeclMode)
    }

    public actual override fun newWriter(output: Appendable, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        return factory.newWriter(output, repairNamespaces, xmlDeclMode)
    }

    public actual fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): KtXmlWriter {
        return KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)
    }

    override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        return factory.newReader(inputStream, encoding)
    }

    actual override fun newReader(reader: Reader): XmlReader {
        return factory.newReader(reader)
    }

    override fun newReader(source: Source): XmlReader {
        return factory.newReader(source)
    }

    public actual override fun newReader(input: CharSequence): XmlReader {
        return factory.newReader(input)
    }

    override fun newReader(inputStr: String): XmlReader {
        return factory.newReader(inputStr)
    }

    public actual fun newGenericReader(input: CharSequence): XmlReader =
        newGenericReader(StringReader(input.toString()))

    public fun newGenericReader(input: String): XmlReader =
        newGenericReader(StringReader(input))

    public fun newGenericReader(inputStream: InputStream, encoding: String?): XmlReader =
        KtXmlReader(inputStream, encoding)

    public actual fun newGenericReader(reader: Reader): XmlReader = KtXmlReader(reader)

    public actual override fun setFactory(factory: XmlStreamingFactory?) {
        _factory = factory ?: AndroidStreamingFactory()
    }

    override fun toCharArray(content: Source): CharArray {
        return newReader(content).toCharArrayWriter().toCharArray()
    }

    override fun toString(source: Source): String {
        return newReader(source).toCharArrayWriter().toString()
    }

    override fun <T : Any> deSerialize(input: InputStream, type: Class<T>): T = deSerialize(input, type.kotlin)
    override fun <T : Any> deSerialize(input: Reader, type: Class<T>): T = deSerialize(input, type.kotlin)
    override fun <T : Any> deSerialize(input: Reader, kClass: KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException(
            "No deserializer for $kClass (${serializationLoader.joinToString { it.javaClass.name }})"
                                                                                    )
        return deserializer(newReader(input), kClass)
    }

    override fun <T : Any> deSerialize(input: String, type: Class<T>): T = deSerialize(input, type.kotlin)
    override fun <T : Any> deSerialize(inputs: Iterable<String>, type: Class<T>): List<T> = deSerialize(inputs, type.kotlin)
    override fun <T : Any> deSerialize(reader: Source, type: Class<T>): T {
        return deSerialize(reader, type.kotlin)
    }

    override fun <T : Any> deserializerFor(type: Class<T>): SerializationProvider.XmlDeserializerFun? = deserializerFor(type.kotlin)
    override fun <T : Any> serializerFor(type: Class<T>): SerializationProvider.XmlSerializerFun<T>? = serializerFor(type.kotlin)

}


public inline fun <reified T : Any> deserialize(input: InputStream): T = deSerialize(input, T::class.java)

public inline fun <reified T : Any> deserialize(input: Reader): T = deSerialize(input, T::class.java)

public inline fun <reified T : Any> deserialize(input: String): T = deSerialize(input, T::class.java)
