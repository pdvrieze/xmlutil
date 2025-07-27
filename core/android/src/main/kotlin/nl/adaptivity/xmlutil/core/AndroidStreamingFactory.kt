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

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.*
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import javax.xml.transform.Result
import javax.xml.transform.Source

/**
 * Android version of the streaming factory.
 */
public class AndroidStreamingFactory : XmlStreamingFactory {

    @Throws(XmlException::class)
    override fun newWriter(writer: Writer, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        return KtXmlWriter(writer, repairNamespaces, xmlDeclMode)
    }

    @Throws(XmlException::class)
    override fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): XmlWriter {
        val writer = OutputStreamWriter(outputStream, encoding)
        return KtXmlWriter(writer, repairNamespaces, xmlDeclMode)
    }

    @Throws(XmlException::class)
    @Deprecated("Usage of results only works on the JVM", level = DeprecationLevel.ERROR)
    override fun newWriter(result: Result, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        throw UnsupportedOperationException("Results are not supported on Android")
    }

    @Deprecated("Usage of sources only works on the JVM", level = DeprecationLevel.ERROR)
    @Throws(XmlException::class)
    override fun newReader(source: Source): XmlReader {
        throw UnsupportedOperationException("Sources are not supported on Android")
    }

    @Throws(XmlException::class)
    override fun newReader(reader: Reader, expandEntities: Boolean): XmlReader {
        try {
            return AndroidXmlReader(reader, expandEntities)
        } catch (e: XmlPullParserException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun newReader(inputStream: InputStream, expandEntities: Boolean): XmlReader {
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser().also {
                it.setInput(inputStream, null)
            }
            return AndroidXmlReader(parser, expandEntities)
        } catch (e: XmlPullParserException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun newReader(inputStream: InputStream, encoding: String, expandEntities: Boolean): XmlReader {
        try {
            return AndroidXmlReader(inputStream, encoding, expandEntities)
        } catch (e: XmlPullParserException) {
            throw XmlException(e)
        }

    }

    internal companion object {
        @Suppress("unused")
        internal val DEFAULT_INSTANCE = AndroidStreamingFactory()

        @JvmStatic
        fun provider(): AndroidStreamingFactory = DEFAULT_INSTANCE
    }
}
