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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.XmlStreamingJavaCommon
import nl.adaptivity.xmlutil.core.impl.dom.DOMImplementationImpl
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.util.SerializationProvider
import java.io.*
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source
import kotlin.jvm.Volatile
import kotlin.reflect.KClass
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer as MPWriter
import java.io.Writer as JavaIoWriter

@Deprecated(
    "Don't use directly", ReplaceWith(
        "xmlStreaming",
        "nl.adaptivity.xmlutil.xmlStreaming",
        "nl.adaptivity.xmlutil.newWriter",
        "nl.adaptivity.xmlutil.newGenericWriter",
    )
)
public actual object XmlStreaming : XmlStreamingJavaCommon(), IXmlStreaming {

    @Suppress("DEPRECATION")
    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0")
    override val serializationLoader: ServiceLoader<SerializationProvider> get() {
        val service = SerializationProvider::class.java
        return ServiceLoader.load(service, service.classLoader)
    }

    private val serviceLoader: ServiceLoader<XmlStreamingFactory> get() {
        val service = XmlStreamingFactory::class.java
        return ServiceLoader.load(service, service.classLoader)
    }

    @Volatile
    private var _factory: XmlStreamingFactory? = null

    private val factory: XmlStreamingFactory
        get() {
            var f: XmlStreamingFactory? = _factory

            if (f != null) return f

            // Ignore errors in the service loader, but fall back instead
            f = try { serviceLoader.firstOrNull() } catch (e: ServiceConfigurationError) { null }

            if (f == null) {
                f = try {
                    Class.forName("nl.adaptivity.xmlutil.StAXStreamingFactory")
                        .getConstructor()
                        .newInstance() as XmlStreamingFactory
                } catch (e: ClassNotFoundException) { /*Doesn't matter */
                    null
                }
            }

            if (f == null) {
                f = try {
                    Class.forName("nl.adaptivity.xmlutil.AndroidStreamingFactory")
                        .getConstructor()
                        .newInstance() as XmlStreamingFactory
                } catch (e: ClassNotFoundException) {
                    null
                }
            }

            if (f == null) f = GenericFactory()

            _factory = f
            return f
        }

    override fun newWriter(result: Result, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(result, repairNamespaces)
    }

    public fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(outputStream, encoding, repairNamespaces)
    }

    actual override fun newWriter(): DomWriter = DomWriter()

    @Suppress("DEPRECATION")
    actual override fun newWriter(dest: Node): DomWriter = DomWriter(dest)

    @Deprecated("Use extension function on IXmlStreaming", level = DeprecationLevel.WARNING)
    public actual fun newWriter(
        writer: MPWriter,
        repairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): XmlWriter {
        return factory.newWriter(writer as Appendable, repairNamespaces = repairNamespaces, xmlDeclMode = xmlDeclMode)
    }

    @Deprecated("Use extension function on IXmlStreaming", level = DeprecationLevel.WARNING)
    public fun newWriter(
        writer: JavaIoWriter,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
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

    @Deprecated(
        "Use extension function on IXmlStreaming", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith(
            "KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)",
            "nl.adaptivity.xmlutil.core.KtXmlWriter"
        )
    )
    public actual fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): KtXmlWriter {
        return KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)
    }

    actual override val genericDomImplementation: DOMImplementation
        get() = DOMImplementationImpl

    @Suppress("DEPRECATION")
    @ExperimentalXmlUtilApi
    actual override fun newReader(source: Node): XmlReader {
        return DomReader(source)
    }

    @Deprecated("Use extension functions on IXmlStreaming")
    override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        return factory.newReader(inputStream, encoding)
    }

    actual override fun newReader(reader: Reader): XmlReader {
        return factory.newReader(reader)
    }

    @Deprecated("Note that sources are inefficient and poorly designed, relying on runtime types")
    override fun newReader(source: Source): XmlReader {
        return factory.newReader(source)
    }

    public actual override fun newReader(input: CharSequence): XmlReader {
        return factory.newReader(input)
    }

    @Deprecated(
        "Use the version taking a CharSequence",
        ReplaceWith("newReader(input as CharSequence)", "nl.adaptivity.xmlutil.XmlStreaming.newReader")
    )
    public override fun newReader(input: String): XmlReader = newReader(input as CharSequence)

    public actual override fun newGenericReader(input: CharSequence): XmlReader =
        newGenericReader(StringReader(input.toString()))

    public fun newGenericReader(input: String): XmlReader =
        newGenericReader(StringReader(input))

    public fun newGenericReader(inputStream: InputStream, encoding: String? = null): XmlReader =
        KtXmlReader(inputStream, encoding)

    public actual override fun newGenericReader(reader: Reader): XmlReader = KtXmlReader(reader)

    public actual override fun setFactory(factory: XmlStreamingFactory?) {
        _factory = factory
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Note that sources are inefficient and poorly designed, relying on runtime types", ReplaceWith(
            "newReader(content).toCharArrayWriter().toCharArray()",
            "nl.adaptivity.xmlutil.XmlStreaming.newReader"
        )
    )
    override fun toCharArray(content: Source): CharArray {
        return newReader(content).toCharArrayWriter().toCharArray()
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Note that sources are inefficient and poorly designed, relying on runtime types", ReplaceWith(
            "newReader(source).toCharArrayWriter().toString()",
            "nl.adaptivity.xmlutil.XmlStreaming.newReader"
        )
    )
    override fun toString(source: Source): String {
        return newReader(source).toCharArrayWriter().toString()
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "This functionality uses service loaders and isn't really needed. Will be removed in 1.0",
        ReplaceWith("deSerialize(input, type.kotlin)", "nl.adaptivity.xmlutil.XmlStreaming.deSerialize")
    )
    override fun <T : Any> deSerialize(input: InputStream, type: Class<T>): T = deSerialize(input, type.kotlin)

    @Suppress("DEPRECATION")
    @Deprecated(
        "This functionality uses service loaders and isn't really needed. Will be removed in 1.0",
        ReplaceWith("deSerialize(input, type.kotlin)", "nl.adaptivity.xmlutil.XmlStreaming.deSerialize")
    )
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
    @Deprecated(
        "This functionality uses service loaders and isn't really needed. Will be removed in 1.0",
        ReplaceWith("deSerialize(input, type.kotlin)", "nl.adaptivity.xmlutil.XmlStreaming.deSerialize")
    )
    override fun <T : Any> deSerialize(input: String, type: Class<T>): T = deSerialize(input, type.kotlin)

    @Deprecated(
        "This functionality uses service loaders and isn't really needed. Will be removed in 1.0",
        ReplaceWith("deSerialize(inputs, type.kotlin)", "nl.adaptivity.xmlutil.XmlStreaming.deSerialize")
    )
    @Suppress("DEPRECATION")
    override fun <T : Any> deSerialize(inputs: Iterable<String>, type: Class<T>): List<T> =
        deSerialize(inputs, type.kotlin)

    @Suppress("DEPRECATION")
    @Deprecated(
        "This functionality uses service loaders and isn't really needed. Will be removed in 1.0",
        ReplaceWith("deSerialize(reader, type.kotlin)", "nl.adaptivity.xmlutil.XmlStreaming.deSerialize")
    )
    override fun <T : Any> deSerialize(reader: Source, type: Class<T>): T {
        return deSerialize(reader, type.kotlin)
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "This functionality uses service loaders and isn't really needed. Will be removed in 1.0",
        ReplaceWith("deserializerFor(type.kotlin)", "nl.adaptivity.xmlutil.XmlStreaming.deserializerFor")
    )
    override fun <T : Any> deserializerFor(type: Class<T>): SerializationProvider.XmlDeserializerFun? =
        deserializerFor(type.kotlin)

    @Suppress("DEPRECATION")
    @Deprecated(
        "This functionality uses service loaders and isn't really needed. Will be removed in 1.0",
        ReplaceWith("serializerFor(type.kotlin)", "nl.adaptivity.xmlutil.XmlStreaming.serializerFor")
    )
    override fun <T : Any> serializerFor(type: Class<T>): SerializationProvider.XmlSerializerFun<T>? =
        serializerFor(type.kotlin)

    public class GenericFactory: XmlStreamingFactory {
        override fun newWriter(writer: JavaIoWriter, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
            return KtXmlWriter(writer, repairNamespaces, xmlDeclMode)
        }

        override fun newWriter(
            outputStream: OutputStream,
            encoding: String,
            repairNamespaces: Boolean,
            xmlDeclMode: XmlDeclMode
        ): XmlWriter {
            return KtXmlWriter(OutputStreamWriter(outputStream, encoding), repairNamespaces, xmlDeclMode)
        }

        override fun newReader(reader: Reader): XmlReader {
            return KtXmlReader(reader)
        }

        override fun newReader(inputStream: InputStream): XmlReader {
            return KtXmlReader(inputStream)
        }

        override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
            return KtXmlReader(inputStream, encoding)
        }
    }

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
    writer: MPWriter,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode,
): XmlWriter = XmlStreaming.newWriter(writer, repairNamespaces, xmlDeclMode)

@Suppress("DEPRECATION", "UnusedReceiverParameter")
public fun IXmlStreaming.newWriter(
    writer: JavaIoWriter,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
): XmlWriter = XmlStreaming.newWriter(writer, repairNamespaces, xmlDeclMode)

