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

@file:JvmName("XmlUtil")
@file:JvmMultifileClass

package nl.adaptivity.xml

import nl.adaptivity.util.xml.ExtXmlDeserializable
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.EventType
import java.io.StringReader
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 04/11/15.
 */
interface XmlDeserializable {

  /**
   * Handle the given attribue.
   * @param attributeNamespace The namespace of the the attribute.
   * *
   * @param attributeLocalName The local name of the attribute
   * *
   * @param attributeValue The value of the attribute
   * *
   * @return `true` if handled, `false` if not. (The caller may use this for errors)
   */
  fun deserializeAttribute(attributeNamespace: CharSequence,
                           attributeLocalName: CharSequence,
                           attributeValue: CharSequence): Boolean = false

  /** Listener called just before the children are deserialized. After attributes have been processed.  */
  @Throws(XmlException::class)
  fun onBeforeDeserializeChildren(reader: XmlReader) {}

  /** The name of the element, needed for the automated validation */
  val elementName: QName
}


/**
 * Utility method to deserialize a list of xml containing strings
 * @param input The strings to deserialize
 * *
 * @param type The type that contains the factory to deserialize
 * *
 * @param  The type
 * *
 * @return A list of deserialized objects.
 * *
 * @throws XmlException If deserialization fails anywhere.
 */
@Throws(XmlException::class)
fun <T> Iterable<String>.deSerialize(type: Class<T>): List<T> {
  val deserializer = type.getAnnotation(XmlDeserializer::class.java) ?: throw IllegalArgumentException("Types must be annotated with " + XmlDeserializer::class.java.name + " to be deserialized automatically")
  val factory: XmlDeserializerFactory<*> = deserializer.value.java.newInstance() as XmlDeserializerFactory<*>

  return this.map { type.cast(factory.deserialize(XmlStreaming.newReader(StringReader(it)))) }
}

@Throws(XmlException::class)
fun <T : XmlDeserializable> T.deserializeHelper(reader: XmlReader): T {
  reader.skipPreamble()

  val elementName = elementName
  assert(reader.isElement(elementName)) { "Expected " + elementName + " but found " + reader.localName }

  for (i in reader.attributeIndices.reversed()) {
    deserializeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributeValue(i))
  }

  onBeforeDeserializeChildren(reader)

  if (this is SimpleXmlDeserializable) {
    loop@ while (reader.hasNext() && reader.next() !== EventType.END_ELEMENT) {
      when (reader.eventType) {
        EventType.START_ELEMENT                            -> if (! deserializeChild(reader)) reader.unhandledEvent()
        EventType.TEXT, EventType.CDSECT -> if (! deserializeChildText(reader.text)) reader.unhandledEvent()
      // If the text was not deserialized, then just fall through
        else                                                                 -> reader.unhandledEvent()
      }
    }
  } else if (this is ExtXmlDeserializable) {
    deserializeChildren(reader)

    if (XmlDeserializable::class.java.desiredAssertionStatus()) reader.require(EventType.END_ELEMENT, elementName.namespaceURI, elementName.localPart)

  } else {// Neither, means ignore children
    if (!isXmlWhitespace(reader.siblingsToFragment().content)) {
      throw XmlException("Unexpected child content in element")
    }
  }
  return this
}
