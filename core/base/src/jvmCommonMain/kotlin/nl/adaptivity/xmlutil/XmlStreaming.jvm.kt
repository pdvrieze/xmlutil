/*
 * Copyright (c) 2024-2025.
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

//@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.CharsequenceReader
import nl.adaptivity.xmlutil.core.impl.dom.DOMImplementationImpl
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node
import java.io.*
import java.util.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer as MPWriter
import org.w3c.dom.Node as DomNode
import java.io.Writer as JavaIoWriter

// Suppression should be fine as the member is hidden
@Suppress("UnusedReceiverParameter", "EXTENSION_SHADOWED_BY_MEMBER")
public fun IXmlStreaming.setFactory(factory: XmlStreamingFactory?) {
    XmlStreaming.setFactoryImpl(factory)
}

internal actual object XmlStreaming : IXmlStreaming {

    private val serviceLoader: ServiceLoader<XmlStreamingFactory>
        get() {
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
            f = runCatching { serviceLoader.firstOrNull() }.getOrNull()

            if (f == null) {
                f = try {
                    Class.forName("nl.adaptivity.xmlutil.StAXStreamingFactory")
                        .getConstructor()
                        .newInstance() as XmlStreamingFactory
                } catch (_: ClassNotFoundException) { /*Doesn't matter */
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

            if (f == null) f = GenericFactory

            _factory = f
            return f
        }


    fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter {
        return factory.newWriter(outputStream, encoding, repairNamespaces)
    }

    actual override fun newWriter(): DomWriter = DomWriter()

    actual override fun newWriter(dest: Node): DomWriter = DomWriter(dest)

    fun newWriter(
        writer: JavaIoWriter,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
    ): XmlWriter {
        return factory.newWriter(writer, repairNamespaces = repairNamespaces, xmlDeclMode = xmlDeclMode)
    }

    fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean, /*= false*/
        xmlDeclMode: XmlDeclMode, /*= XmlDeclMode.None*/
    ): XmlWriter {
        return factory.newWriter(output, repairNamespaces, xmlDeclMode)
    }

    actual override val genericDomImplementation: DOMImplementation
        get() = DOMImplementationImpl

    @ExperimentalXmlUtilApi
    actual override fun newReader(source: Node): XmlReader {
        return DomReader(source)
    }

    fun newReader(inputStream: InputStream): XmlReader {
        return factory.newReader(inputStream, false)
    }

    fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        return factory.newReader(inputStream, encoding)
    }

    actual override fun newReader(reader: Reader, expandEntities: Boolean): XmlReader {
        return factory.newReader(reader, expandEntities)
    }

    actual override fun newReader(input: CharSequence, expandEntities: Boolean): XmlReader {
        return factory.newReader(input, expandEntities)
    }

    actual override fun newGenericReader(input: CharSequence, expandEntities: Boolean): XmlReader =
        newGenericReader(CharsequenceReader(input), expandEntities = expandEntities)

    fun newGenericReader(input: String, expandEntities: Boolean = false): XmlReader =
        newGenericReader(StringReader(input), expandEntities = expandEntities)

    fun newGenericReader(
        inputStream: InputStream,
        encoding: String? = null,
        expandEntities: Boolean = false
    ): XmlReader =
        KtXmlReader(inputStream, encoding, expandEntities = expandEntities)

    actual override fun newGenericReader(reader: Reader, expandEntities: Boolean): XmlReader =
        KtXmlReader(reader, expandEntities = expandEntities)

    // Still hidden to allow for the function to exist in the parent interface (for compatibility).
    @Deprecated("Use the extension method for the JVM platform", level = DeprecationLevel.HIDDEN)
    override fun setFactory(factory: XmlStreamingFactory?) = setFactoryImpl(XmlStreaming.factory)

    internal fun setFactoryImpl(factory: XmlStreamingFactory?) {
        _factory = factory
    }

    internal object GenericFactory : XmlStreamingFactory {
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

        override fun newReader(reader: Reader, expandEntities: Boolean): XmlReader {
            return KtXmlReader(reader, expandEntities)
        }

        override fun newReader(inputStream: InputStream, expandEntities: Boolean): XmlReader {
            return KtXmlReader(inputStream)
        }

        override fun newReader(inputStream: InputStream, encoding: String, expandEntities: Boolean): XmlReader {
            return KtXmlReader(inputStream, encoding, expandEntities)
        }
    }

}

@Suppress("DEPRECATION")
public actual val xmlStreaming: IXmlStreaming get() = XmlStreaming

@Suppress("UnusedReceiverParameter")
@ExperimentalXmlUtilApi
public val IXmlStreaming.genericFactory: XmlStreamingFactory get() = XmlStreaming.GenericFactory

@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newReader(node: DomNode): XmlReader {
    return DomReader(node)
}

@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newReader(inputStream: InputStream): XmlReader {
    return XmlStreaming.newReader(inputStream)
}

@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newReader(inputStream: InputStream, encoding: String): XmlReader =
    XmlStreaming.newReader(inputStream, encoding)

@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newGenericReader(inputStream: InputStream): XmlReader {
    return XmlStreaming.newGenericReader(inputStream)
}

@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newGenericReader(inputStream: InputStream, encoding: String): XmlReader {
    return XmlStreaming.newGenericReader(inputStream, encoding)
}

public fun IXmlStreaming.newWriter(
    outputStream: OutputStream,
    encoding: String,
    repairNamespaces: Boolean = false
): XmlWriter =
    (this as XmlStreaming).newWriter(outputStream, encoding, repairNamespaces)

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

