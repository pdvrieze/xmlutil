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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.*
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import javax.xml.transform.Result
import javax.xml.transform.Source
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer as MPWriter
import java.io.Writer as JavaWriter


public fun IXmlStreaming.newWriter(result: Result, repairNamespaces: Boolean = false): XmlWriter =
    (this as XmlStreamingJavaCommon).newWriter(result, repairNamespaces)

public fun IXmlStreaming.newWriter(
    outputStream: OutputStream,
    encoding: String,
    repairNamespaces: Boolean = false
): XmlWriter =
    (this as XmlStreaming).newWriter(outputStream, encoding, repairNamespaces)

public fun IXmlStreaming.newWriter(
    writer: MPWriter,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.None
): XmlWriter =
    (this as XmlStreaming).newWriter(writer, repairNamespaces, xmlDeclMode)


@Suppress("DEPRECATION")
public fun IXmlStreaming.newReader(inputStream: InputStream, encoding: String): XmlReader =
    (this as XmlStreaming).newReader(inputStream, encoding)

@Suppress("DEPRECATION")
public fun IXmlStreaming.newReader(source: Source): XmlReader =
    (this as XmlStreamingJavaCommon).newReader(source)


/**
 * Common base for [XmlStreaming] that provides common additional methods available on
 * jvm platforms that work with Java library types such as [OutputStream],
 * [MPWriter], [Reader], [InputStream], etc..
 */
@Suppress("DeprecatedCallableAddReplaceWith")
public abstract class XmlStreamingJavaCommon : IXmlStreaming {

    @Deprecated("Use extension functions on IXmlStreaming")
    public fun newWriter(result: Result): XmlWriter = newWriter(result, false)

    public abstract fun newWriter(result: Result, repairNamespaces: Boolean = false): XmlWriter

    @Deprecated("Use extension functions on IXmlStreaming")
    public open fun newWriter(outputStream: OutputStream, encoding: String): XmlWriter =
        newWriter(outputStream, encoding, false)

//    protected abstract fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter

    @Suppress("DEPRECATION")
    @Deprecated("Use extension functions on IXmlStreaming", level = DeprecationLevel.HIDDEN)
    public fun newWriter(writer: MPWriter): XmlWriter = newWriter(writer, false)

    public fun newWriter(writer: JavaWriter): XmlWriter = newWriter(writer, false)

    @Suppress("DEPRECATION")
    @Deprecated("Use version that takes XmlDeclMode")
    public fun newWriter(writer: MPWriter, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter =
        newWriter(writer as Appendable, repairNamespaces, omitXmlDecl)

    @Deprecated("Use version that takes XmlDeclMode")
    public fun newWriter(writer: JavaWriter, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter =
        newWriter(writer as Appendable, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Deprecated("Use extension functions on IXmlStreaming")
    public fun newWriter(writer: MPWriter, repairNamespaces: Boolean): XmlWriter =
        newWriter(writer as Appendable, repairNamespaces)

    public fun newWriter(writer: JavaWriter, repairNamespaces: Boolean): XmlWriter =
        newWriter(writer as Appendable, repairNamespaces, XmlDeclMode.None)

    @Deprecated("Use version that takes XmlDeclMode")
    public fun newWriter(output: Appendable, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter =
        newWriter(output, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Deprecated("Use extension functions on IXmlStreaming")
    public abstract fun newReader(inputStream: InputStream, encoding: String): XmlReader

    @Deprecated("Use extension functions on IXmlStreaming")
    public abstract fun newReader(source: Source): XmlReader

    @Deprecated("Use the version taking a CharSequence")
    public abstract fun newReader(input: String): XmlReader
}
