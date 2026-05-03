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
import nl.adaptivity.xmlutil.dom.PlatformNode
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

@ExperimentalXmlUtilApi
public abstract class AbstractDocument<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    nodeStorage: (P) -> AbstractNodeStorage<N, P>
) : AbstractParentNode<N, P>(null, nodeStorage = nodeStorage), Document {
    override fun getOwnerDocument(): Nothing? = null

    final override fun getNodetype(): NodeType = NodeType.DOCUMENT_NODE
    final override fun getNodeValue(): Nothing? = null
    final override fun getNodeName(): String = "#document"

    override fun lookupPrefix(namespace: String): String? {
        return when (namespace) {
            XMLConstants.DEFAULT_NS_PREFIX -> XMLConstants.NULL_NS_URI
            XMLConstants.XML_NS_URI -> XMLConstants.XML_NS_PREFIX
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> XMLConstants.XMLNS_ATTRIBUTE
            else -> null
        }
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        return when (prefix) {
            XMLConstants.NULL_NS_URI -> XMLConstants.DEFAULT_NS_PREFIX
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
            is Attr -> {
                val n = node.getName()
                when {
                    ':' in n -> createAttributeNS(node.getNamespaceURI(), n)
                    else -> createAttribute(n)
                }
            }

            is Element -> {
                val r = when (val u = node.namespaceURI) {
                    null, "" -> createElement(node.localName)
                    else -> createElementNS(u, node.localName)
                }
                for (a in node.attributes) {
                    val n = a.getName()
                    when {
                        ':' in n -> r.setAttributeNS(a.namespaceURI, n, a.getValue())
                        else -> r.setAttribute(n, a.getValue())
                    }
                }
                for (c in childNodes) r.appendChild(importNode(c, true))

                r
            }

            is DocumentFragment if deep -> createDocumentFragment().also { t ->
                for (c in childNodes) t.appendChild(importNode(c, true))
            }

            is DocumentFragment -> createDocumentFragment()

            is CDATASection -> createCDATASection(node.data)

            is Text -> createTextNode(node.data)

            is Comment -> createComment(node.data)
            is ProcessingInstruction -> createProcessingInstruction(node.target, node.data)
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
