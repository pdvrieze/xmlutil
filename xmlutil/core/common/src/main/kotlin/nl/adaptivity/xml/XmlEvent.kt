/*
 * Copyright (c) 2018.
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

/**
 * A class to represent the events that can occur in XML Documents
 *
 * Created by pdvrieze on 16/11/15.
 */
sealed  class XmlEvent private constructor(val locationInfo: String?) {

  companion object {
    internal val IGNORABLE = object: AbstractSet<EventType>() {
      private val elements = BooleanArray(EventType.values().size)

      init {
        elements[EventType.COMMENT.ordinal]=true
        elements[EventType.START_DOCUMENT.ordinal]=true
        elements[EventType.END_DOCUMENT.ordinal]=true
        elements[EventType.PROCESSING_INSTRUCTION.ordinal]=true
        elements[EventType.DOCDECL.ordinal]=true
        elements[EventType.IGNORABLE_WHITESPACE.ordinal]=true
      }

      override fun contains(element: EventType) = elements[element.ordinal]

      override val size get() = 6

      override fun iterator(): Iterator<EventType>
        = elements.mapIndexed { index, b -> if (b) EventType.values()[index] else null }.filterNotNull().iterator()
    }


    fun from(reader: XmlReader) = reader.eventType.createEvent(reader)

    @Deprecated("Use the extension property", ReplaceWith("reader.namespaceDecls", "nl.adaptivity.xml.attributes"))
    internal fun getNamespaceDecls(reader: XmlReader): Array<out Namespace> {
      val readerOffset = reader.namespaceStart
      val namespaces = Array<Namespace>(reader.namespaceEnd -readerOffset) { i ->
        val nsIndex = readerOffset + i
          NamespaceImpl(reader.getNamespacePrefix(nsIndex), reader.getNamespaceUri(nsIndex))
      }
      return namespaces
    }

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

    override fun writeTo(writer: XmlWriter) = eventType.writeEvent(writer, this)

    override val isIgnorable: Boolean
      get() =
        super.isIgnorable || (eventType == EventType.TEXT && isXmlWhitespace(text))

  }

  class EndDocumentEvent(locationInfo: String?) : XmlEvent(locationInfo) {

    override fun writeTo(writer: XmlWriter) = writer.endDocument()

    override val eventType: EventType get() = EventType.END_DOCUMENT
  }

  class EndElementEvent(locationInfo: String?, namespaceUri: CharSequence, localName: CharSequence, prefix: CharSequence) :
      NamedEvent(locationInfo, namespaceUri, localName, prefix) {

    override fun writeTo(writer: XmlWriter) = writer.endTag(namespaceUri, localName, prefix)

    override val eventType: EventType get() = EventType.END_ELEMENT
  }

  class StartDocumentEvent(locationInfo: String?,
                           val version: CharSequence?,
                           val encoding: CharSequence?,
                           val standalone: Boolean?) :
      XmlEvent(locationInfo) {

    override fun writeTo(writer: XmlWriter) = writer.startDocument(version, encoding, standalone)

    override val eventType: EventType get() = EventType.START_DOCUMENT
  }

  abstract class NamedEvent(locationInfo: String?,
                                    val namespaceUri: CharSequence,
                                    val localName: CharSequence,
                                    val prefix: CharSequence) :
      XmlEvent(locationInfo) {

    fun isEqualNames(ev: NamedEvent): Boolean {
        return namespaceUri.toString()==ev.namespaceUri.toString() &&
               localName.toString()==ev.localName.toString() &&
               prefix.toString()==ev.prefix.toString()
    }

  }

  class StartElementEvent(locationInfo: String?,
                          namespaceUri: CharSequence,
                          localName: CharSequence,
                          prefix: CharSequence,
                          val attributes: Array<out Attribute>,
                          val namespaceDecls: Array<out Namespace>) :
      NamedEvent(locationInfo, namespaceUri, localName, prefix), NamespaceContext {

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
            .filter { ns -> ns.namespaceURI.toString() == namespaceUri.toString() }
            .lastOrNull()?.prefix
    }

    override fun getNamespaceURI(prefix: String) = getNamespaceUri(prefix)

    fun getNamespaceUri(prefix: CharSequence): String? {
      return namespaceDecls
            .asSequence()
            .filter { ns -> ns.prefix ==prefix.toString() }
            .lastOrNull()?.namespaceURI
    }

    val namespaceContext: NamespaceContext get() = this

    override fun getPrefixes(namespaceURI: String): Iterator<String> {
      return namespaceDecls
            .asSequence()
            .filter { ns -> ns.namespaceURI == namespaceUri.toString() }
            .map { it.prefix }.iterator()
    }
  }

  class Attribute(locationInfo: String?, val namespaceUri: CharSequence, val localName: CharSequence, val prefix: CharSequence, val value: CharSequence) : XmlEvent(
        locationInfo) {

    override val eventType: EventType get() = EventType.ATTRIBUTE

    override fun writeTo(writer: XmlWriter) = writer.attribute(namespaceUri, localName, prefix, value)

    fun hasNamespaceUri(): Boolean {
      return XMLConstants.XMLNS_ATTRIBUTE_NS_URI == namespaceUri.toString() ||
             (prefix.isEmpty() && XMLConstants.XMLNS_ATTRIBUTE == localName.toString())
    }
  }

  class NamespaceImpl(private val namespacePrefix: CharSequence, private val namespaceUri: CharSequence) : Namespace {

      override val prefix: String
          get() = namespacePrefix.toString()
      override val namespaceURI get() = namespaceUri.toString()

  }

  abstract val eventType: EventType

  open val isIgnorable:Boolean get() = eventType.isIgnorable

  abstract fun writeTo(writer: XmlWriter)

}
