/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil

import org.xmlpull.v1.XmlPullParserException
import java.io.*
import javax.xml.transform.Result
import javax.xml.transform.Source

/**
 * Created by pdvrieze on 21/11/15.
 */
class AndroidStreamingFactory : XmlStreamingFactory {

    @Throws(XmlException::class)
    override fun newWriter(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
        return AndroidXmlWriter(writer, repairNamespaces, omitXmlDecl)
    }

    @Throws(XmlException::class)
    override fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
        return AndroidXmlWriter(outputStream, encoding, repairNamespaces, omitXmlDecl)
    }

    @Throws(XmlException::class)
    override fun newWriter(result: Result, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
        throw UnsupportedOperationException("Results are not supported")
    }

    @Throws(XmlException::class)
    override fun newReader(source: Source): XmlReader {
        throw UnsupportedOperationException("Sources are not supported")
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
