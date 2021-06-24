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

import nl.adaptivity.xmlutil.core.impl.KtXmlWriter
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import javax.xml.transform.Result
import javax.xml.transform.Source

/**
 * Android version of the streaming factory.
 */
class AndroidStreamingFactory : XmlStreamingFactory {

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
    override fun newWriter(result: Result, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        throw UnsupportedOperationException("Results are not supported on Android")
    }

    @Throws(XmlException::class)
    override fun newReader(source: Source): XmlReader {
        throw UnsupportedOperationException("Sources are not supported on Android")
    }

    @Throws(XmlException::class)
    override fun newReader(reader: Reader): XmlReader {
        try {
            return AndroidXmlReader(reader)
        } catch (e: XmlPullParserException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        try {
            return AndroidXmlReader(inputStream, encoding)
        } catch (e: XmlPullParserException) {
            throw XmlException(e)
        }

    }
}
