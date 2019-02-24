/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil

import nl.adaptivity.js.util.asElement
import org.w3c.dom.CharacterData
import org.w3c.dom.Node
import kotlin.dom.isElement
import kotlin.dom.isText

actual typealias PlatformXmlReader = JSDomReader

/**
 * Created by pdvrieze on 22/03/17.
 */
class JSDomReader(val delegate: Node) : XmlReader {
  private var current: Node? = null

  override val namespaceURI: String get() = current?.asElement()?.namespaceURI ?: throw XmlException(
      "Only elements have a namespace uri")
  override val localName: String get() = current?.asElement()?.localName ?: throw XmlException(
      "Only elements have a local name")
  override val prefix: String get() = current?.asElement()?.prefix ?: throw XmlException(
      "Only elements have a namespace uri")
  override var isStarted: Boolean = false
    private set

  private var isClosing: Boolean = false

  override var depth: Int = 0
    private set

  override val text: String
      get() = when (current?.nodeType) {
      Node.ENTITY_REFERENCE_NODE,
      Node.COMMENT_NODE,
      Node.TEXT_NODE,
      Node.PROCESSING_INSTRUCTION_NODE,
      Node.CDATA_SECTION_NODE -> (current as CharacterData).data
    else -> throw XmlException("Node is not a text node")
  }

  override val attributeCount get() = current?.asElement()?.attributes?.length ?: 0

  override val eventType get() = current?.nodeType?.toEventType() ?: EventType.START_DOCUMENT

  override val namespaceStart get() = TODO("Namespaces will need to be implemented independently")

  override val namespaceEnd: Int get() = TODO("Not correctly implemented yet")

  override val locationInfo: String?
    get() {
      var c: Node? = current
      var r:String = when{
        c!!.isElement -> c.nodeName
        c.isText -> "text()"
        else -> "."
      }
      c = c.parentNode
      while (c!=null && c.isElement) {
        r = "${c.parentNode}/$r"
      }
      return r
    }
  override val namespaceContext: NamespaceContext
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val encoding: String? get() = delegate.ownerDocument!!.inputEncoding

  override val standalone: Boolean?
    get() = TODO("Not implemented")
  override val version: String? get() = "1.0"

  override fun hasNext(): Boolean
  {
    return !isClosing || current != delegate
  }

  override fun next(): EventType
  {
    val c = current
    if (c == null) {
      isStarted = true
      current = delegate
      return EventType.START_DOCUMENT
    } else {
      when {
        isClosing -> {
          if (c.parentNode!!.nextSibling == null) {
            current = c.parentNode
          } else {
            isClosing = false
            current = c.parentNode!!.nextSibling
          }
        }
        c.firstChild != null -> {
          current = c.firstChild
        }
        c.nextSibling != null -> {
          current = c.nextSibling
        }
        else -> {
          isClosing = true
        }
      }
      return when {
        isClosing && current?.nodeType == Node.ELEMENT_NODE -> EventType.END_ELEMENT
        isClosing -> EventType.END_DOCUMENT
        else -> current!!.nodeType.toEventType()
      }
    }
  }

  override fun getAttributeNamespace(index: Int): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getAttributePrefix(index: Int): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getAttributeLocalName(index: Int): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getAttributeValue(index: Int): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getAttributeValue(nsUri: String?, localName: String): String? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getNamespacePrefix(index: Int): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun close()
  {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getNamespaceURI(index: Int): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getNamespacePrefix(namespaceUri: String): String? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getNamespaceURI(prefix: String): String? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

private fun Short.toEventType(): EventType {
  return when(this) {
    Node.ATTRIBUTE_NODE -> EventType.ATTRIBUTE
    Node.CDATA_SECTION_NODE -> EventType.CDSECT
    Node.COMMENT_NODE -> EventType.COMMENT
    Node.DOCUMENT_TYPE_NODE -> EventType.DOCDECL
    Node.ENTITY_REFERENCE_NODE -> EventType.ENTITY_REF
    Node.DOCUMENT_NODE -> EventType.START_DOCUMENT
//    Node.DOCUMENT_NODE -> EventType.END_DOCUMENT
    Node.PROCESSING_INSTRUCTION_NODE -> EventType.PROCESSING_INSTRUCTION
    Node.TEXT_NODE -> EventType.TEXT
    Node.ELEMENT_NODE -> EventType.START_ELEMENT
//    Node.ELEMENT_NODE -> EventType.END_ELEMENT
    else -> throw XmlException("Unsupported event type")
  }
}