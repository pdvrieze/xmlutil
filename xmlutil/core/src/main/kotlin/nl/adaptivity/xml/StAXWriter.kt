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
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.Result

import java.io.OutputStream
import java.io.Writer
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


/**
 * An implementation of [XmlWriter] that uses an underlying stax writer.
 * Created by pdvrieze on 16/11/15.
 */
class StAXWriter(private val mDelegate: XMLStreamWriter) : AbstractXmlWriter() {
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
      mDelegate.writeStartElement(prefix.asString(), localName.toString(), namespace.asString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun endTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?) {
    // TODO add verifying assertions
    try {
      mDelegate.writeEndElement()
      depth--
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun endDocument() {
    assert(depth == 0) // Don't write this until really the end of the document
    try {
      mDelegate.writeEndDocument()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeEndDocument() {
    endDocument()
  }

  @Throws(XmlException::class)
  override fun close() {
    try {
      mDelegate.close()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun flush() {
    try {
      mDelegate.flush()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun attribute(namespace: CharSequence?, name: CharSequence, prefix: CharSequence?, value: CharSequence) {
    try {
      if (namespace.isNullOrEmpty() || prefix.isNullOrEmpty()) {
        mDelegate.writeAttribute(name.asString(), value.asString())
      } else {
        mDelegate.writeAttribute(namespace.asString(), name.toString(), value.toString())
      }
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeAttribute(localName: String, value: String) {
    attribute(null, localName, null, value)
  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeAttribute(prefix: String, namespaceURI: String, localName: String, value: String) {
    attribute(namespaceURI, localName, prefix, value)
  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeAttribute(namespaceURI: String, localName: String, value: String) {
    attribute(namespaceURI, localName, null, value)
  }

  @Throws(XmlException::class)
  override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence) = try {
    mDelegate.writeNamespace(namespacePrefix.toString(), namespaceUri.toString())
  } catch (e: XMLStreamException) {
    throw XmlException(e)
  }

  @Throws(XmlException::class)
  override fun comment(text: CharSequence) {
    try {
      mDelegate.writeComment(text.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("")
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
        mDelegate.writeProcessingInstruction(textStr.substring(0, split), textStr.substring(split, text.length))
      } else {
        mDelegate.writeProcessingInstruction(text.toString()!!)
      }
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeProcessingInstruction(target: String) {
    processingInstruction(target)
  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeProcessingInstruction(target: String, data: String) {
    processingInstruction(target + " " + data)
  }

  @Throws(XmlException::class)
  override fun cdsect(text: CharSequence) {
    try {
      mDelegate.writeCData(text.toString()!!)
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeCData(data: String) {
    cdsect(data)
  }

  @Throws(XmlException::class)
  override fun docdecl(dtd: CharSequence) {
    try {
      mDelegate.writeDTD(dtd.toString()!!)
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeDTD(dtd: String) {
    docdecl(dtd)
  }

  @Throws(XmlException::class)
  override fun entityRef(name: CharSequence) {
    try {
      mDelegate.writeEntityRef(name.toString()!!)
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Deprecated("")
  @Throws(XmlException::class)
  fun writeEntityRef(name: String) {
    entityRef(name)
  }

  @Throws(XmlException::class)
  override fun startDocument(version: CharSequence, encoding: CharSequence, standalone: Boolean?) {
    try {
      if (standalone != null && _writeStartDocument !=null && _XMLStreamWriter2!!.isInstance(mDelegate)) {
        try {
          _writeStartDocument.invoke(mDelegate, version.toString(), encoding.toString(), standalone)
        } catch (e: IllegalAccessException) {
          throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
          throw RuntimeException(e)
        }

      } else {
        mDelegate.writeStartDocument(encoding.toString()!!, version.toString()!!) // standalone doesn't work
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
      mDelegate.writeCharacters(text.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun getPrefix(namespaceUri: CharSequence?): CharSequence? {
    try {
      return mDelegate.getPrefix(namespaceUri.asString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence) {
    try {
      mDelegate.setPrefix(prefix.toString(), namespaceUri.toString())
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun getNamespaceUri(prefix: CharSequence): CharSequence? {
    return mDelegate.namespaceContext.getNamespaceURI(prefix.toString())
  }

  @Throws(XmlException::class)
  fun setDefaultNamespace(uri: String) {
    setPrefix(XMLConstants.DEFAULT_NS_PREFIX, uri)
  }

  override var namespaceContext: NamespaceContext
    get() = mDelegate.namespaceContext
    @Throws(XmlException::class)
    set(context) = if (depth == 0) {
      try {
        mDelegate.namespaceContext = context
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }

    } else {
      throw XmlException("Modifying the namespace context halfway in a document")
    }

  companion object {

    internal val _XMLStreamWriter2: Class<out XMLStreamWriter>?
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
      _XMLStreamWriter2 = clazz?.asSubclass(XMLStreamWriter::class.java)
      _writeStartDocument = m
    }

    private fun newFactory(repairNamespaces: Boolean): XMLOutputFactory {
      val xmlOutputFactory = XMLOutputFactory.newFactory()
      if (repairNamespaces) xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true)
      return xmlOutputFactory
    }

    // XXX remove these ASAP
    @Deprecated("Use asString", ReplaceWith("charSequence.asString()","nl.adaptivity.xml.asString"), DeprecationLevel.ERROR)
    private fun toString(charSequence: CharSequence?): String? {
      return charSequence?.toString()
    }

    @Deprecated("Use toString", ReplaceWith("charSequence.toString()"), DeprecationLevel.ERROR)
    @JvmName("toStringNotNull")
    private fun toString(charSequence: CharSequence): String {
      return charSequence?.toString()
    }
  }
}
