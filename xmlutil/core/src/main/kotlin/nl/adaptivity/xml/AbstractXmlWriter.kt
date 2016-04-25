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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import net.devrieze.util.StringUtil
import nl.adaptivity.util.xml.XmlDelegatingWriter
import nl.adaptivity.xml.XmlStreaming.EventType
import org.w3c.dom.Node
import javax.xml.namespace.QName
import javax.xml.transform.dom.DOMSource

/**
 * Created by pdvrieze on 16/11/15.
 */
abstract class AbstractXmlWriter : XmlWriter {

  companion object {

    @JvmStatic
    fun XmlWriter.serialize(node: Node) {
      this.serialize(XmlStreaming.newReader(DOMSource(node)))
    }

    @JvmStatic
    fun XmlWriter.serialize(reader: XmlReader) {
      val `in` = reader
      val out = this
      while (`in`.hasNext()) {
        val eventType = `in`.next() ?: break
        when (eventType) {
          EventType.START_DOCUMENT,
          EventType.PROCESSING_INSTRUCTION,
          EventType.DOCDECL,
          EventType.END_DOCUMENT -> {
            if (out.depth <= 0) {
              writeCurrentEvent(`in`, out)
            }
          }
        // otherwise fall through
          else                   -> writeCurrentEvent(`in`, out)
        }
      }

    }


    @JvmStatic
    @Throws(XmlException::class)
    fun writeCurrentEvent(reader: XmlReader, writer: XmlWriter) {
      when (reader.eventType) {
        EventType.START_DOCUMENT         -> writer.startDocument(null, reader.encoding, reader.standalone)
        EventType.START_ELEMENT          -> {
          writer.startTag(reader.namespaceUri, reader.localName, reader.prefix)
          run {
            for (i in reader.namespaceIndices) {
              writer.namespaceAttr(reader.getNamespacePrefix(i), reader.getNamespaceUri(i))
            }
          }
          run {
            for (i in reader.attributeIndices) {
              writer.attribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                               null, reader.getAttributeValue(i))
            }
          }
        }
        EventType.END_ELEMENT            -> writer.endTag(reader.namespaceUri, reader.localName,
                                                          reader.prefix)
        EventType.COMMENT                -> writer.comment(reader.text)
        EventType.TEXT                   -> writer.text(reader.text)
        EventType.ATTRIBUTE              -> writer.attribute(reader.namespaceUri, reader.localName,
                                                             reader.prefix, reader.text)
        EventType.CDSECT                 -> writer.cdsect(reader.text)
        EventType.DOCDECL                -> writer.docdecl(reader.text)
        EventType.END_DOCUMENT           -> writer.endDocument()
        EventType.ENTITY_REF             -> writer.entityRef(reader.text)
        EventType.IGNORABLE_WHITESPACE   -> writer.ignorableWhitespace(reader.text)
        EventType.PROCESSING_INSTRUCTION -> writer.processingInstruction(reader.text)
        else                             -> throw XmlException("Unsupported element found")
      }
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun XmlWriter.endTag(predelemname: QName) {
      this.endTag(predelemname.namespaceURI, predelemname.localPart, predelemname.prefix)
    }

    @JvmStatic
    fun XmlWriter.filterSubstream(): XmlWriter {
      return SubstreamFilterWriter(this)
    }


    @Throws(XmlException::class)
    private fun undeclaredPrefixes(reader: XmlReader,
                                   reference: XmlWriter,
                                   missingNamespaces: MutableMap<String, String>) {
      assert(reader.eventType === EventType.START_ELEMENT)
      val prefix = StringUtil.toString(reader.prefix)
      if (prefix != null) {
        if (!missingNamespaces.containsKey(prefix)) {
          val uri = reader.namespaceUri
          if (StringUtil.isEqual(reference.getNamespaceUri(prefix)!!,
                                 uri) && reader.isPrefixDeclaredInElement(prefix)) {
            return
          } else if (uri.length > 0) {
            if (!StringUtil.isEqual(reference.getNamespaceUri(prefix)!!, uri)) {
              missingNamespaces.put(prefix, uri.toString())
            }
          }
        }
      }
    }


  } // end of companion

  /**
   * Default implementation that merely flushes the stream.
   * @throws XmlException When something fails
   */
  @Throws(XmlException::class)
  override fun close() = flush()
}


private class SubstreamFilterWriter(delegate: XmlWriter) : XmlDelegatingWriter(delegate) {

  override fun processingInstruction(text: CharSequence) { /* ignore */ }

  override fun endDocument() { /* ignore */ }

  override fun docdecl(text: CharSequence) { /* ignore */ }

  override fun startDocument(version: CharSequence?, encoding: CharSequence?, standalone: Boolean?) { /* ignore */ }
}
