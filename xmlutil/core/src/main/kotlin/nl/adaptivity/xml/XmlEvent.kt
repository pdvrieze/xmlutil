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

import nl.adaptivity.xml.XmlStreaming.EventType
import nl.adaptivity.xml.XmlStreaming.EventType.*

import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

import java.util.ArrayList


/**
 * Created by pdvrieze on 16/11/15.
 */
abstract class XmlEvent private constructor(val locationInfo: String) {

  class TextEvent(locationInfo: String, override val eventType: EventType, val text: CharSequence) : XmlEvent(
        locationInfo) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) {
      when (eventType) {
        CDSECT                 -> writer.cdsect(text)
        COMMENT                -> writer.comment(text)
        DOCDECL                -> writer.docdecl(text)
        ENTITY_REF             -> writer.entityRef(text)
        IGNORABLE_WHITESPACE   -> writer.ignorableWhitespace(text)
        PROCESSING_INSTRUCTION -> writer.processingInstruction(text)
        TEXT                   -> writer.text(text)
      }
    }
  }

  class EndDocumentEvent(locationInfo: String) : XmlEvent(locationInfo) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) {
      writer.endDocument()
    }

    override val eventType: EventType
      get() = END_DOCUMENT
  }

  private class EndElementEvent(locationInfo: String, namespaceUri: CharSequence, localName: CharSequence, prefix: CharSequence) : NamedEvent(
        locationInfo,
        namespaceUri,
        localName,
        prefix) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) {
      writer.endTag(namespaceUri, localName, prefix)
    }

    override val eventType: EventType
      get() = END_ELEMENT
  }

  class StartDocumentEvent(locationInfo: String, var version: CharSequence, var encoding: CharSequence, var standalone: Boolean?) : XmlEvent(
        locationInfo) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) {
      writer.startDocument(version, encoding, standalone)
    }

    override val eventType: EventType
      get() = START_DOCUMENT
  }

  private abstract class NamedEvent(locationInfo: String, val namespaceUri: CharSequence, val localName: CharSequence, val prefix: CharSequence) : XmlEvent(
        locationInfo) {

    fun isEqualNames(ev: NamedEvent): Boolean {
      return StringUtil.isEqual(namespaceUri, ev.namespaceUri) &&
            StringUtil.isEqual(localName, ev.localName) &&
            StringUtil.isEqual(prefix, ev.prefix)
    }

  }

  private class StartElementEvent(locationInfo: String, namespaceUri: CharSequence, localName: CharSequence, prefix: CharSequence, val attributes: Array<out Attribute>, val namespaceDecls: Array<out Namespace>) : NamedEvent(
        locationInfo,
        namespaceUri,
        localName,
        prefix), NamespaceContext {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) {
      writer.startTag(namespaceUri, localName, prefix)
      for (attr in attributes) {
        writer.attribute(attr.namespaceUri, attr.localName, attr.prefix, attr.value)
      }
      for (ns in namespaceDecls) {
        writer.namespaceAttr(ns.prefix, ns.namespaceURI)
      }
    }

    override val eventType: EventType
      get() = START_ELEMENT

    override fun getPrefix(namespaceURI: String): String? {
      return getPrefix(namespaceURI as CharSequence)
    }

    fun getPrefix(namespaceUri: CharSequence): String? {
      for (ns in namespaceDecls) {
        if (StringUtil.isEqual(ns.namespaceURI, namespaceUri)) {
          return ns.prefix
        }
      }
      return null
    }

    override fun getNamespaceURI(prefix: String): String? {
      return getNamespaceUri(prefix)
    }

    fun getNamespaceUri(prefix: CharSequence): String? {
      for (ns in namespaceDecls) {
        if (StringUtil.isEqual(ns.prefix, prefix)) {
          return ns.namespaceURI
        }
      }
      return null
    }

    val namespaceContext: NamespaceContext
      get() = this

    override fun getPrefixes(namespaceURI: String): Iterator<String> {
      val result = ArrayList<String>(namespaceDecls.size)
      for (ns in namespaceDecls) {
        if (StringUtil.isEqual(ns.namespaceURI, namespaceUri)) {
          result.add(ns.prefix)
        }
      }
      return result.iterator()
    }
  }

  class Attribute(locationInfo: String, val namespaceUri: CharSequence, val localName: CharSequence, val prefix: CharSequence, val value: CharSequence) : XmlEvent(
        locationInfo) {

    override val eventType: EventType
      get() = ATTRIBUTE

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) {
      writer.attribute(namespaceUri, localName, prefix, value)
    }

    fun getNamespaceUri(): Boolean {
      return StringUtil.isEqual(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                namespaceUri) || prefix.length == 0 && StringUtil.isEqual(XMLConstants.XMLNS_ATTRIBUTE,
                                                                                          localName)
    }
  }

  private class NamespaceImpl(namespacePrefix: CharSequence, namespaceUri: CharSequence) : Namespace {

    override val prefix: String
    override val namespaceURI: String

    init {
      prefix = namespacePrefix.toString()
      namespaceURI = namespaceUri.toString()
    }
  }

  abstract val eventType: EventType

  @Throws(XmlException::class)
  abstract fun writeTo(writer: XmlWriter)

  companion object {

    @Throws(XmlException::class)
    fun from(reader: XmlReader): XmlEvent {
      val eventType = reader.eventType

      val locationInfo = reader.locationInfo
      when (eventType) {
        CDSECT, COMMENT, DOCDECL, ENTITY_REF, IGNORABLE_WHITESPACE, PROCESSING_INSTRUCTION, TEXT ->
          return TextEvent(locationInfo, eventType, reader.text)
        END_DOCUMENT   ->
          return EndDocumentEvent(locationInfo)
        END_ELEMENT    ->
          return EndElementEvent(locationInfo, reader.namespaceUri, reader.localName, reader.prefix)
        START_DOCUMENT ->
          return StartDocumentEvent(locationInfo, reader.version, reader.encoding, reader.standalone)
        START_ELEMENT  ->
          return StartElementEvent(locationInfo, reader.namespaceUri, reader.localName,
                                   reader.prefix, getAttributes(reader), getNamespaceDecls(reader))
      }
      throw IllegalStateException("This should not be reachable")
    }

    @Throws(XmlException::class)
    private fun getNamespaceDecls(reader: XmlReader): Array<out Namespace> {
      val readerOffset = reader.namespaceStart
      val namespaces = Array<Namespace>(reader.namespaceEnd -readerOffset) { i ->
        val nsIndex = readerOffset + i
        NamespaceImpl(reader.getNamespacePrefix(nsIndex), reader.getNamespaceUri(nsIndex))
      }
      return namespaces
    }

    @Throws(XmlException::class)
    private fun getAttributes(reader: XmlReader): Array<out Attribute> {
      val result = Array<Attribute>(reader.attributeCount) { i ->
        Attribute(reader.locationInfo,
                  reader.getAttributeNamespace(i)!!,
                  reader.getAttributeLocalName(i),
                  reader.getAttributePrefix(i)!!,
                  reader.getAttributeValue(i))
      }

      return result
    }
  }
}
