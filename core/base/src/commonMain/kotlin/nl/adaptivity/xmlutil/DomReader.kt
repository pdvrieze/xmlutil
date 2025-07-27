/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.dom.adoptNode
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.util.filterTyped
import nl.adaptivity.xmlutil.util.forEachAttr
import nl.adaptivity.xmlutil.util.impl.createDocument
import nl.adaptivity.xmlutil.util.myLookupNamespaceURI
import nl.adaptivity.xmlutil.util.myLookupPrefix
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Node as Node2

/**
 * [XmlReader] that reads from DOM.
 *
 * @author Created by pdvrieze on 22/03/17.
 */
@Deprecated("Don't use directly. Instead create an instance through xmlStreaming", ReplaceWith("xmlStreaming.newReader(delegate)", "nl.adaptivity.xmlutil.xmlStreaming"))
@XmlUtilDeprecatedInternal
public class DomReader(public val delegate: Node2, public val expandEntities: Boolean) : XmlReader {

    public constructor(delegate: Node2) : this(delegate, false)

    @Suppress("DEPRECATION")
    public constructor(delegate: Node1) :
            this((delegate as? Node2) ?: createDocument(QName("XX")).adoptNode(delegate))

    private var current: Node2? = null

    override val namespaceURI: String
        get() = currentElement?.run { getNamespaceURI() ?: "" }
            ?: throw XmlException("Only elements have a namespace uri")

    override val localName: String
        // allow localName to be null for non-namespace aware nodes
        get() {
            val current = current
            return when (current?.nodeType) {
                NodeConsts.ELEMENT_NODE -> (current as Element).getLocalName()
                NodeConsts.ENTITY_REFERENCE_NODE if (!expandEntities) -> current.nodeName
                else -> throw XmlException("Only elements have a local name")
            }
        }

    override val prefix: String
        get() = currentElement?.let { it.getPrefix() ?: "" }
            ?: throw XmlException("Only elements have a prefix")


    override var isStarted: Boolean = false
        private set

    private var atEndOfElement: Boolean = false

    override var depth: Int = 0
        private set

    override val piTarget: String
        get() {
            val c = requireCurrent
            require(c.nodeType == NodeConsts.PROCESSING_INSTRUCTION_NODE)
            return (c as ProcessingInstruction).getTarget()
        }

    override val piData: String
        get() {
            val c = requireCurrent
            require(c.nodeType == NodeConsts.PROCESSING_INSTRUCTION_NODE)
            return (c as ProcessingInstruction).getData()
        }

    @Suppress("DEPRECATION")
    override val text: String
        get() = when (current?.nodeType) {
            NodeConsts.ENTITY_REFERENCE_NODE,
            NodeConsts.COMMENT_NODE,
            NodeConsts.TEXT_NODE,
            NodeConsts.CDATA_SECTION_NODE -> (current as CharacterData).data

            NodeConsts.PROCESSING_INSTRUCTION_NODE -> (current as CharacterData).let { "${it.nodeName} ${it.getData()}" }

            else -> throw XmlException("Node is not a text node")
        }

    override val attributeCount: Int get() = (current as Element?)?.getAttributes()?.getLength() ?: 0

    override val eventType: EventType
        get() = when (val c = current) {
            null -> EventType.END_DOCUMENT
            else -> c.toEventType(atEndOfElement, expandEntities)
        }

    private var _namespaceAttrs: List<Attr>? = null
    private val namespaceAttrs: List<Attr>
        get() {
            return _namespaceAttrs ?: (
                    requireCurrentElem.getAttributes().filterTyped {
                        (it.getNamespaceURI() == null || it.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI) &&
                                (it.getPrefix() == "xmlns" || (it.getPrefix()
                                    .isNullOrEmpty() && it.getLocalName() == "xmlns")) &&
                                it.getValue() != XMLConstants.XMLNS_ATTRIBUTE_NS_URI
                    }.also {
                        _namespaceAttrs = it
                    })

        }

    override val extLocationInfo: XmlReader.LocationInfo
        get() {
            fun <A : Appendable> helper(node: Node2?, result: A): A = when (node?.nodetype) {
                null, NodeType.DOCUMENT_NODE
                    -> result

                NodeType.ELEMENT_NODE
                    -> helper(node.parentNode, result).apply { append('/').append(node.nodeName) }

                NodeType.TEXT_NODE
                    -> helper(node.parentNode, result).apply { append("/text()") }

                else -> helper(node.parentNode, result).apply { append("/.") }
            }

            return XmlReader.StringLocationInfo(helper(current, StringBuilder()).toString())
        }

    @Deprecated(
        "Use extLocationInfo as that allows more detailed information",
        replaceWith = ReplaceWith("extLocationInfo?.toString()")
    )
    override val locationInfo: String
        get() = extLocationInfo.toString()

    private val requireCurrent get() = current ?: throw IllegalStateException("No current element")
    private val requireCurrentElem get() = currentElement ?: throw IllegalStateException("No current element")

    private val currentElement: Element?
        get() = when (current?.nodeType) {
            NodeConsts.ELEMENT_NODE -> current as Element
            else -> null
        }

    override val namespaceContext: IterableNamespaceContext
        get() = object : IterableNamespaceContext {
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
                        c.getAttributes().forEachAttr { attr ->
                            when {
                                attr.getPrefix() == "xmlns" ->
                                    yield(
                                        XmlEvent.NamespaceImpl(
                                            attr.getLocalName() ?: attr.getName(),
                                            attr.getValue()
                                        )
                                    )

                                attr.getPrefix().isNullOrEmpty() && attr.getLocalName() == "xmlns" ->
                                    yield(XmlEvent.NamespaceImpl("", attr.getValue()))
                            }
                        }
                        c = c.getParentElement()
                    }
                }.iterator()
            }

            override fun getPrefixes(namespaceURI: String): Iterator<String> {
                // TODO return all possible ones by doing so recursively
                return listOfNotNull(getPrefix(namespaceURI)).iterator()
            }
        }

    override val namespaceDecls: List<Namespace>
        get() {
            return namespaceAttrs.map { attr ->
                when {
                    attr.getPrefix() == "xmlns" ->
                        XmlEvent.NamespaceImpl(attr.getLocalName()!!, attr.getValue())

                    else ->
                        XmlEvent.NamespaceImpl("", attr.getValue())
                }
            }
        }

    override val encoding: String?
        get() {
            val d = delegate
            // Note that unchecked cast is thrown for javascript
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION", "UNCHECKED_CAST", "KotlinRedundantDiagnosticSuppress")
            return when (d.nodeType) {
                NodeConsts.DOCUMENT_NODE -> (d as Document).inputEncoding
                else -> d.ownerDocument!!.inputEncoding
            }
        }

    override val standalone: Boolean?
        get() = null // not defined on DOM.

    override val version: String get() = "1.0"

    override fun hasNext(): Boolean {
        return !(atEndOfElement && current?.parentNode == null) || current != delegate
    }

    override fun next(): EventType {
        if (eventType == EventType.END_ELEMENT) --depth

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
                        return current?.toEventType(true, expandEntities) ?: EventType.END_DOCUMENT
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
            val c2 = requireCurrent
            val nodeType = c2.nodeType
            if (nodeType != NodeConsts.ELEMENT_NODE && nodeType != NodeConsts.DOCUMENT_NODE) {
                atEndOfElement = true // No child elements for things like text
            }
            return c2.toEventType(atEndOfElement, expandEntities).also {
                if (it == EventType.START_ELEMENT) { ++depth }
            }
        }
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "KotlinRedundantDiagnosticSuppress")
    @Deprecated("Provided for compatibility.")
    public fun getDelegate(): Node1? = delegate as? Node1

    override fun getAttributeNamespace(index: Int): String {
        val attr: Attr = requireCurrentElem.getAttributes()[index] ?: throw IndexOutOfBoundsException()
        return attr.getNamespaceURI() ?: ""
    }

    override fun getAttributePrefix(index: Int): String {
        val attr: Attr = requireCurrentElem.getAttributes()[index] ?: throw IndexOutOfBoundsException()
        return attr.getPrefix() ?: ""
    }

    override fun getAttributeLocalName(index: Int): String {
        val attr: Attr = requireCurrentElem.getAttributes()[index] ?: throw IndexOutOfBoundsException()
        return attr.getLocalName() ?: attr.getName()
    }

    override fun getAttributeValue(index: Int): String {
        val attr: Attr = requireCurrentElem.getAttributes()[index] ?: throw IndexOutOfBoundsException()
        return attr.getValue()
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


private fun Node2.toEventType(endOfElement: Boolean, expandEntities: Boolean): EventType {
    @Suppress("DEPRECATION")
    return when (nodeType) {
        NodeConsts.ATTRIBUTE_NODE -> EventType.ATTRIBUTE
        NodeConsts.CDATA_SECTION_NODE -> EventType.CDSECT
        NodeConsts.COMMENT_NODE -> EventType.COMMENT
        NodeConsts.DOCUMENT_TYPE_NODE -> EventType.DOCDECL
        NodeConsts.ENTITY_REFERENCE_NODE if (expandEntities) -> EventType.TEXT
        NodeConsts.ENTITY_REFERENCE_NODE -> EventType.ENTITY_REF
        NodeConsts.DOCUMENT_FRAGMENT_NODE,
        NodeConsts.DOCUMENT_NODE -> if (endOfElement) EventType.END_DOCUMENT else EventType.START_DOCUMENT

        NodeConsts.PROCESSING_INSTRUCTION_NODE -> EventType.PROCESSING_INSTRUCTION
        NodeConsts.TEXT_NODE -> when {
            isXmlWhitespace(textContent!!) -> EventType.IGNORABLE_WHITESPACE
            else -> EventType.TEXT
        }

        NodeConsts.ELEMENT_NODE -> if (endOfElement) EventType.END_ELEMENT else EventType.START_ELEMENT

        else -> throw XmlException("Unsupported event type ($this)")
    }
}
