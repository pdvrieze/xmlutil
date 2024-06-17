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

import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node


public expect fun IXmlStreaming.newWriter(
    output: Appendable,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.None
): XmlWriter

public expect fun IXmlStreaming.newWriter(
    writer: Writer,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.None
): XmlWriter


@Suppress("UnusedReceiverParameter")
public fun IXmlStreaming.newGenericWriter(
    output: Appendable,
    isRepairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.None
): KtXmlWriter = KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)


public expect val xmlStreaming: IXmlStreaming

/**
 * This class is the entry point for creating [XmlReader] and [XmlWriter]
 * instances. Some interfaces are common, others are limited to some
 * architectures.
 */
@Deprecated(
    "Don't use directly",
    ReplaceWith(
        "xmlStreaming",
        "nl.adaptivity.xmlutil.xmlStreaming",
        "nl.adaptivity.xmlutil.newWriter",
        "nl.adaptivity.xmlutil.newGenericWriter",
    )
)
public expect object XmlStreaming : IXmlStreaming {
    public fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): KtXmlWriter

    public fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter

    public fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter

    public override val genericDomImplementation: DOMImplementation

    public override fun newWriter(): DomWriter

    @ExperimentalXmlUtilApi
    public override fun newWriter(dest: Node): DomWriter

    public override fun setFactory(factory: XmlStreamingFactory?)

    public override fun newReader(input: CharSequence): XmlReader

    public override fun newReader(reader: Reader): XmlReader

    @ExperimentalXmlUtilApi
    public override fun newReader(source: Node): XmlReader

    public override fun newGenericReader(input: CharSequence): XmlReader

    public override fun newGenericReader(reader: Reader): XmlReader
}


public enum class XmlDeclMode {
    /** Don't emit XML Declaration */
    None,

    /** Emit an xml declaration just containing the xml version number. Only charsets that aren't UTF will be emitted. */
    Minimal,

    /** Emit an xml declaration whatever is provided by default, if possible minimal. */
    Auto,

    /** Emit an xml declaration that includes the character set. */
    Charset;

    public companion object {
        @XmlUtilInternal
        public fun from(value: Boolean): XmlDeclMode = when (value) {
            true -> None
            else -> Auto
        }
    }
}
