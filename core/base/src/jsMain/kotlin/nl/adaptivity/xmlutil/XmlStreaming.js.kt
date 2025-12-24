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
import nl.adaptivity.xmlutil.core.impl.dom.wrap
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.createDocument
import org.w3c.dom.ParentNode
import org.w3c.dom.parsing.DOMParser
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Node as DomNode

@Deprecated("XmlStreamingFactory makes no sense in JS", level = DeprecationLevel.ERROR)
public actual interface XmlStreamingFactory

internal actual object XmlStreaming : IXmlStreaming {
    @ExperimentalXmlUtilApi
    actual override fun newReader(source: Node2): XmlReader {
        return DomReader(source, false)
    }

    fun newReader(source: PlatformNode): XmlReader {
        return DomReader(source)
    }

    actual override fun newWriter(): DomWriter {
        return DomWriter()
    }

    actual override fun newWriter(dest: Node2): DomWriter = DomWriter(dest)

    actual override fun newReader(input: CharSequence, expandEntities: Boolean): XmlReader {
        // fall back to generic reader for contexts without DOM (Node etc.)
        if (jsTypeOf(js("DOMParser")) == "undefined") return newGenericReader(input, expandEntities)

        val str = when { // Ignore initial BOM (it parses incorrectly without exception)
            input[0] == '\ufeff' -> input.subSequence(1, input.length)
            else -> input
        }.toString()

        return DomReader(DOMParser().parseFromString(str, "text/xml").wrap())
    }

    actual override fun newReader(reader: Reader, expandEntities: Boolean): XmlReader =
        KtXmlReader(reader, expandEntities)

    actual override fun newGenericReader(input: CharSequence, expandEntities: Boolean): XmlReader =
        newGenericReader(StringReader(input), expandEntities = expandEntities)

    actual override fun newGenericReader(reader: Reader, expandEntities: Boolean): XmlReader =
        KtXmlReader(reader, expandEntities = expandEntities)

    fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): XmlWriter {
        // fall back to generic reader for contexts without DOM (Node etc.)
        if (jsTypeOf(js("DOMParser")) == "undefined") return newGenericWriter(output, repairNamespaces, xmlDeclMode)

        return AppendableXmlWriter(output, DomWriter(xmlDeclMode))
    }

    fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): KtXmlWriter {
        return KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)
    }

    fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean /*= false*/,
        xmlDeclMode: XmlDeclMode /*= XmlDeclMode.None*/,
    ): XmlWriter {
        if (jsTypeOf(js("DOMParser")) == "undefined") return newGenericWriter(writer, repairNamespaces, xmlDeclMode)

        val document = xmlStreaming.genericDomImplementation.createDocument()
        return WriterXmlWriter(writer, DomWriter(document, xmlDeclMode = xmlDeclMode))
    }

    actual override val genericDomImplementation: DOMImplementation
        get() = DOMImplementationImpl

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    actual override val platformDOMImplementation: PlatformDOMImplementation
        get() = DOMImplementationImpl.delegate as PlatformDOMImplementation
}

/**
 * Retrieve a platform independent accessor to create Streaming XML parsing objects
 */
public actual val xmlStreaming: IXmlStreaming get() = XmlStreaming

/**
 * Create a new [DomWriter] that results in writing to a new DOM document.
 * @return The [DomWriter]
 */
@Suppress("UnusedReceiverParameter", "EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use the member function", level = DeprecationLevel.HIDDEN)
public fun IXmlStreaming.newWriter(): DomWriter = XmlStreaming.newWriter()

/**
 * Create a new [DomWriter] that results in writing to DOM with [dest] as the receiver node.
 * @param dest Destination node that will be the root
 * @return The [DomWriter]
 */
@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newWriter(dest: ParentNode): DomWriter = XmlStreaming.newWriter(dest as Node2)

/**
 * Create a new XML reader with the given source node as starting point.  Depending on the
 * configuration, this parser can be platform specific.
 * @param delegate The node to expose
 * @return A (potentially platform specific) [XmlReader], generally a [DomReader]
 */
@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newReader(delegate: DomNode): XmlReader = XmlStreaming.newReader(delegate as Node2)

/**
 * Create a new [XmlWriter] that appends to the given [Appendable]. This writer
 * could be a platform specific writer.
 *
 * @param output The appendable to which the XML will be written
 * @param repairNamespaces Should the writer ensure that namespace
 *   declarations are written when needed, even when not explicitly done.
 * @param xmlDeclMode When not explicitly written, this parameter determines
 *   whether the XML declaration is written.
 * @return A (potentially platform specific) [XmlWriter]
 */
public actual fun IXmlStreaming.newWriter(
    output: Appendable,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode
): XmlWriter = XmlStreaming.newWriter(output, repairNamespaces, xmlDeclMode)

/**
 * Create a new [XmlWriter] that appends to the given [Writer]. This writer
 * could be a platform specific writer.
 *
 * @param writer The writer to which the XML will be written. This writer
 *   will be closed by the [XmlWriter]
 * @param repairNamespaces Should the writer ensure that namespace
 *   declarations are written when needed, even when not explicitly done.
 * @param xmlDeclMode When not explicitly written, this parameter determines
 *   whether the XML declaration is written.
 * @return A (potentially platform specific) [XmlWriter]
 */
public actual fun IXmlStreaming.newWriter(
    writer: Writer,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode,
): XmlWriter = XmlStreaming.newWriter(writer, repairNamespaces, xmlDeclMode)

