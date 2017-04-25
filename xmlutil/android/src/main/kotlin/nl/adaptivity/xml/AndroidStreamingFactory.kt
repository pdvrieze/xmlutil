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

import org.xmlpull.v1.XmlPullParserException

import javax.xml.transform.Result
import javax.xml.transform.Source

import java.io.*


/**
 * Created by pdvrieze on 21/11/15.
 */
class AndroidStreamingFactory : XmlStreamingFactory
{

  @Override
  @Throws(XmlException::class)
  fun newWriter(writer: Writer, repairNamespaces: Boolean): XmlWriter {
    try {
      return AndroidXmlWriter(writer, repairNamespaces)
    } catch (e: XmlPullParserException) {
      throw XmlException(e)
    } catch (e: IOException) {
      throw XmlException(e)
    }

  }

  @Override
  @Throws(XmlException::class)
  fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter
  {
    try
    {
      return AndroidXmlWriter(outputStream, encoding, repairNamespaces)
    } catch (e: XmlPullParserException)
    {
      throw XmlException(e)
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Override
  @Throws(XmlException::class)
  fun newWriter(result: Result, repairNamespaces: Boolean): XmlWriter
  {
    throw UnsupportedOperationException("Results are not supported")
  }

  @Override
  @Throws(XmlException::class)
  fun newReader(source: Source): XmlReader
  {
    throw UnsupportedOperationException("Sources are not supported")
  }

  @Override
  @Throws(XmlException::class)
  fun newReader(reader: Reader): XmlReader
  {
    try
    {
      return AndroidXmlReader(reader)
    } catch (e: XmlPullParserException)
    {
      throw XmlException(e)
    }

  }

  @Override
  @Throws(XmlException::class)
  fun newReader(inputStream: InputStream, encoding: String): XmlReader
  {
    try
    {
      return AndroidXmlReader(inputStream, encoding)
    } catch (e: XmlPullParserException)
    {
      throw XmlException(e)
    }

  }
}
