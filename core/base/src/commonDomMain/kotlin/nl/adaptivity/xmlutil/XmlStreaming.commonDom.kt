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
@Deprecated(
    "Don't use directly", ReplaceWith(
        "xmlStreaming",
        "nl.adaptivity.xmlutil.xmlStreaming",
        "nl.adaptivity.xmlutil.newWriter",
        "nl.adaptivity.xmlutil.newGenericWriter",
    )
)
public actual object XmlStreaming : IXmlStreaming {

    public actual override fun setFactory(factory: XmlStreamingFactory?) {
        throw UnsupportedOperationException("Native does not support setting the factory")
    }

    @Deprecated("Does not work", level = DeprecationLevel.ERROR)
    public inline fun <reified T : Any> deSerialize(@Suppress("UNUSED_PARAMETER") input: String): T {
        throw UnsupportedOperationException("Cannot work")
    }

    public actual override fun newReader(input: CharSequence): XmlReader {
        return KtXmlReader(StringReader(input.toString()))
    }

    public actual override fun newReader(reader: Reader): XmlReader {
        return newGenericReader(reader)
    }

    public actual override fun newGenericReader(input: CharSequence): XmlReader =
        newGenericReader(StringReader(input.toString()))

    public actual override fun newGenericReader(reader: Reader): XmlReader = KtXmlReader(reader)

    @ExperimentalXmlUtilApi
    actual override fun newReader(source: Node): XmlReader {
        @Suppress("DEPRECATION")
        return DomReader(source)
    }

    actual override fun newWriter(): DomWriter = DomWriter()

    @Suppress("DEPRECATION")
    actual override fun newWriter(dest: Node): DomWriter = DomWriter(dest)

    public fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean,
        omitXmlDecl: Boolean
    ): XmlWriter {
        @Suppress("DEPRECATION")
        return newWriter(output, repairNamespaces, XmlDeclMode.from(omitXmlDecl))
    }

    @Deprecated("Use overload in IXmlStreaming")
    public actual fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): XmlWriter {
        return KtXmlWriter(output, repairNamespaces, xmlDeclMode)
    }

    public actual fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): KtXmlWriter {
        return KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)
    }

    public fun newWriter(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
        @Suppress("DEPRECATION")
        return newWriter(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))
    }

    @Deprecated("Use overload on IXmlStreaming", ReplaceWith("newWriter(writer, repairNamespace, xmlDeclMode"))
    public actual fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): XmlWriter {
        return KtXmlWriter(writer, repairNamespaces, xmlDeclMode)
    }

    actual override val genericDomImplementation: DOMImplementation
        get() = SimpleDOMImplementation
}

@Suppress("DEPRECATION")
public actual val xmlStreaming: IXmlStreaming
    get() = XmlStreaming


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

