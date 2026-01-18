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

import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.AppendableWriter
import nl.adaptivity.xmlutil.core.impl.CharsequenceReader
import java.io.*

/**
 * A factory that can be used to customize [xmlStreaming] to use these custom factory functions
 * when not using the explicit generic implementations. Factories are picked up using service
 * loaders.
 *
 * @see IXmlStreaming.setFactory
 */
@Suppress(
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING",
)
public actual interface XmlStreamingFactory {
    public fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
        xmlVersionHint: XmlVersion = XmlVersion.XML10
    ): XmlWriter// = newWriter(writer, repairNamespaces, xmlDeclMode)

    public fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
        xmlVersionHint: XmlVersion = XmlVersion.XML10
    ): XmlWriter

    public fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
        xmlVersionHint: XmlVersion = XmlVersion.XML10
    ): XmlWriter = newWriter(AppendableWriter(output), repairNamespaces, xmlDeclMode, xmlVersionHint)

    public fun newReader(reader: Reader): XmlReader = newReader(reader, false)
    public fun newReader(reader: Reader, expandEntities: Boolean): XmlReader

    public fun newReader(inputStream: InputStream): XmlReader = newReader(inputStream, expandEntities = false)

    /**
     * Version of newReader that autodetects the encoding. It first looks for UTF16/UTF32.
     * Then it looks at the declared encoding in the attribute in 8-bit ascii mode. If not
     * it will use the byte order mark to determine UTF16LE/BE or UTF8.
     *
     * If no other encoding is determined, the used encoding will be UTF-8 per the XML standard.
     */
    public fun newReader(inputStream: InputStream, expandEntities: Boolean): XmlReader = newReader(
        inputStream,
        expandEntities = false
    )

    public fun newReader(inputStream: InputStream, encoding: String = "UTF-8"): XmlReader =
        newReader(inputStream, encoding, false)

    public fun newReader(inputStream: InputStream, encoding: String = "UTF-8", expandEntities: Boolean): XmlReader

    public fun newReader(input: CharSequence): XmlReader = newReader(input, false)

    public fun newReader(input: CharSequence, expandEntities: Boolean): XmlReader =
        newReader(CharsequenceReader(input), expandEntities)

    public fun newReader(input: String): XmlReader = newReader(input, false)
    public fun newReader(input: String, expandEntities: Boolean): XmlReader =
        newReader(StringReader(input), expandEntities)
}
