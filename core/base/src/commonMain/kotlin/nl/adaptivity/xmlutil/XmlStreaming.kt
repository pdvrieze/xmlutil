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

import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node
import kotlin.jvm.JvmOverloads

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
@Deprecated("Use 4 argument version", level = DeprecationLevel.HIDDEN)
public fun IXmlStreaming.newWriter(
    output: Appendable,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
): XmlWriter = newWriter(output, repairNamespaces, xmlDeclMode, XmlVersion.XML10)

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
public expect fun IXmlStreaming.newWriter(
    output: Appendable,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
    xmlVersionHint: XmlVersion = XmlVersion.XML10,
): XmlWriter

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

@Deprecated("Use 4 argument version", level = DeprecationLevel.HIDDEN)
public fun IXmlStreaming.newWriter(
    writer: Writer,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
): XmlWriter = newWriter(writer, repairNamespaces, xmlDeclMode, XmlVersion.XML10)


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
public expect fun IXmlStreaming.newWriter(
    writer: Writer,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
    xmlVersionHint: XmlVersion = XmlVersion.XML10,
): XmlWriter


/**
 * Create a new [XmlWriter] that appends to the given [Appendable]. This writer
 * could be a platform specific writer.
 *
 * @param output The appendable to which the XML will be written
 * @param isRepairNamespaces Should the writer ensure that namespace
 *   declarations are written when needed, even when not explicitly done.
 * @param xmlDeclMode When not explicitly written, this parameter determines
 *   whether the XML declaration is written.
 * @return A platform independent [XmlWriter], generally [nl.adaptivity.xmlutil.core.KtXmlWriter]
 */
@Suppress("UnusedReceiverParameter")
@JvmOverloads
public fun IXmlStreaming.newGenericWriter(
    output: Appendable,
    isRepairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
    xmlVersion: XmlVersion = XmlVersion.XML10,
): KtXmlWriter = KtXmlWriter(output, isRepairNamespaces, xmlDeclMode, xmlVersion)

/**
 * Retrieve a platform independent accessor to create Streaming XML parsing objects
 */
public expect val xmlStreaming: IXmlStreaming

/**
 * This class is the entry point for creating [XmlReader] and [XmlWriter]
 * instances. Some interfaces are common, others are limited to some
 * architectures.
 */
internal expect object XmlStreaming : IXmlStreaming {
    override val genericDomImplementation: DOMImplementation
    override val platformDOMImplementation: PlatformDOMImplementation

    override fun newWriter(): DomWriter

    @ExperimentalXmlUtilApi
    override fun newWriter(dest: Node): DomWriter

    override fun newReader(input: CharSequence, expandEntities: Boolean): XmlReader

    override fun newReader(reader: Reader, expandEntities: Boolean): XmlReader

    @ExperimentalXmlUtilApi
    override fun newReader(source: Node): XmlReader

    override fun newGenericReader(input: CharSequence, expandEntities: Boolean): XmlReader

    override fun newGenericReader(reader: Reader, expandEntities: Boolean): XmlReader
}

/**
 * Mode to use for writing the XML Declaration.
 */
public enum class XmlDeclMode(public val isMinimal: Boolean = false) {
    /** Don't emit XML Declaration */
    None,

    /** Emit an xml declaration just containing the xml version number. Only charsets that aren't UTF will be emitted. */
    Minimal(true),

    /** Emit a declaration only for XML 1.1 and higher (as required by the XML specification), otherwise minimal */
    IfRequired(true) {
        @XmlUtilInternal
        override fun resolve(xmlVersion: XmlVersion?): XmlDeclMode = when (xmlVersion) {
            null,
            XmlVersion.XML10 -> None

            else -> Minimal
        }

    },

    /** Emit an xml declaration whatever is provided by default, if possible minimal. */
    Auto,

    /** Emit an xml declaration that includes the character set. */
    Charset;

    @XmlUtilInternal
    /** Deterimine the effective mode based on the requested version */
    public open fun resolve(xmlVersion: XmlVersion?): XmlDeclMode = this

    public companion object {
        /**
         * Helper function that is used to convert a boolean to [XmlDeclMode]
         */
        @XmlUtilInternal
        public fun from(value: Boolean): XmlDeclMode = when (value) {
            true -> None
            else -> Auto
        }
    }
}
