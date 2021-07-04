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

import java.io.*
import javax.xml.stream.XMLStreamException
import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

public class StAXStreamingFactory : XmlStreamingFactory {

    @Throws(XmlException::class)
    override fun newWriter(writer: Writer, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        try {
            return StAXWriter(writer, repairNamespaces, xmlDeclMode)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    override fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): XmlWriter {
        try {
            return StAXWriter(outputStream, encoding, repairNamespaces, xmlDeclMode)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    override fun newWriter(result: Result, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        try {
            return StAXWriter(result, repairNamespaces, xmlDeclMode)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    override fun newReader(reader: Reader): XmlReader {
        try {
            return StAXReader(reader)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
        try {
            return StAXReader(inputStream, encoding)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    override fun newReader(source: Source): XmlReader {
        try {
            return when (source) {
                is StreamSource -> StAXReader(source)
                else -> {
                    val tf = TransformerFactory.newInstance()
                    val trans = tf.newTransformer()
                    val sw = StringWriter()
                    trans.transform(source, StreamResult(sw))
                    newReader(sw.toString())
                }
            }
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

}
