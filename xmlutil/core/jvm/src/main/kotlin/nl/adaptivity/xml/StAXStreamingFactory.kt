/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import javax.xml.stream.XMLStreamException
import javax.xml.transform.Result
import javax.xml.transform.Source

class StAXStreamingFactory : XmlStreamingFactory {

  @Throws(XmlException::class)
  override fun newWriter(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
    try {
      return StAXWriter(writer, repairNamespaces, omitXmlDecl)
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }
  }

  @Throws(XmlException::class)
  override fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
    try {
      return StAXWriter(outputStream, encoding, repairNamespaces, omitXmlDecl)
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }
  }

  @Throws(XmlException::class)
  override fun newWriter(result: Result, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
    try {
      return StAXWriter(result, repairNamespaces, omitXmlDecl)
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
      return StAXReader(source)
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }
  }

}