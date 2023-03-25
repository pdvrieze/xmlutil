/*
 * Copyright (c) 2022.
 *
 * This file is part of xmlutil.
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

import nl.adaptivity.xmlutil.core.impl.isXmlWhitespace
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.util.*

/**
 * Created by pdvrieze on 22/03/17.
 */
public class DomReader(public val delegate: Node) : XmlReader {
    private var current: Node? = null

    override val namespaceURI: String
        get() = currentElement?.run { namespaceURI ?: "" }
            ?: throw XmlException("Only elements have a namespace uri")

    override val localName: String
        get() = currentElement?.localName
            ?: throw XmlException("Only elements have a local name")

    override val prefix: String
        get() = currentElement?.run { prefix ?: "" }
            ?: throw XmlException("Only elements have a prefix")


    override var isStarted: Boolean = false
        private set

    private var atEndOfElement: Boolean = false

    override var depth: Int = 0
        private set

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    override val text: String
        get() = when (current?.nodeType) {
            NodeConsts.ENTITY_REFERENCE_NODE,
            NodeConsts.COMMENT_NODE,
            NodeConsts.TEXT_NODE,
            NodeConsts.PROCESSING_INSTRUCTION_NODE,
            NodeConsts.CDATA_SECTION_NODE -> (current as CharacterData).data

            else -> throw XmlException("Node is not a text node")
        }

    @Suppress("UNCHECKED_CAST")
    override val attributeCount: Int get() = (current as Element?)?.attributes?.length ?: 0

    override val eventType: EventType
        get() = when (val c = current) {
            null -> EventType.END_DOCUMENT
            else -> c.toEventType(atEndOfElement)
        }

    private var _namespaceAttrs: List<Attr>? = null
    internal val namespaceAttrs: List<Attr>
        get() {

            return _namespaceAttrs ?: (
                    requireCurrentElem.attributes.filterTyped { it.prefix == "xmlns" || (it.prefix.isNullOrEmpty() && it.localName == "xmlns") }
                        .also {
                            _namespaceAttrs = it
                        })

        }

    override val locationInfo: String
        get() {

            fun <A : Appendable> helper(node: Node?, result: A): A = when {
                node == null ||
                        node.nodeType == NodeConsts.DOCUMENT_NODE
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
    private val requireCurrentElem get() = currentElement ?: throw IllegalStateException("No current element")
    internal val currentElement: Element?
        get() = when (current?.nodeType) {
            NodeConsts.ELEMENT_NODE -> @Suppress("UNCHECKED_CAST") current as Element
            else -> null
        }

    override val namespaceContext: IterableNamespaceContext
        get() = object : IterableNamespaceContext {
            @Suppress("UNCHECKED_CAST")
            private val currentElement: Element? = (requireCurrent as? Element) ?: requireCurrent.parentNode as? Element

            override fun getNamespaceURI(prefix: String): String? {
                return currentElement?.myLookupNamespaceURI(prefix)
            }

            override fun getPrefix(namespaceURI: String): String? {
                return currentElement?.myLookupPrefix(namespaceURI)
            }

            override fun freeze(): IterableNamespaceContext = this

            override fun iterator(): Iterator<Namespace> {
                return sequence<Namespace> {
                    var c: Element? = currentElement
                    while (c != null) {
                        c.attributes.forEachAttr { attr ->
                            when {
                                attr.prefix == "xmlns" ->
                                    yield(XmlEvent.NamespaceImpl(attr.localName ?: attr.name, attr.value))

                                attr.prefix.isNullOrEmpty() && attr.localName == "xmlns" ->
                                    yield(XmlEvent.NamespaceImpl("", attr.value))
                            }
                        }
                        c = c.parentElement
                    }
                }.iterator()
            }

            @Deprecated(
                "Don't use as unsafe",
                replaceWith = ReplaceWith("prefixesFor(namespaceURI)", "nl.adaptivity.xmlutil.prefixesFor")
            )
            override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
                // TODO return all possible ones by doing so recursively
                return listOfNotNull(getPrefix(namespaceURI)).iterator()
            }
        }

    override val namespaceDecls: List<Namespace>
        get() {
            return sequence<Namespace> {
                for (attr in attributes) {
                    when {
                        attr.prefix == "xmlns" ->
                            yield(XmlEvent.NamespaceImpl(attr.localName, attr.value))

                        attr.prefix.isEmpty() && attr.localName == "xmlns" ->
                            yield(XmlEvent.NamespaceImpl("", attr.value))
                    }
                }
            }.toList()
        }

    override val encoding: String?
        get() {
            val d = delegate
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION", "UNCHECKED_CAST")
            return when (d.nodeType) {
                NodeConsts.DOCUMENT_NODE -> (d as Document).inputEncoding
                else -> d.ownerDocument!!.inputEncoding
            }
        }

    override val standalone: Boolean?
        get() = null // not defined on DOM.

    override val version: String get() = "1.0"

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
                atEndOfElement -> {
                    if (c.nextSibling != null) {
                        current = c.nextSibling
                        atEndOfElement = false
                        // This falls back all the way to the bottom to return the current even type (starting the sibling)
                    } else { // no more siblings, go back to parent
                        current = c.parentNode
                        return current?.toEventType(true) ?: EventType.END_DOCUMENT
                    }
                }

                c.firstChild != null -> { // If we have a child, the next element is the first child
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
            val c = current!!
            val nodeType = c.nodeType
            if (nodeType != NodeConsts.ELEMENT_NODE && nodeType != NodeConsts.DOCUMENT_NODE) {
                atEndOfElement = true // No child elements for things like text
            }
            return c.toEventType(atEndOfElement)
        }
    }

    override fun getAttributeNamespace(index: Int): String {
        val attr: Attr = requireCurrentElem.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.namespaceURI ?: ""
    }

    override fun getAttributePrefix(index: Int): String {
        val attr: Attr = requireCurrentElem.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.prefix ?: ""
    }

    override fun getAttributeLocalName(index: Int): String {
        val attr: Attr = requireCurrentElem.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.localName ?: attr.name
    }

    override fun getAttributeValue(index: Int): String {
        val attr: Attr = requireCurrentElem.attributes.get(index) ?: throw IndexOutOfBoundsException()
        return attr.value
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        return requireCurrentElem.getAttributeNS(nsUri, localName)
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


private fun Node.toEventType(endOfElement: Boolean): EventType {
    @Suppress("DEPRECATION")
    return when (nodeType) {
        NodeConsts.ATTRIBUTE_NODE -> EventType.ATTRIBUTE
        NodeConsts.CDATA_SECTION_NODE -> EventType.CDSECT
        NodeConsts.COMMENT_NODE -> EventType.COMMENT
        NodeConsts.DOCUMENT_TYPE_NODE -> EventType.DOCDECL
        NodeConsts.ENTITY_REFERENCE_NODE -> EventType.ENTITY_REF
        NodeConsts.DOCUMENT_FRAGMENT_NODE,
        NodeConsts.DOCUMENT_NODE -> if (endOfElement) EventType.END_DOCUMENT else EventType.START_DOCUMENT
//    Node.DOCUMENT_NODE -> EventType.END_DOCUMENT
        NodeConsts.PROCESSING_INSTRUCTION_NODE -> EventType.PROCESSING_INSTRUCTION
        NodeConsts.TEXT_NODE -> when {
            textContent!!.isXmlWhitespace() -> EventType.IGNORABLE_WHITESPACE
            else -> EventType.TEXT
        }

        NodeConsts.ELEMENT_NODE -> if (endOfElement) EventType.END_ELEMENT else EventType.START_ELEMENT
//    Node.ELEMENT_NODE -> EventType.END_ELEMENT
        else -> throw XmlException("Unsupported event type ($this)")
    }
}
