/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import org.w3c.dom.Node
import org.w3c.dom.ParentNode
import org.w3c.dom.parsing.DOMParser
import org.w3c.dom.parsing.XMLSerializer
import kotlin.reflect.KClass

public actual interface XmlStreamingFactory


@Deprecated("Don't use directly", ReplaceWith("xmlStreaming",
    "nl.adaptivity.xmlutil.xmlStreaming",
    "nl.adaptivity.xmlutil.newWriter",
    "nl.adaptivity.xmlutil.newGenericWriter",
))
public actual object XmlStreaming: IXmlStreaming {


    internal fun newWriter(): DomWriter {
        return DomWriter()
    }

    internal fun newWriter(dest: ParentNode): DomWriter {
        return DomWriter(dest as nl.adaptivity.xmlutil.dom.Node)
    }


    internal fun newReader(delegate: Node): DomReader {
        return DomReader(delegate as nl.adaptivity.xmlutil.dom.Node)
    }

    @Deprecated("Does not work on Javascript except for setting null", level = DeprecationLevel.ERROR)
    public override fun setFactory(factory: XmlStreamingFactory?) {
        if (factory != null)
            throw UnsupportedOperationException("Javascript has no services, don't bother creating them")
    }

    @Deprecated("Does not work", level = DeprecationLevel.ERROR)
    @Suppress("UNUSED_PARAMETER")
    public fun <T : Any> deSerialize(input: String, type: KClass<T>): Nothing = TODO("JS does not support annotations")
    /*: T {
        return newReader(input).deSerialize(type)
    }*/

    @Deprecated("This functionality uses service loaders and isn't really needed. Will be removed in 1.0. Not MP compatible", level = DeprecationLevel.ERROR)
    public inline fun <reified T : Any> deSerialize(input: String): T = TODO("JS does not support annotations")
    /*: T {
        return deSerialize(input, T::class)
    }*/

    public override fun newReader(input: CharSequence): XmlReader {
        val str = when { // Ignore initial BOM (it parses incorrectly without exception)
            input.get(0) == '\ufeff' -> input.subSequence(1, input.length)
            else -> input
        }.toString()
        return DomReader(DOMParser().parseFromString(str, "text/xml") as nl.adaptivity.xmlutil.dom.Node)
    }

    public override fun newReader(reader: Reader): XmlReader = KtXmlReader(reader)

    public override fun newGenericReader(input: CharSequence): XmlReader =
        newGenericReader(StringReader(input))

    public override fun newGenericReader(reader: Reader): XmlReader = KtXmlReader(reader)

    public fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean,
        omitXmlDecl: Boolean
    ): XmlWriter {
        return newWriter(output, repairNamespaces, XmlDeclMode.from(omitXmlDecl))
    }

    public actual fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): XmlWriter {
        return AppendingWriter(output, DomWriter(xmlDeclMode))
    }

    public actual fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): KtXmlWriter {
        return KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)
    }

    public fun newWriter(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
        return newWriter(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))
    }

    public actual fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): XmlWriter {
        return WriterXmlWriter(writer, DomWriter(xmlDeclMode))
    }
}

/*
fun <T:Any> DomReader.deSerialize(type: KClass<T>): T {
    TODO("Kotlin JS does not support annotations yet so no way to determine the deserializer")
    val an = type.annotations.firstOrNull { jsTypeOf(it) == kotlin.js.jsClass<XmlDeserializer>().name }
    val deserializer = type.getAnnotation(XmlDeserializer::class.java) ?: throw IllegalArgumentException("Types must be annotated with " + XmlDeserializer::class.java.name + " to be deserialized automatically")

    return type.cast(deserializer.value.java.newInstance().deserialize(this))
}
 */

internal class AppendingWriter(private val target: Appendable, private val delegate: DomWriter) :
    XmlWriter by delegate {
    override fun close() {
        try {
            val xmls = XMLSerializer()
            val domText = xmls.serializeToString(delegate.target as Node)
            target.append(domText)
        } finally {
            delegate.close()
        }
    }

    override fun flush() {
        delegate.flush()
    }
}

internal class WriterXmlWriter(private val target: Writer, private val delegate: DomWriter) : XmlWriter by delegate {
    override fun close() {
        try {
            val xmls = XMLSerializer()
            val domText = xmls.serializeToString(delegate.target as Node)

            val xmlDeclMode = delegate.xmlDeclMode
            if (xmlDeclMode != XmlDeclMode.None) {
                val encoding = when (xmlDeclMode) {
                    XmlDeclMode.Charset -> delegate.requestedEncoding ?: "UTF-8"
                    else -> when (delegate.requestedEncoding?.lowercase()?.startsWith("utf-")) {
                        false -> delegate.requestedEncoding
                        else -> null
                    }
                }

                val xmlVersion = delegate.requestedVersion ?: "1.0"

                target.write("<?xml version=\"")
                target.write(xmlVersion)
                target.write("\"")
                if (encoding != null) {
                    target.write(" encoding=\"")
                    target.write(encoding)
                    target.write("\"")
                }
                target.write("?>")
                if (delegate.indentSequence.isNotEmpty()) {
                    target.write("\n")
                }
            }

            target.write(domText)
        } finally {
            delegate.close()
        }
    }

    override fun flush() {
        delegate.flush()
    }
}

@Suppress("DEPRECATION")
public actual val xmlStreaming: IXmlStreaming get() = XmlStreaming

@Suppress("UnusedReceiverParameter", "DEPRECATION")
public fun IXmlStreaming.newWriter(): DomWriter = XmlStreaming.newWriter()
@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newWriter(dest: ParentNode): DomWriter = xmlStreaming.newWriter(dest)
@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newReader(delegate: Node): DomReader = xmlStreaming.newReader(delegate)


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

