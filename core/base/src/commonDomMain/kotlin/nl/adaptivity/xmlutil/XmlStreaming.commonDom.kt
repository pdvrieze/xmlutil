/*
 * Copyright (c) 2024-2026.
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
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.dom.SimpleDOMImplementation
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node

/**
 * This class is the entry point for creating [XmlReader] and [XmlWriter]
 * instances. Some interfaces are common, others are limited to some
 * architectures.
 */
internal actual object XmlStreaming : IXmlStreaming {

    actual override fun newReader(input: CharSequence, expandEntities: Boolean): XmlReader {
        return newGenericReader(input, expandEntities)
    }

    actual override fun newReader(reader: Reader, expandEntities: Boolean): XmlReader {
        return newGenericReader(reader, expandEntities)
    }

    actual override fun newGenericReader(input: CharSequence, expandEntities: Boolean): XmlReader =
        newGenericReader(StringReader(input.toString()), expandEntities = expandEntities)

    actual override fun newGenericReader(reader: Reader, expandEntities: Boolean): XmlReader =
        KtXmlReader(reader, expandEntities = expandEntities)

    @ExperimentalXmlUtilApi
    actual override fun newReader(source: Node): XmlReader {
        return DomReader(source, false)
    }

    actual override fun newWriter(): DomWriter = DomWriter()

    actual override fun newWriter(dest: Node): DomWriter = DomWriter(dest)

    fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode,
        xmlVersionHint: XmlVersion = XmlVersion.XML10
    ): XmlWriter {
        return KtXmlWriter(output, repairNamespaces, xmlDeclMode, xmlVersionHint)
    }

    fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode,
        xmlVersionHint: XmlVersion
    ): XmlWriter {
        return KtXmlWriter(writer, repairNamespaces, xmlDeclMode, xmlVersionHint)
    }

    actual override val genericDomImplementation: DOMImplementation
        get() = SimpleDOMImplementation

    actual override val platformDOMImplementation: PlatformDOMImplementation
        get() = SimpleDOMImplementation
}

/**
 * Retrieve a platform independent accessor to create Streaming XML parsing objects
 */
public actual val xmlStreaming: IXmlStreaming get() = XmlStreaming

/**
 * Create a new [XmlWriter] that appends to the given [Appendable].
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
    xmlDeclMode: XmlDeclMode,
    xmlVersionHint: XmlVersion,
): XmlWriter = XmlStreaming.newWriter(output, repairNamespaces, xmlDeclMode, xmlVersionHint)

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
    xmlVersionHint: XmlVersion,
): XmlWriter = XmlStreaming.newWriter(writer, repairNamespaces, xmlDeclMode, xmlVersionHint)

