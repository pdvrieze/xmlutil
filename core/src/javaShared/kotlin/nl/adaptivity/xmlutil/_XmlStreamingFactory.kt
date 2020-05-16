/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.io.StringReader
import javax.xml.transform.Result
import javax.xml.transform.Source

actual interface XmlStreamingFactory {

    @Deprecated("Use version with xmlDeclMode")
    fun newWriter(writer: Writer, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    fun newWriter(writer: Writer, repairNamespaces: Boolean = false, xmlDeclMode: XmlDeclMode = XmlDeclMode.None): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = false,
        omitXmlDecl: Boolean
                 ): XmlWriter =
        newWriter(outputStream, encoding, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
                 ): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    fun newWriter(result: Result, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(result, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    fun newWriter(result: Result, repairNamespaces: Boolean = false, xmlDeclMode: XmlDeclMode = XmlDeclMode.None): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    fun newWriter(output: Appendable, repairNamespaces: Boolean = false, omitXmlDecl: Boolean) =
        newWriter(AppendableWriter(output), repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    fun newWriter(output: Appendable, repairNamespaces: Boolean = false, xmlDeclMode: XmlDeclMode = XmlDeclMode.None) =
        newWriter(AppendableWriter(output), repairNamespaces, xmlDeclMode)

    fun newReader(source: Source): XmlReader

    fun newReader(reader: Reader): XmlReader

    fun newReader(inputStream: InputStream, encoding: String = Charsets.UTF_8.name()): XmlReader

    fun newReader(input: CharSequence): XmlReader = newReader(CharsequenceReader(input))

    fun newReader(input: String): XmlReader = newReader(StringReader(input))
}