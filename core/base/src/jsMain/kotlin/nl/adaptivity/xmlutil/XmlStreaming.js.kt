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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.dom.DOMImplementationImpl
import nl.adaptivity.xmlutil.core.impl.dom.unWrap
import nl.adaptivity.xmlutil.core.impl.dom.wrap
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.firstChild
import nl.adaptivity.xmlutil.dom2.nextSibling
import org.w3c.dom.ParentNode
import org.w3c.dom.parsing.DOMParser
import org.w3c.dom.parsing.XMLSerializer
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Node as DomNode

public actual interface XmlStreamingFactory


@Deprecated(
    "Don't use directly", ReplaceWith(
        "xmlStreaming",
        "nl.adaptivity.xmlutil.xmlStreaming",
        "nl.adaptivity.xmlutil.newWriter",
        "nl.adaptivity.xmlutil.newGenericWriter",
    )
)
internal actual object XmlStreaming : IXmlStreaming {
    @ExperimentalXmlUtilApi
    actual override fun newReader(source: Node2): XmlReader {
        @Suppress("DEPRECATION")
        return DomReader(source)
    }

    actual override fun newWriter(): DomWriter {
        return DomWriter()
    }

    @Suppress("DEPRECATION")
    actual override fun newWriter(dest: Node2): DomWriter = DomWriter(dest)

    @Deprecated("Does not work on Javascript except for setting null", level = DeprecationLevel.ERROR)
    public actual override fun setFactory(factory: XmlStreamingFactory?) {
        if (factory != null)
            throw UnsupportedOperationException("Javascript has no services, don't bother creating them")
    }

    public actual override fun newReader(input: CharSequence, expandEntities: Boolean): XmlReader {
        // fall back to generic reader for contexts without DOM (Node etc.)
        if (jsTypeOf(js("DOMParser")) == "undefined") return newGenericReader(input, expandEntities)

        val str = when { // Ignore initial BOM (it parses incorrectly without exception)
            input[0] == '\ufeff' -> input.subSequence(1, input.length)
            else -> input
        }.toString()

        @Suppress("DEPRECATION")
        return DomReader(DOMParser().parseFromString(str, "text/xml").wrap() as Node2)
    }

    public actual override fun newReader(reader: Reader, expandEntities: Boolean): XmlReader =
        KtXmlReader(reader, expandEntities)

    public actual override fun newGenericReader(input: CharSequence, expandEntities: Boolean): XmlReader =
        newGenericReader(StringReader(input), expandEntities = expandEntities)

    public actual override fun newGenericReader(reader: Reader, expandEntities: Boolean): XmlReader =
        KtXmlReader(reader, expandEntities = expandEntities)

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
        // fall back to generic reader for contexts without DOM (Node etc.)
        if (jsTypeOf(js("DOMParser")) == "undefined") return newGenericWriter(output, repairNamespaces, xmlDeclMode)

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
        if (jsTypeOf(js("DOMParser")) == "undefined") return newGenericWriter(writer, repairNamespaces, xmlDeclMode)

        val document = xmlStreaming.genericDomImplementation.createDocument()
        @Suppress("DEPRECATION")
        return WriterXmlWriter(writer, DomWriter(document, xmlDeclMode = xmlDeclMode))
    }

    actual override val genericDomImplementation: DOMImplementation
        get() = DOMImplementationImpl
}

internal class AppendingWriter(private val target: Appendable, private val delegate: DomWriter) :
    XmlWriter by delegate {
    override fun close() {
        try {
            val xmls = XMLSerializer()
            val domText = xmls.serializeToString(delegate.target.unWrap())
            target.append(domText)
        } finally {
            delegate.close()
        }
    }

    override fun flush() {
        delegate.flush()
    }

    override var indent: Int
        @Suppress("DEPRECATION")
        @Deprecated("Use indentString for better accuracy")
        get() = delegate.indent
        set(value) {
            delegate.indent = value
        }

    @Deprecated(
        "Use the version that takes strings",
        replaceWith = ReplaceWith("namespaceAttr(namespacePrefix.toString(), namespaceUri.toString())")
    )
    override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence) {
        @Suppress("DEPRECATION")
        delegate.namespaceAttr(namespacePrefix, namespaceUri)
    }

    override fun namespaceAttr(namespace: Namespace) {
        delegate.namespaceAttr(namespace)
    }

    override fun processingInstruction(target: String, data: String) {
        delegate.processingInstruction(target, data)
    }

    @Deprecated(
        "Use the version that takes strings",
        replaceWith = ReplaceWith("setPrefix(prefix.toString(), namespaceUri.toString())")
    )
    override fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence) {
        @Suppress("DEPRECATION")
        delegate.setPrefix(prefix, namespaceUri)
    }
}

internal class WriterXmlWriter(private val target: Writer, delegate: DomWriter) : XmlWriter by delegate {

    private val delegate = delegate

    private val owner: Node2 = delegate.currentNode ?: delegate.target

    override fun close() {
        try {
            val xmls = XMLSerializer()

            if (delegate.currentNode != null) {
                val domText = buildString {
                    var c = owner.firstChild
                    while (c != null) {
                        append(xmls.serializeToString(c.unWrap()))
                        c = c.nextSibling
                    }
                }

//                val domText2 = xmls.serializeToString((delegate.target as IDocument).delegate)

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
            }
        } finally {
            delegate.close()
        }
    }

    override fun flush() {
        delegate.flush()
    }

    override var indent: Int
        @Suppress("DEPRECATION")
        @Deprecated("Use indentString for better accuracy")
        get() = delegate.indent
        set(value) {
            delegate.indent = value
        }

    @Deprecated(
        "Use the version that takes strings",
        replaceWith = ReplaceWith("namespaceAttr(namespacePrefix.toString(), namespaceUri.toString())")
    )
    override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence) {
        @Suppress("DEPRECATION")
        delegate.namespaceAttr(namespacePrefix, namespaceUri)
    }

    override fun namespaceAttr(namespace: Namespace) {
        delegate.namespaceAttr(namespace)
    }

    override fun processingInstruction(target: String, data: String) {
        delegate.processingInstruction(target, data)
    }

    @Deprecated(
        "Use the version that takes strings",
        replaceWith = ReplaceWith("setPrefix(prefix.toString(), namespaceUri.toString())")
    )
    override fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence) {
        @Suppress("DEPRECATION")
        delegate.setPrefix(prefix, namespaceUri)
    }
}

@Suppress("DEPRECATION")
public actual val xmlStreaming: IXmlStreaming get() = XmlStreaming

@Suppress("UnusedReceiverParameter", "DEPRECATION", "EXTENSION_SHADOWED_BY_MEMBER")
public fun IXmlStreaming.newWriter(): DomWriter = XmlStreaming.newWriter()

@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newWriter(dest: ParentNode): DomWriter = xmlStreaming.newWriter(dest)

@Suppress("UnusedReceiverParameter", "DEPRECATION")
public fun IXmlStreaming.newReader(delegate: DomNode): XmlReader = xmlStreaming.newReader(delegate)


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

