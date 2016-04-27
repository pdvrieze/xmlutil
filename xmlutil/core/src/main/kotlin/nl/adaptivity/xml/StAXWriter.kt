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

import net.devrieze.util.kotlin.asString
import java.io.OutputStream
import java.io.Writer
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.xml.namespace.NamespaceContext
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.Result


/**
 * An implementation of [XmlWriter] that uses an underlying stax writer.
 * Created by pdvrieze on 16/11/15.
 */
class StAXWriter(val delegate: XMLStreamWriter) : AbstractXmlWriter() {
  override var depth:Int = 0
    private set

  @Throws(XMLStreamException::class)
  constructor(writer: Writer, repairNamespaces: Boolean) : this(newFactory(repairNamespaces).createXMLStreamWriter(
        writer)) {
  }

  @Throws(XMLStreamException::class)
  constructor(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean) : this(newFactory(
        repairNamespaces).createXMLStreamWriter(outputStream, encoding)) {
  }

  @Throws(XMLStreamException::class)
  constructor(result: Result, repairNamespaces: Boolean) : this(newFactory(repairNamespaces).createXMLStreamWriter(
        result)) {
  }

  @Throws(XmlException::class)
  override fun startTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?) {
    depth++
    try {
      delegate.writeStartElement(prefix.asString(), localName.toString(), namespace.asString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun endTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?) {
    // TODO add verifying assertions
    try {
      delegate.writeEndElement()
      depth--
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun endDocument() {
    assert(depth == 0) // Don't write this until really the end of the document
    try {
      delegate.writeEndDocument()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("", ReplaceWith("endDocument()"))
  @Throws(XmlException::class)
  fun writeEndDocument() {
    endDocument()
  }

  @Throws(XmlException::class)
  override fun close() {
    try {
      delegate.close()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun flush() {
    try {
      delegate.flush()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun attribute(namespace: CharSequence?, name: CharSequence, prefix: CharSequence?, value: CharSequence) {
    try {
      if (namespace.isNullOrEmpty() || prefix.isNullOrEmpty()) {
        delegate.writeAttribute(name.asString(), value.asString())
      } else {
        delegate.writeAttribute(namespace.asString(), name.toString(), value.toString())
      }
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("", ReplaceWith("attribute(null, localName, null, value)"))
  @Throws(XmlException::class)
  fun writeAttribute(localName: String, value: String) {
    attribute(null, localName, null, value)
  }

  @Deprecated("", ReplaceWith("attribute(namespaceURI, localName, prefix, value)"))
  @Throws(XmlException::class)
  fun writeAttribute(prefix: String, namespaceURI: String, localName: String, value: String) {
    attribute(namespaceURI, localName, prefix, value)
  }

  @Deprecated("", ReplaceWith("attribute(namespaceURI, localName, null, value)"))
  @Throws(XmlException::class)
  fun writeAttribute(namespaceURI: String, localName: String, value: String) {
    attribute(namespaceURI, localName, null, value)
  }

  @Throws(XmlException::class)
  override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence) = try {
    delegate.writeNamespace(namespacePrefix.toString(), namespaceUri.toString())
  } catch (e: XMLStreamException) {
    throw XmlException(e)
  }

  @Throws(XmlException::class)
  override fun comment(text: CharSequence) {
    try {
      delegate.writeComment(text.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("", ReplaceWith("comment(data)"))
  @Throws(XmlException::class)
  fun writeComment(data: String) {
    comment(data)
  }

  @Throws(XmlException::class)
  override fun processingInstruction(text: CharSequence) {
    val textStr = text.toString()
    val split = textStr.indexOf(' ')
    try {
      if (split > 0) {
        delegate.writeProcessingInstruction(textStr.substring(0, split), textStr.substring(split, text.length))
      } else {
        delegate.writeProcessingInstruction(text.toString())
      }
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("", ReplaceWith("processingInstruction(target)"))
  @Throws(XmlException::class)
  fun writeProcessingInstruction(target: String) {
    processingInstruction(target)
  }

  @Deprecated("", ReplaceWith("processingInstruction(target + \" \" + data)"))
  @Throws(XmlException::class)
  fun writeProcessingInstruction(target: String, data: String) {
    processingInstruction(target + " " + data)
  }

  @Throws(XmlException::class)
  override fun cdsect(text: CharSequence) {
    try {
      delegate.writeCData(text.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("", ReplaceWith("cdsect(data)"))
  @Throws(XmlException::class)
  fun writeCData(data: String) {
    cdsect(data)
  }

  @Throws(XmlException::class)
  override fun docdecl(text: CharSequence) {
    try {
      delegate.writeDTD(text.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("", ReplaceWith("docdecl(dtd)"))
  @Throws(XmlException::class)
  fun writeDTD(dtd: String) {
    docdecl(dtd)
  }

  @Throws(XmlException::class)
  override fun entityRef(text: CharSequence) {
    try {
      delegate.writeEntityRef(text.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("", ReplaceWith("entityRef(name)"))
  @Throws(XmlException::class)
  fun writeEntityRef(name: String) {
    entityRef(name)
  }

  @Throws(XmlException::class)
  override fun startDocument(version: CharSequence?, encoding: CharSequence?, standalone: Boolean?) {
    try {
      if (standalone != null && _writeStartDocument !=null && _XMLStreamWriter!!.isInstance(delegate)) {
        try {
          _writeStartDocument.invoke(delegate, version.toString(), encoding.toString(), standalone)
        } catch (e: IllegalAccessException) {
          throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
          throw RuntimeException(e)
        }

      } else {
        delegate.writeStartDocument(encoding.toString(), version.toString()) // standalone doesn't work
      }
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun ignorableWhitespace(text: CharSequence) {
    text(text.toString())
  }

  @Throws(XmlException::class)
  override fun text(text: CharSequence) {
    try {
      delegate.writeCharacters(text.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun getPrefix(namespaceUri: CharSequence?): CharSequence? {
    try {
      return delegate.getPrefix(namespaceUri.asString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence) {
    try {
      delegate.setPrefix(prefix.toString(), namespaceUri.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun getNamespaceUri(prefix: CharSequence): CharSequence? {
    return delegate.namespaceContext.getNamespaceURI(prefix.toString())
  }

  override var namespaceContext: NamespaceContext
    get() = delegate.namespaceContext
    @Throws(XmlException::class)
    set(context) = if (depth == 0) {
      try {
        delegate.namespaceContext = context
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }

    } else {
      throw XmlException("Modifying the namespace context halfway in a document")
    }

  companion object {

    internal val _XMLStreamWriter: Class<out XMLStreamWriter>?
    internal val _writeStartDocument: Method?

    init {
      val clazz: Class<*>?
      var m: Method? = null
      try {
        clazz = StAXWriter::class.java.classLoader.loadClass("org.codehaus.stax2.XMLStreamWriter")
        m = clazz.getMethod("writeStartDocument", String::class.java, String::class.java, Boolean::class.java)
      } catch (e: ClassNotFoundException) {
        clazz = null
      } catch (e: NoSuchMethodException) {
        clazz = null
      }

      //noinspection unchecked
      _XMLStreamWriter = clazz?.asSubclass(XMLStreamWriter::class.java)
      _writeStartDocument = m
    }

    private fun newFactory(repairNamespaces: Boolean): XMLOutputFactory {
      val xmlOutputFactory = XMLOutputFactory.newFactory()
      if (repairNamespaces) xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true)
      return xmlOutputFactory
    }

  }
}
