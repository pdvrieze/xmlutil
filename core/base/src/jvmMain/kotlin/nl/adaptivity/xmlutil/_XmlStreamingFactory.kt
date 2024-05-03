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

import nl.adaptivity.xmlutil.core.impl.AppendableWriter
import nl.adaptivity.xmlutil.core.impl.CharsequenceReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithoutCompatibility
import java.io.*
import javax.xml.transform.Result
import javax.xml.transform.Source

@Suppress(
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING",
    "DeprecatedCallableAddReplaceWith"
)
@MpJvmDefaultWithoutCompatibility
public actual interface XmlStreamingFactory {

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(writer: Writer, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    public fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = false,
        omitXmlDecl: Boolean
    ): XmlWriter =
        newWriter(outputStream, encoding, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    public fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(result: Result, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(result, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Deprecated("Usage of results is deprecated")
    public fun newWriter(
        result: Result,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter = throw UnsupportedOperationException("Results are not supported by this factory")

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(output: Appendable, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(AppendableWriter(output), repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    public fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter = newWriter(AppendableWriter(output), repairNamespaces, xmlDeclMode)

    @Deprecated("Sources are deprecated (only partially supported)")
    public fun newReader(source: Source): XmlReader =
        throw UnsupportedOperationException("Sources are not supported by this factory")

    public fun newReader(reader: Reader): XmlReader

    public fun newReader(inputStream: InputStream, encoding: String = "UTF-8"): XmlReader

    public fun newReader(input: CharSequence): XmlReader = newReader(CharsequenceReader(input))

    public fun newReader(input: String): XmlReader = newReader(StringReader(input))
}
