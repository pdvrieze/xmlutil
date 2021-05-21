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
import nl.adaptivity.js.util.filter
import nl.adaptivity.js.util.myLookupNamespaceURI
import nl.adaptivity.js.util.myLookupPrefix
import org.w3c.dom.*
import kotlinx.dom.isElement
import kotlinx.dom.isText
import nl.adaptivity.xmlutil.util.CombiningNamespaceContext

actual typealias PlatformXmlReader = JSDomReader

/**
 * Created by pdvrieze on 22/03/17.
 */
class JSDomReader(val delegate: Node) : XmlReader {
    private var current: Node? = null

    override val namespaceURI: String
        get() = current?.asElement()?.run { namespaceURI ?: "" }
            ?: throw XmlException("Only elements have a namespace uri")

    override val localName: String
        get() = current?.asElement()?.localName
            ?: throw XmlException("Only elements have a local name")

    override val prefix: String
        get() = current?.asElement()?.run { prefix ?: "" }
            ?: throw XmlException("Only elements have a prefix")


    override var isStarted: Boolean = false
        private set

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

    override val eventType
        get() = when (val c = current) {
            null -> EventType.END_DOCUMENT
            else -> c.nodeType.toEventType(atEndOfElement)
        }

    override val namespaceStart get() = 0

    override val namespaceEnd: Int
        get() {
            return namespaceAttrs.size
        }

    private var _namespaceAttrs: List<Attr>? = null
    internal val namespaceAttrs: List<Attr>
        get() {

            return _namespaceAttrs ?: (
                    currentElement.attributes.filter { it.prefix == "xmlns" || (it.prefix.isNullOrEmpty() && it.localName == "xmlns") }.also {
                        _namespaceAttrs = it
                    })

        }

    override val locationInfo: String?
        get() {

            fun <A : Appendable> helper(node: Node?, result: A): A = when {
                node == null ||
                        node.nodeType == Node.DOCUMENT_NODE
                     -> result

                node.isElement
                     -> helper(node.parentNode, result).apply { append('/').append(node.nodeName) }

                node.isText
                     -> helper(node.parentNode, result).apply { append("/text()") }

                else -> helper(node.parentNode, result).apply { append("/.") }
            }

            return helper(current, StringBuilder()).toString()
        }

    private val requireCurrent get() = current ?: throw IllegalStateException("No current element")
    internal val currentElement get() = current as? Element ?: throw IllegalStateException("Node is not an element")

    override val namespaceContext: FreezableNamespaceContext
        get() = object : FreezableNamespaceContext {
            private val currentElement: Element? = (requireCurrent as? Element) ?: requireCurrent.parentElement

            override fun getNamespaceURI(prefix: String): String? {
                return currentElement?.lookupNamespaceURI(prefix)
            }

            override fun getPrefix(namespaceURI: String): String? {
                return currentElement?.lookupPrefix(namespaceURI)
            }

            override fun freeze(): FreezableNamespaceContext = this

            @Suppress("OverridingDeprecatedMember")
            override fun getPrefixes(namespaceURI: String): Iterator<String> {
                // TODO return all possible ones by doing so recursively
                return listOfNotNull(getPrefix(namespaceURI)).iterator()
            }
        }

    override val encoding: String?
        get() = when (val d = delegate) {
            is Document -> d.inputEncoding
            else        -> d.ownerDocument!!.inputEncoding
        }

    override val standalone: Boolean?
        get() = null // not defined on DOM.

    override val version: String? get() = "1.0"

    override fun hasNext(): Boolean {
        return !atEndOfElement || current != delegate
    }

    override fun next(): EventType {
        _namespaceAttrs = null // reset lazy value
        val c = current
        if (c == null) {
            isStarted = true
            current = delegate
            return EventType.START_DOCUMENT
        } else { // set current to the new element
            when {
                atEndOfElement       -> {
                    if (c.nextSibling != null) {
                        current = c.nextSibling
                        atEndOfElement = false
                        // This falls back all the way to the bottom to return the current even type (starting the sibling)
                    } else { // no more siblings, go back to parent
                        current = c.parentNode
                        return current?.nodeType?.toEventType(true) ?: EventType.END_DOCUMENT
                    }
                }
                c.firstChild != null -> { // If we have a child, the next element is the first child
                    current = c.firstChild
                }
                else                 -> {
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
            val nodeType = current!!.nodeType
            if (nodeType != Node.ELEMENT_NODE && nodeType != Node.DOCUMENT_NODE) {
                atEndOfElement = true // No child elements for things like text
            }
            return nodeType.toEventType(atEndOfElement)
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
        return when (val localName = namespaceAttrs[index].localName ?: "") {
            "xmlns" -> ""
            else    -> localName
        }
    }

    override fun getNamespaceURI(index: Int): String {
        @Suppress("USELESS_ELVIS")
        return namespaceAttrs[index].value ?: ""
    }

    override fun close() {
        current = null
    }

    override fun getNamespacePrefix(namespaceUri: String): String? {
        return requireCurrent.myLookupPrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return requireCurrent.myLookupNamespaceURI(prefix)
    }
}


private fun Short.toEventType(endOfElement: Boolean): EventType {
    return when (this) {
        Node.ATTRIBUTE_NODE              -> EventType.ATTRIBUTE
        Node.CDATA_SECTION_NODE          -> EventType.CDSECT
        Node.COMMENT_NODE                -> EventType.COMMENT
        Node.DOCUMENT_TYPE_NODE          -> EventType.DOCDECL
        Node.ENTITY_REFERENCE_NODE       -> EventType.ENTITY_REF
        Node.DOCUMENT_NODE               -> if (endOfElement) EventType.START_DOCUMENT else EventType.END_DOCUMENT
//    Node.DOCUMENT_NODE -> EventType.END_DOCUMENT
        Node.PROCESSING_INSTRUCTION_NODE -> EventType.PROCESSING_INSTRUCTION
        Node.TEXT_NODE                   -> EventType.TEXT
        Node.ELEMENT_NODE                -> if (endOfElement) EventType.END_ELEMENT else EventType.START_ELEMENT
//    Node.ELEMENT_NODE -> EventType.END_ELEMENT
        else                             -> throw XmlException("Unsupported event type")
    }
}
