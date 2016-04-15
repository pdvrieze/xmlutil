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
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.stream.*
import javax.xml.transform.Source

import java.io.InputStream
import java.io.Reader
import nl.adaptivity.xml.XmlStreaming.EventType


/**
 * An implementation of [XmlReader] based upon the JDK StAX implementation.
 * @author Created by pdvrieze on 16/11/15.
 */
class StAXReader(private val mDelegate: XMLStreamReader) : AbstractXmlReader() {

  override var isStarted = false
    private set(value: Boolean) {
      field = value
    }
  private var mFixWhitespace = false
  override var depth = 0
    private set(value: Int) {
      field = value
    }


  @Throws(XMLStreamException::class)
  constructor(reader: Reader) : this(XMLInputFactory.newFactory().createXMLStreamReader(reader)) {
  }

  @Throws(XMLStreamException::class)
  constructor(inputStream: InputStream, encoding: String) : this(XMLInputFactory.newFactory().createXMLStreamReader(
        inputStream,
        encoding)) {
  }

  @Throws(XMLStreamException::class)
  constructor(source: Source) : this(XMLInputFactory.newFactory().createXMLStreamReader(source)) {
  }

  @Throws(XmlException::class)
  override fun close() {
    try {
      mDelegate.close()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  override fun isEndElement(): Boolean {
    return mDelegate.isEndElement
  }

  val isStandalone: Boolean
    @Deprecated("")
    get() = standalone ?: false

  override fun isCharacters(): Boolean {
    return mDelegate.isCharacters
  }

  override fun isStartElement(): Boolean {
    return mDelegate.isStartElement
  }

  @Throws(XmlException::class)
  override fun isWhitespace(): Boolean {
    return mDelegate.isWhiteSpace
  }

  val isWhiteSpace: Boolean
    @Deprecated("Use alternative name", ReplaceWith("isWhitespace"))
    @Throws(XmlException::class)
    get() = isWhitespace()

  val namespaceURI: String
    @Deprecated("")
    get() = namespaceUri

  override val namespaceUri: String
    get() = mDelegate.namespaceURI

  @Deprecated("")
  fun hasText(): Boolean {
    return mDelegate.hasText()
  }

  @Throws(XmlException::class)
  override fun require(type: EventType, namespace: CharSequence?, name: CharSequence?) {
    try {
      mDelegate.require(LOCAL_TO_DELEGATE[type.ordinal], namespace.asString(), name.asString())
    } catch (e: XMLStreamException) {

      throw XmlException(e)
    }

  }

  val namespaceCount: Int
    @Deprecated("Not needed", ReplaceWith("namespaceEnd - namespaceStart"))
    @Throws(XmlException::class)
    get() = namespaceEnd - namespaceStart

  val textCharacters: CharArray
    @Deprecated("", ReplaceWith("text.toCharArray()"))
    get() = text.toCharArray()

  val characterEncodingScheme: String
    @Deprecated("")
    get() = mDelegate.characterEncodingScheme

  @Deprecated("")
  override fun getAttributeName(i: Int): QName {
    return QName(getAttributeNamespace(i), getAttributeLocalName(i), getAttributePrefix(i))
  }

  override fun getNamespaceUri(prefix: CharSequence): String? {
    return mDelegate.getNamespaceURI(prefix.toString())
  }

  fun getNamespaceURI(prefix: String): String? {
    return getNamespaceUri(prefix)
  }

  @Throws(XmlException::class)
  override fun getNamespacePrefix(namespaceUri: CharSequence): String? {
    return mDelegate.namespaceContext.getPrefix(namespaceUri.toString())
  }

  override val locationInfo: String?
    get() {
      val location = mDelegate.location
      return location?.toString()
    }

  val location: Location
    @Deprecated("")
    get() = mDelegate.location

  override fun getAttributeValue(nsUri: CharSequence?, localName: CharSequence): String? {
    return mDelegate.getAttributeValue(nsUri.asString(), localName.toString())
  }

  override val version: String
    @Deprecated("")
    get() = mDelegate.version

  override val name: QName
    @Deprecated("")
    get() = QName(namespaceUri, localName, prefix)

  @Throws(XmlException::class)
  override fun next(): EventType? {
    isStarted = true
    try {
      if (mDelegate.hasNext()) {
        return updateDepth(fixWhitespace(delegateToLocal(mDelegate.next())))
      } else {
        return null
      }
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  private fun delegateToLocal(eventType: Int) = DELEGATE_TO_LOCAL[eventType] ?: throw XmlException("Unsupported event type")

  @Throws(XmlException::class)
  override fun nextTag(): EventType {
    isStarted = true
    try {
      return updateDepth(fixWhitespace(delegateToLocal(mDelegate.nextTag())))
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }

  }

  private fun fixWhitespace(eventType: EventType): EventType {
    if (eventType === EventType.TEXT) {
      if (isXmlWhitespace(mDelegate.text)) {
        mFixWhitespace = true
        return EventType.IGNORABLE_WHITESPACE
      }
    }
    mFixWhitespace = false
    return eventType
  }

  private fun updateDepth(eventType: EventType) = when (eventType) {
    XmlStreaming.EventType.START_ELEMENT -> { ++depth; eventType }
    XmlStreaming.EventType.END_ELEMENT   -> { --depth; eventType }
    else -> eventType
  }

  @Throws(XmlException::class)
  override fun hasNext(): Boolean {
    try {
      return mDelegate.hasNext()
    } catch (e: XMLStreamException) {
      throw XmlException(e)
    }
  }

  override val attributeCount: Int
    get() = mDelegate.attributeCount

  override fun getAttributeNamespace(i: Int): String {
    return mDelegate.getAttributeNamespace(i)
  }

  override fun getAttributeLocalName(i: Int): String {
    return mDelegate.getAttributeLocalName(i)
  }

  override fun getAttributePrefix(i: Int): String {
    return mDelegate.getAttributePrefix(i)
  }

  override fun getAttributeValue(i: Int): String {
    return mDelegate.getAttributeValue(i)
  }

  override val namespaceStart: Int
    get() = 0

  override val namespaceEnd: Int
    @Throws(XmlException::class)
    get() = mDelegate.namespaceCount

  @Deprecated("Wrong name", ReplaceWith("getNamespaceUri(index)"))
  fun getNamespaceURI(index: Int) = getNamespaceUri(index)

  override fun getNamespaceUri(i: Int) = mDelegate.getNamespaceURI(i)

  override fun getNamespacePrefix(i: Int) = mDelegate.getNamespacePrefix(i)

  override val namespaceContext: NamespaceContext
    get() = mDelegate.namespaceContext

  override val eventType: EventType
    get() = if (mFixWhitespace) EventType.IGNORABLE_WHITESPACE else delegateToLocal(mDelegate.eventType)

  override val text: String
    get() = mDelegate.text

  override val encoding: String
    get() = mDelegate.encoding

  override val localName: String
    get() = mDelegate.localName

  override val prefix: String
    get() = mDelegate.prefix

  val piData: String?
    @Deprecated("")
    get() {
      val text = text
      val index = text.indexOf(' ')
      if (index < 0) {
        return null
      } else {
        return text.substring(index + 1)
      }
    }

  val piTarget: String
    @Deprecated("")
    get() {
      val text = text
      val index = text.indexOf(' ')
      if (index < 0) {
        return text
      } else {
        return text.substring(0, index)
      }
    }

  override val standalone: Boolean?
    get() = if (mDelegate.standaloneSet()) mDelegate.isStandalone else null

  companion object {

    private val DELEGATE_TO_LOCAL = Array<EventType?>(16) { i ->
      when (i) {
        XMLStreamConstants.CDATA -> EventType.CDSECT
        XMLStreamConstants.COMMENT -> EventType.COMMENT
        XMLStreamConstants.DTD -> EventType.DOCDECL
        XMLStreamConstants.END_DOCUMENT -> EventType.END_DOCUMENT
        XMLStreamConstants.END_ELEMENT -> EventType.END_ELEMENT
        XMLStreamConstants.ENTITY_REFERENCE -> EventType.ENTITY_REF
        XMLStreamConstants.SPACE -> EventType.IGNORABLE_WHITESPACE
        XMLStreamConstants.PROCESSING_INSTRUCTION -> EventType.PROCESSING_INSTRUCTION
        XMLStreamConstants.START_DOCUMENT -> EventType.START_DOCUMENT
        XMLStreamConstants.START_ELEMENT -> EventType.START_ELEMENT
        XMLStreamConstants.CHARACTERS -> EventType.TEXT
        XMLStreamConstants.ATTRIBUTE -> EventType.ATTRIBUTE
        else -> null
      }

    }

    private val LOCAL_TO_DELEGATE = IntArray(12) { i ->
      when (i) {
        EventType.CDSECT.ordinal -> XMLStreamConstants.CDATA
        EventType.COMMENT.ordinal -> XMLStreamConstants.COMMENT
        EventType.DOCDECL.ordinal -> XMLStreamConstants.DTD
        EventType.END_DOCUMENT.ordinal -> XMLStreamConstants.END_DOCUMENT
        EventType.END_ELEMENT.ordinal -> XMLStreamConstants.END_ELEMENT
        EventType.ENTITY_REF.ordinal -> XMLStreamConstants.ENTITY_REFERENCE
        EventType.IGNORABLE_WHITESPACE.ordinal -> XMLStreamConstants.SPACE
        EventType.PROCESSING_INSTRUCTION.ordinal -> XMLStreamConstants.PROCESSING_INSTRUCTION
        EventType.START_DOCUMENT.ordinal -> XMLStreamConstants.START_DOCUMENT
        EventType.START_ELEMENT.ordinal -> XMLStreamConstants.START_ELEMENT
        EventType.TEXT.ordinal -> XMLStreamConstants.CHARACTERS
        EventType.ATTRIBUTE.ordinal -> XMLStreamConstants.ATTRIBUTE
        else -> -1
      }
    }
  }
}
