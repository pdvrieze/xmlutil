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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.XmlStreamingJavaCommon
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.util.SerializationProvider
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.StringReader
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source
import kotlin.reflect.KClass

@Deprecated("Don't use directly", ReplaceWith("xmlStreaming",
    "nl.adaptivity.xmlutil.xmlStreaming",
    "nl.adaptivity.xmlutil.newWriter",
    "nl.adaptivity.xmlutil.newGenericWriter",
))
public actual object XmlStreaming : XmlStreamingJavaCommon(), IXmlStreaming {

    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override val serializationLoader: ServiceLoader<SerializationProvider> by lazy {
        val service = SerializationProvider::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    private val serviceLoader: ServiceLoader<XmlStreamingFactory> by lazy {
        val service = XmlStreamingFactory::class.java
        ServiceLoader.load(service, service.classLoader)
    }

    private var _factory: XmlStreamingFactory? = StAXStreamingFactory()

    private val factory: XmlStreamingFactory
        get() = _factory ?: serviceLoader.first().also { _factory = it }

    override fun newWriter(result: Result, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(result, repairNamespaces)
    }

    public fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(outputStream, encoding, repairNamespaces)
    }

    @Deprecated("Use extension function on IXmlStreaming", level = DeprecationLevel.WARNING)
    public actual fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): XmlWriter {
        return factory.newWriter(writer, repairNamespaces = repairNamespaces, xmlDeclMode = xmlDeclMode)
    }

    @Deprecated("Use extension function on IXmlStreaming", level = DeprecationLevel.WARNING)
    public actual fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): XmlWriter {
        return factory.newWriter(output, repairNamespaces, xmlDeclMode)
    }

    @Deprecated("Use extension function on IXmlStreaming", level = DeprecationLevel.WARNING)
    public actual fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): KtXmlWriter {
        return KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)
    }

    override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        return factory.newReader(inputStream, encoding)
    }

    override fun newReader(reader: Reader): XmlReader {
        return factory.newReader(reader)
    }

    @Deprecated("Note that sources are inefficient and poorly designed, relying on runtime types")
    override fun newReader(source: Source): XmlReader {
        return factory.newReader(source)
    }

    public override fun newReader(input: CharSequence): XmlReader {
        return factory.newReader(input)
    }

    @Deprecated("Use the version taking a CharSequence")
    public override fun newReader(input: String) : XmlReader = newReader(input as CharSequence)

    public override fun newGenericReader(input: CharSequence): XmlReader =
        newGenericReader(StringReader(input.toString()))

    public fun newGenericReader(input: String): XmlReader =
        newGenericReader(StringReader(input))

    public fun newGenericReader(inputStream: InputStream, encoding: String?): XmlReader =
        KtXmlReader(inputStream, encoding)

    public override fun newGenericReader(reader: Reader): XmlReader = KtXmlReader(reader)

    public override fun setFactory(factory: XmlStreamingFactory?) {
        _factory = factory ?: StAXStreamingFactory()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Note that sources are inefficient and poorly designed, relying on runtime types")
    override fun toCharArray(content: Source): CharArray {
        return newReader(content).toCharArrayWriter().toCharArray()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Note that sources are inefficient and poorly designed, relying on runtime types")
    override fun toString(source: Source): String {
        return newReader(source).toCharArrayWriter().toString()
    }

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override fun <T : Any> deSerialize(input: InputStream, type: Class<T>): T = deSerialize(input, type.kotlin)

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override fun <T : Any> deSerialize(input: Reader, type: Class<T>): T = deSerialize(input, type.kotlin)

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override fun <T : Any> deSerialize(input: Reader, kClass: KClass<T>): T {
        val deserializer = deserializerFor(kClass) ?: throw IllegalArgumentException(
            "No deserializer for $kClass (${serializationLoader.joinToString { it.javaClass.name }})"
        )
        return deserializer(newReader(input), kClass)
    }

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override fun <T : Any> deSerialize(input: String, type: Class<T>): T = deSerialize(input, type.kotlin)

    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    @Suppress("DEPRECATION")
    override fun <T : Any> deSerialize(inputs: Iterable<String>, type: Class<T>): List<T> =
        deSerialize(inputs, type.kotlin)

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override fun <T : Any> deSerialize(reader: Source, type: Class<T>): T {
        return deSerialize(reader, type.kotlin)
    }

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override fun <T : Any> deserializerFor(type: Class<T>): SerializationProvider.XmlDeserializerFun? =
        deserializerFor(type.kotlin)

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override fun <T : Any> serializerFor(type: Class<T>): SerializationProvider.XmlSerializerFun<T>? =
        serializerFor(type.kotlin)

}

@Suppress("DEPRECATION")
public actual val xmlStreaming: IXmlStreaming get() = XmlStreaming


@Suppress("DEPRECATION")
public actual fun IXmlStreaming.newWriter(
    output: Appendable,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode
): XmlWriter = XmlStreaming.newWriter(output, repairNamespaces, xmlDeclMode)

@Suppress("DEPRECATION")
public actual fun IXmlStreaming.newWriter(
    writer: Writer,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode,
): XmlWriter = XmlStreaming.newWriter(writer, repairNamespaces, xmlDeclMode)

