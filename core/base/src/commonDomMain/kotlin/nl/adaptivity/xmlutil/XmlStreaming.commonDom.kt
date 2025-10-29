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
import nl.adaptivity.xmlutil.core.impl.dom.SimpleDOMImplementation
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
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
        return DomReader(source)
    }

    actual override fun newWriter(): DomWriter = DomWriter()

    actual override fun newWriter(dest: Node): DomWriter = DomWriter(dest)

    fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): XmlWriter {
        return KtXmlWriter(output, repairNamespaces, xmlDeclMode)
    }

    fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): XmlWriter {
        return KtXmlWriter(writer, repairNamespaces, xmlDeclMode)
    }

    actual override val genericDomImplementation: DOMImplementation
        get() = SimpleDOMImplementation
}

public actual val xmlStreaming: IXmlStreaming get() = XmlStreaming


public actual fun IXmlStreaming.newWriter(
    output: Appendable,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode
): XmlWriter = XmlStreaming.newWriter(output, repairNamespaces, xmlDeclMode)

public actual fun IXmlStreaming.newWriter(
    writer: Writer,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode,
): XmlWriter = XmlStreaming.newWriter(writer, repairNamespaces, xmlDeclMode)

