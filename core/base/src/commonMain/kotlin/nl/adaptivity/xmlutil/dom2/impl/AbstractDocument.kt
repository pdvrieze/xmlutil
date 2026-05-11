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
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformCDATASection
import nl.adaptivity.xmlutil.dom.PlatformComment
import nl.adaptivity.xmlutil.dom.PlatformDocumentFragment
import nl.adaptivity.xmlutil.dom.PlatformElement
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.PlatformProcessingInstruction
import nl.adaptivity.xmlutil.dom.PlatformText
import nl.adaptivity.xmlutil.dom.attributes
import nl.adaptivity.xmlutil.dom.childNodes
import nl.adaptivity.xmlutil.dom.getData
import nl.adaptivity.xmlutil.dom.getName
import nl.adaptivity.xmlutil.dom.getNamespaceURI
import nl.adaptivity.xmlutil.dom.getNodeName
import nl.adaptivity.xmlutil.dom.getValue
import nl.adaptivity.xmlutil.dom.iterator
import nl.adaptivity.xmlutil.dom.localName
import nl.adaptivity.xmlutil.dom.name
import nl.adaptivity.xmlutil.dom.namespaceURI
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.CDATASection
import nl.adaptivity.xmlutil.dom2.Comment
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.DocumentFragment
import nl.adaptivity.xmlutil.dom2.DocumentType
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.ProcessingInstruction
import nl.adaptivity.xmlutil.dom2.Text
import nl.adaptivity.xmlutil.dom2.attributes
import nl.adaptivity.xmlutil.dom2.childNodes
import nl.adaptivity.xmlutil.dom2.data
import nl.adaptivity.xmlutil.dom2.localName
import nl.adaptivity.xmlutil.dom2.namespaceURI
import nl.adaptivity.xmlutil.dom2.parentNode
import nl.adaptivity.xmlutil.dom2.target
import nl.adaptivity.xmlutil.dom2.value

@ExperimentalXmlUtilApi
public abstract class AbstractDocument<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    nodeStorage: (P) -> MutableAbstractNodeStorage<N, P>
) : AbstractParentNode<N, P>(null, nodeStorage = nodeStorage), Document {
    override fun getOwnerDocument(): Nothing? = null

    final override fun getNodetype(): NodeType = NodeType.DOCUMENT_NODE
    final override fun getNodeValue(): Nothing? = null
    final override fun getNodeName(): String = "#document"

    final override fun getTextContent(): Nothing? = null

    final override fun setTextContent(value: String?) {/* Defined as NO-OP*/ }

    abstract override fun getDocumentElement(): AbstractElement<N, P>?

    final override fun getParentElement(): Nothing? = null

    final override fun getParentNode(): Nothing? = null

    final override fun getPreviousSibling(): Nothing? = null

    final override fun getNextSibling(): Nothing? = null

    final override fun getAttributes(): Nothing? = null

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
        val newNode = when (node) {
            is PlatformAttr -> {
                val n = node.getName()
                when {
                    ':' in n -> createAttributeNS(node.getNamespaceURI(), n)
                    n == "xmlns" -> createAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, n)
                    else -> createAttribute(n)
                }.also { it.value = node.getValue() }
            }

            is PlatformElement -> {
                val r = when (val u = node.namespaceURI) {
                    null, "" -> createElement(node.localName)
                    else -> createElementNS(u, node.name)
                }
                for (a in node.attributes) {
                    val n = a.getName()
                    when {
                        ':' in n -> r.setAttributeNS(a.getNamespaceURI(), n, a.getValue())
                        n == "xmlns" -> r.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, n, a.getValue())
                        else -> r.setAttribute(n, a.getValue())
                    }
                }
                for (c in node.childNodes) r.appendChild(importNode(c, true))

                r
            }

            is PlatformDocumentFragment if deep -> createDocumentFragment().also { t ->
                for (c in node.childNodes) t.appendChild(importNode(c, true))
            }

            is PlatformDocumentFragment -> createDocumentFragment()

            is PlatformCDATASection -> createCDATASection(node.getData())

            is PlatformText -> createTextNode(node.getData())

            is PlatformComment -> createComment(node.getData())
            is PlatformProcessingInstruction -> createProcessingInstruction(node.getNodeName(), node.getData())
            else -> throw IllegalArgumentException("Cannot import node of type ${node::class.simpleName}")
        }

        @Suppress("UNCHECKED_CAST")
        return newNode as N
    }

    override fun createAttribute(localName: String): AbstractAttr<N, P> {
        return createAttributeNS("", localName)
    }

    override fun createElement(localName: String): AbstractElement<N, P> {
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
}
