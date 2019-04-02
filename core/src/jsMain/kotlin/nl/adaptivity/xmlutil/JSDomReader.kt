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
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.get
import kotlin.dom.isElement
import kotlin.dom.isText

actual typealias PlatformXmlReader = JSDomReader

/**
 * Created by pdvrieze on 22/03/17.
 */
class JSDomReader(val delegate: Node) : XmlReader {
    private var current: Node? = null

    override val namespaceURI: String
        get() = current?.asElement()?.namespaceURI ?: throw XmlException(
            "Only elements have a namespace uri"
                                                                        )
    override val localName: String
        get() = current?.asElement()?.localName ?: throw XmlException(
            "Only elements have a local name"
                                                                     )
    override val prefix: String
        get() = current?.asElement()?.prefix ?: throw XmlException(
            "Only elements have a namespace uri"
                                                                  )
    override var isStarted: Boolean = false
        private set

    private var currentAttribute: Int = -1
    private var atEndOfElement: Boolean = false

    override var depth: Int = 0
        private set

    override val text: String
        get() = when (current?.nodeType) {
            Node.ENTITY_REFERENCE_NODE,
            Node.COMMENT_NODE,
            Node.TEXT_NODE,
            Node.PROCESSING_INSTRUCTION_NODE,
            Node.CDATA_SECTION_NODE -> (current as CharacterData).data
            else                    -> throw XmlException("Node is not a text node")
        }

    override val attributeCount get() = current?.asElement()?.attributes?.length ?: 0

    override val eventType get() = when {
        atEndOfElement==false                  -> current?.nodeType?.toEventType() ?: EventType.START_DOCUMENT
        current?.nodeType == Node.ELEMENT_NODE -> EventType.END_ELEMENT
        else                                   -> EventType.END_DOCUMENT
    }

    override val namespaceStart get() = TODO("Namespaces will need to be implemented independently")

    override val namespaceEnd: Int get() = TODO("Not correctly implemented yet")

    override val locationInfo: String?
        get() {
            var c: Node? = current
            var r: String = when {
                c!!.isElement -> c.nodeName
                c.isText      -> "text()"
                else          -> "."
            }
            c = c.parentNode
            while (c != null && c.isElement) {
                r = "${c.parentNode}/$r"
            }
            return r
        }

    private val requireCurrent get() = current ?: throw IllegalStateException("No current element")
    private val currentElement get() = current as? Element ?: throw IllegalStateException("Node is not an element")

    override val namespaceContext: NamespaceContext = object : NamespaceContext {
        override fun getNamespaceURI(prefix: String): String? {
            return delegate.lookupNamespaceURI(prefix)
        }

        override fun getPrefix(namespaceURI: String): String? {
            return delegate.lookupPrefix(namespaceURI)
        }

        override fun getPrefixes(namespaceURI: String): Iterator<String> {
            // TODO return all possible ones by doing so recursively
            return listOfNotNull(getPrefix(namespaceURI)).iterator()
        }

    }

    override val encoding: String? get() = delegate.ownerDocument!!.inputEncoding

    override val standalone: Boolean?
        get() = TODO("Not implemented")

    override val version: String? get() = "1.0"

    override fun hasNext(): Boolean {
        return !atEndOfElement || current != delegate
    }

    override fun next(): EventType {
        val c = current
        if (c == null) {
            isStarted = true
            current = delegate
            return EventType.START_DOCUMENT
        } else { // set current to the new element
            when {
                atEndOfElement        -> {
                    if (c.nextSibling != null) {
                        current = c.nextSibling
                        atEndOfElement = false
                        // This falls back all the way to the bottom to return the current even type (starting the sibling)
                    } else { // no more siblings, go back to parent
                        current = c.parentNode
                        if (current?.nodeType == Node.ELEMENT_NODE) return EventType.END_ELEMENT
                        return EventType.END_DOCUMENT
                    }
                }
                c.firstChild != null  -> { // If we have a child, the next element is the first child
                    current = c.firstChild
                }
                else -> {
                    // We have no children, but we have a sibling. We are at the end of this element, next we will return
                    // the sibling, or close the parent if there is no sibling
                    atEndOfElement = true
                    return EventType.END_ELEMENT
                }
/*
                else                  -> {
                    atEndOfElement = true // We are the last item in the parent, so the parent needs to be end of an element as well
                    return EventType.END_ELEMENT
                }
*/
            }
            return current!!.nodeType.toEventType()
        }
    }

    override fun getAttributeNamespace(index: Int): String {
        val attr = currentElement.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.namespaceURI ?: ""
    }

    override fun getAttributePrefix(index: Int): String {
        val attr = currentElement.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.prefix ?: ""
    }

    override fun getAttributeLocalName(index: Int): String {
        val attr = currentElement.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.localName
    }

    override fun getAttributeValue(index: Int): String {
        val attr = currentElement.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.value
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        return currentElement.getAttributeNS(nsUri, localName)
    }

    override fun getNamespacePrefix(index: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        current = null
    }

    override fun getNamespaceURI(index: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNamespacePrefix(namespaceUri: String): String? {
        return requireCurrent.lookupPrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return requireCurrent.lookupNamespaceURI(prefix)
    }
}

private fun Short.toEventType(): EventType {
    return when (this) {
        Node.ATTRIBUTE_NODE              -> EventType.ATTRIBUTE
        Node.CDATA_SECTION_NODE          -> EventType.CDSECT
        Node.COMMENT_NODE                -> EventType.COMMENT
        Node.DOCUMENT_TYPE_NODE          -> EventType.DOCDECL
        Node.ENTITY_REFERENCE_NODE       -> EventType.ENTITY_REF
        Node.DOCUMENT_NODE               -> EventType.START_DOCUMENT
//    Node.DOCUMENT_NODE -> EventType.END_DOCUMENT
        Node.PROCESSING_INSTRUCTION_NODE -> EventType.PROCESSING_INSTRUCTION
        Node.TEXT_NODE                   -> EventType.TEXT
        Node.ELEMENT_NODE                -> EventType.START_ELEMENT
//    Node.ELEMENT_NODE -> EventType.END_ELEMENT
        else                             -> throw XmlException("Unsupported event type")
    }
}