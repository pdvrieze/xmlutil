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

import net.devrieze.util.kotlin.matches
import nl.adaptivity.xml.XmlStreaming.EventType
import java.util.*
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

/**
 * A class to represent the events that can occur in XML Documents
 *
 * Created by pdvrieze on 16/11/15.
 */
sealed  class XmlEvent private constructor(val locationInfo: String?) {

  companion object {
    @JvmStatic
    internal val IGNORABLE = EnumSet.of(EventType.COMMENT, EventType.START_DOCUMENT, EventType.END_DOCUMENT,
        EventType.PROCESSING_INSTRUCTION, EventType.DOCDECL, EventType.IGNORABLE_WHITESPACE)


    @Throws(XmlException::class)
    @JvmStatic
    fun from(reader: XmlReader) = reader.eventType.createEvent(reader)

    @JvmStatic
    @JvmName("getNamespaceDecls")
    @Deprecated("Use the extension property", ReplaceWith("reader.namespaceDecls", "nl.adaptivity.xml.attributes"))
    internal fun getNamespaceDecls(reader: XmlReader): Array<out Namespace> {
      val readerOffset = reader.namespaceStart
      val namespaces = Array<Namespace>(reader.namespaceEnd -readerOffset) { i ->
        val nsIndex = readerOffset + i
        NamespaceImpl(reader.getNamespacePrefix(nsIndex), reader.getNamespaceUri(nsIndex))
      }
      return namespaces
    }

    @JvmStatic
    @JvmName("getAttributes")
    @Deprecated("Use the extension property", ReplaceWith("reader.attributes", "nl.adaptivity.xml.attributes"))
    internal fun getAttributes(reader: XmlReader): Array<out Attribute> {
      val result = Array(reader.attributeCount) { i ->
        Attribute(reader.locationInfo,
                  reader.getAttributeNamespace(i),
                  reader.getAttributeLocalName(i),
                  reader.getAttributePrefix(i),
                  reader.getAttributeValue(i))
      }

      return result
    }

  }

  class TextEvent(locationInfo: String?, override val eventType: EventType, val text: CharSequence) : XmlEvent(
        locationInfo) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) = eventType.writeEvent(writer, this)

    override val isIgnorable: Boolean
      get() =
        super.isIgnorable || (eventType == EventType.TEXT && isXmlWhitespace(text))

  }

  class EndDocumentEvent(locationInfo: String?) : XmlEvent(locationInfo) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) = writer.endDocument()

    override val eventType: EventType get() = EventType.END_DOCUMENT
  }

  class EndElementEvent(locationInfo: String?, namespaceUri: CharSequence, localName: CharSequence, prefix: CharSequence) :
        NamedEvent(locationInfo, namespaceUri, localName, prefix) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) = writer.endTag(namespaceUri, localName, prefix)

    override val eventType: EventType get() = EventType.END_ELEMENT
  }

  class StartDocumentEvent(locationInfo: String?,
                           val version: CharSequence,
                           val encoding: CharSequence,
                           val standalone: Boolean?) :
        XmlEvent(locationInfo) {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) = writer.startDocument(version, encoding, standalone)

    override val eventType: EventType get() = EventType.START_DOCUMENT
  }

  abstract class NamedEvent(locationInfo: String?,
                                    val namespaceUri: CharSequence,
                                    val localName: CharSequence,
                                    val prefix: CharSequence) :
        XmlEvent(locationInfo) {

    fun isEqualNames(ev: NamedEvent): Boolean {
      return namespaceUri matches ev.namespaceUri &&
             localName matches ev.localName &&
             prefix matches ev.prefix
    }

  }

  class StartElementEvent(locationInfo: String?,
                                  namespaceUri: CharSequence,
                                  localName: CharSequence,
                                  prefix: CharSequence,
                                  val attributes: Array<out Attribute>,
                                  val namespaceDecls: Array<out Namespace>) :
        NamedEvent(locationInfo, namespaceUri, localName, prefix), NamespaceContext {

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) {
      writer.startTag(namespaceUri, localName, prefix)

      attributes.forEach { attr -> writer.attribute(attr.namespaceUri, attr.localName, attr.prefix, attr.value) }

      namespaceDecls.forEach { ns -> writer.namespaceAttr(ns.prefix, ns.namespaceURI) }
    }

    override val eventType: EventType get() = EventType.START_ELEMENT

    override fun getPrefix(namespaceURI: String) = getPrefix(namespaceURI as CharSequence)

    fun getPrefix(namespaceUri: CharSequence): String? {
      return namespaceDecls
            .asSequence()
            .filter { ns -> ns.namespaceURI matches namespaceUri }
            .lastOrNull()?.prefix
    }

    override fun getNamespaceURI(prefix: String) = getNamespaceUri(prefix)

    fun getNamespaceUri(prefix: CharSequence): String? {
      return namespaceDecls
            .asSequence()
            .filter { ns -> ns.prefix matches prefix }
            .lastOrNull()?.namespaceURI
    }

    val namespaceContext: NamespaceContext get() = this

    override fun getPrefixes(namespaceURI: String): Iterator<String> {
      return namespaceDecls
            .asSequence()
            .filter { ns -> ns.namespaceURI matches namespaceUri }
            .map { it.prefix }.iterator()
    }
  }

  class Attribute(locationInfo: String?, val namespaceUri: CharSequence, val localName: CharSequence, val prefix: CharSequence, val value: CharSequence) : XmlEvent(
        locationInfo) {

    override val eventType: EventType get() = EventType.ATTRIBUTE

    @Throws(XmlException::class)
    override fun writeTo(writer: XmlWriter) = writer.attribute(namespaceUri, localName, prefix, value)

    fun hasNamespaceUri(): Boolean {
      return XMLConstants.XMLNS_ATTRIBUTE_NS_URI matches namespaceUri ||
            (prefix.length == 0 && XMLConstants.XMLNS_ATTRIBUTE matches localName)
    }
  }

  class NamespaceImpl(namespacePrefix: CharSequence, namespaceUri: CharSequence) : Namespace {

    override val prefix = namespacePrefix.toString()
    override val namespaceURI = namespaceUri.toString()

  }

  abstract val eventType: EventType

  open val isIgnorable:Boolean = eventType in IGNORABLE

  @Throws(XmlException::class)
  abstract fun writeTo(writer: XmlWriter)

}
