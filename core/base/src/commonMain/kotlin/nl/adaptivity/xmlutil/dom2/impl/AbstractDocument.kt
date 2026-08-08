/*
 * Copyright (c) 2026.
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

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.dom2.*

@ExperimentalXmlUtilApi
public abstract class AbstractDocument<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    nodeStorage: (P) -> MutableAbstractNodeStorage<N, P>
) : AbstractParentNode<N, P>(null, nodeStorage = nodeStorage), Document {
    override fun getOwnerDocument(): Nothing? = null

    private var documentURI: String? = null

    final override fun getNodetype(): NodeType = NodeType.DOCUMENT_NODE
    final override fun getNodeValue(): Nothing? = null

    @ExperimentalXmlUtilApi
    final override fun setNodeValue(value: String?) {
        /** defined as NO-OP */
    }

    final override fun getNodeName(): String = "#document"

    final override fun getNamespaceURI(): Nothing? = null

    final override fun getPrefix(): Nothing? = null

    final override fun getLocalName(): Nothing? = null

    final override fun getTextContent(): Nothing? = null

    final override fun setTextContent(value: String?) {/* Defined as NO-OP*/ }

    abstract override fun getDocumentElement(): AbstractElement<N, P>?

    final override fun getParentElement(): Nothing? = null

    final override fun getParentNode(): Nothing? = null

    final override fun getPreviousSibling(): Nothing? = null

    final override fun getNextSibling(): Nothing? = null

    final override fun getAttributes(): Nothing? = null

    final override fun hasAttributes(): Boolean = false

    final override fun lookupPrefix(namespace: String): String? {
        getDocumentElement()?.let { return it.lookupPrefix(namespace) }

        return when (namespace) {
            XMLConstants.NULL_NS_URI -> {
                val docElem = getDocumentElement() ?: return XMLConstants.DEFAULT_NS_PREFIX
                when (val defaultNS = docElem.lookupNamespaceURI("")) {
                    XMLConstants.NULL_NS_URI, null -> XMLConstants.DEFAULT_NS_PREFIX
                    else -> null
                }
            }

            XMLConstants.XML_NS_URI -> XMLConstants.XML_NS_PREFIX

            XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> XMLConstants.XMLNS_ATTRIBUTE

            else -> null
        }
    }

    final override fun lookupNamespaceURI(prefix: String): String? {
        getDocumentElement()?.let { return it.lookupNamespaceURI(prefix) }

        return when (prefix) {
            XMLConstants.XML_NS_PREFIX -> XMLConstants.XML_NS_URI
            XMLConstants.XMLNS_ATTRIBUTE -> XMLConstants.XMLNS_ATTRIBUTE_NS_URI
            else -> null
        }
    }

    private fun AbstractElement<N, P>.getElementById(elementId: String): AbstractElement<N, P>? {
        for (a in getAttributes()) {
            if (a.isId() && a.getValue() == elementId) {
                return this
            }
        }

        return getChildNodes().asSequence()
            .filterIsInstance<AbstractElement<N, P>>()
            .mapNotNull { it.getElementById(elementId) }
            .firstOrNull()
    }

    override fun getElementById(elementId: String): AbstractElement<N, P>? {
        return getChildNodes().asSequence()
            .filterIsInstance<AbstractElement<N, P>>()
            .mapNotNull { it.getElementById(elementId) }
            .firstOrNull()
    }

    override fun getElementsByTagName(qualifiedName: String): AbstractNodeList<N, P> {
        val docElem = getDocumentElement() ?: return EmptyNodeList
        val elems = mutableListOf<N>()
        collectElementsByTagName(docElem, qualifiedName, elems)
        return NodeListImpl(elems)
    }

    override fun getElementsByTagNameNS(
        namespace: String?,
        localName: String
    ): AbstractNodeList<N, P> {
        val docElem = getDocumentElement() ?: return EmptyNodeList
        val elems = mutableListOf<N>()
        collectElementsByTagNameNS(docElem, namespace?:"", localName, elems)
        return NodeListImpl(elems)
    }

    /**
     * Adopt a node from another document but the same implementation.
     */
    abstract override fun adoptNode(node: PlatformNode): N?

    protected fun adoptNodeImpl(node: @UnsafeVariance N): N {
        when (node) {
            is Document -> throw IllegalArgumentException("Cannot adopt a document")
            is DocumentType -> throw IllegalArgumentException("Cannot adopt a document type")
        }
        node.parentNode?.removeChild(node)
        node.setOwnerDocument(this)

        return node
    }

    override fun importNode(
        node: PlatformNode,
        deep: Boolean
    ): N {
        val nt = (node as? Node)?.getNodetype()?.value ?: node.nodeType
        val newNode = when (nt) {
            NodeConsts.ATTRIBUTE_NODE -> {
                val a = node.asPlatformAttr()
                val n = a.getName()
                when {
                    ':' in n -> createAttributeNS(a.getNamespaceURI(), n)
                    n == "xmlns" -> createAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, n)
                    else -> createAttribute(n)
                }.also { it.value = a.getValue() }
            }

            NodeConsts.ELEMENT_NODE -> {
                val elem = node.asPlatformElement()
                val u = elem.namespaceURI
                val r = when {
                    u.isNullOrEmpty() -> createElement(elem.localName)
                    else -> createElementNS(u, elem.name)
                }
                for (n in elem.attributes) {
                    val a = n.asPlatformAttr()
                    val name = a.getName()
                    when {
                        ':' in name -> r.setAttributeNS(a.getNamespaceURI(), name, a.getValue())
                        name == "xmlns" -> r.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, name, a.getValue())
                        else -> r.setAttribute(name, a.getValue())
                    }
                }
                if (deep) {
                    for (c in elem.childNodes) {
                        val importedNode = importNode(c, true)
                        r.appendChild(importedNode)
                    }
                }

                r
            }

            NodeConsts.DOCUMENT_FRAGMENT_NODE -> when {
                deep -> createDocumentFragment().also { t ->
                    for (c in node.asPlatformDocumentFragment().childNodes) t.appendChild(importNode(c, true))
                }

                else -> createDocumentFragment()
            }

            NodeConsts.CDATA_SECTION_NODE -> createCDATASection(node.asPlatformCharacterData().getData())

            NodeConsts.TEXT_NODE -> createTextNode(node.asPlatformCharacterData().getData())

            NodeConsts.COMMENT_NODE -> createComment(node.asPlatformCharacterData().getData())

            NodeConsts.PROCESSING_INSTRUCTION_NODE -> {
                val pi = node.asPlatformProcessingInstruction()
                createProcessingInstruction(pi.getNodeName(), pi.getData())
            }

            else -> throw DOMException.notSupportedErr("Cannot import node  ${node}, nodeType: ${nt}")
        }

        @Suppress("UNCHECKED_CAST")
        return newNode as N
    }

    override fun appendChild(node: PlatformNode): N {
        if (node is AbstractDocumentType<*, *> && node.getOwnerDocument() == null) {
            node.setOwnerDocument(this)
        }
        return super.appendChild(node)
    }

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): N {
        require(newChild !is AbstractDocumentType<*, *> || newChild == oldChild) {
            "Document types cannot be replaced"
        }
        return super.replaceChild(newChild, oldChild)
    }

    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): N {
        if (newChild is AbstractDocumentType<*, *> && newChild.getOwnerDocument() == null) {
            newChild.setOwnerDocument(this)
        }
        return super.insertBefore(newChild, refChild)
    }

    override fun createAttribute(localName: String): AbstractAttr<N, P> {
        return createAttributeNS("", localName)
    }

    override fun createElement(localName: String): AbstractElement<N, P> {
        checkNotNull(localName) { "Local name cannot be null or empty" }
        return createElementNS("", localName)
    }

    override fun setOwnerDocument(ownerDocument: AbstractDocument<@UnsafeVariance N, @UnsafeVariance P>): Nothing {
        throw UnsupportedOperationException("Cannot set owner document of a document")
    }

    abstract override fun createAttributeNS(namespace: String?, qualifiedName: String): AbstractAttr<N, P>

    abstract override fun createElementNS(namespaceURI: String, qualifiedName: String): AbstractElement<N, P>

    abstract override fun createDocumentFragment(): AbstractDocumentFragment<N, P>

    abstract override fun createTextNode(data: String): AbstractText<N, P>

    abstract override fun createCDATASection(data: String): AbstractCDataSection<N, P>

    abstract override fun createComment(data: String): AbstractComment<N, P>

    abstract override fun createProcessingInstruction(target: String, data: String):
            AbstractProcessingInstruction<N, P>

    abstract override fun cloneNode(deep: Boolean): AbstractDocument<N, P>

    override fun isEqualNode(other: PlatformNode): Boolean {
        return when {
            this === other -> true
            nodeType != other.nodeType -> false //handle javascript instance check issues
            other !is AbstractDocument<*,*> -> false

            else -> nodeStorage.isEqualNodes(other.getChildNodes())
        }
    }

    override fun getBaseURI(): String? {
        return documentURI
    }

    override fun setDocumentURI(documentURI: String?) {
        this.documentURI = documentURI
    }

}
