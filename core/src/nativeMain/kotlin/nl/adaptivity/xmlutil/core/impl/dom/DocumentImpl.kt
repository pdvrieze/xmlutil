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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.isXmlWhitespace
import nl.adaptivity.xmlutil.dom.*

internal class DocumentImpl(override val doctype: DocumentType?) : Document {
    override val implementation: DOMImplementation get() = SimpleDOMImplementation

    private var _documentElement: ElementImpl? = null
    override val documentElement: Element? get() = _documentElement

    override var characterSet: String = "UTF-8"

    override val nodeType: Short get() = Node.DOCUMENT_NODE

    override val nodeName: String get() = "#document"

    override val ownerDocument: Document get() = this

    override var parentNode: Node?
        get() = null
        set(_) {
            throw UnsupportedOperationException()
        }

    private val _childNodes: NodeListImpl = NodeListImpl()

    override val childNodes: NodeList
        get() = _childNodes

    override val firstChild: Node? get() = _childNodes.elements.firstOrNull()

    override val lastChild: Node? get() = _childNodes.elements.lastOrNull()

    override val previousSibling: Node? get() = null

    override val nextSibling: Node? get() = null

    override val textContent: String? get() = null

    override fun adoptNode(node: Node): Node {
        if (node !is NodeImpl) throw DOMException("Not supported")
        if (node.ownerDocument === this) return node
        node.parentNode?.removeChild(node)
        node.ownerDocument = this
        return node
    }

    override fun importNode(node: Node, deep: Boolean): Node = when (node) {
        is Attr -> AttrImpl(this, node)
        is CDATASection -> CDATASectionImpl(this, node)
        is Comment -> CommentImpl(this, node)
        is Document -> throw DOMException("Documents cannot be imported")
        is DocumentFragment -> DocumentFragmentImpl(this).also { cpy ->
            if (deep) {
                node.childNodes.forEach { child -> cpy.appendChild(importNode(child, deep)) }
            }
        }
        is Element -> ElementImpl(this, node).also { cpy ->
            if (deep) {
                node.childNodes.forEach { child -> cpy.appendChild(importNode(child, deep)) }
            }
        }
        is ProcessingInstruction -> ProcessingInstructionImpl(this, node)
        is Text -> TextImpl(this, node)
        else -> throw DOMException("Unsupported node subtype")
    }

    override fun appendChild(node: Node): Node {
        val n = checkNode(node)
        when (n) {
            is DocumentFragmentImpl -> for (child in n.childNodes) { appendChild(child); n._childNodes.elements.clear() }

            is ElementImpl -> {
                if (documentElement != null) throw UnsupportedOperationException("Only one root element is supported for now")
                _documentElement = n
            }
            is ProcessingInstructionImpl -> {}// fine
            is TextImpl -> require(n.data.isXmlWhitespace()) { "Non-whitespace nodes cannot be added directly to a document" }
            else -> throw IllegalArgumentException("Attempting to add node ${n.nodeType} where not permitted")
        }
        n.parentNode = this
        _childNodes.elements.add(n)

        return n
    }

    override fun removeChild(node: Node): Node {
        if (node != _documentElement) throw DOMException("Node is not a child of this document")
        _documentElement = null
        _childNodes.elements.remove(node)
        (node as? NodeImpl)?.let { it.parentNode = null }
        return node
    }

    override fun replaceChild(oldChild: Node, newChild: Node): Node {
        if (oldChild != _documentElement) throw DOMException("Old node not found in document")
        checkNode(newChild)
        if (newChild !is ElementImpl) throw UnsupportedOperationException("Only element children to root supported for now")
        _documentElement = newChild
        return oldChild
    }

    override fun createDocumentFragment(): DocumentFragment {
        return DocumentFragmentImpl(this)
    }

    override fun createElement(localName: String): Element {
        return ElementImpl(this, null, localName, null)
    }

    override fun createElementNS(namespaceURI: String, qualifiedName: String): Element {
        val localName = qualifiedName.substringAfterLast(':', qualifiedName)
        val prefix = qualifiedName.substringBeforeLast(':', "").takeUnless { it.isEmpty() }
        return ElementImpl(this, namespaceURI, localName, prefix)
    }

    override fun createAttribute(localName: String): Attr {
        return AttrImpl(this, null, localName, null, "")
    }

    override fun createAttributeNS(namespace: String?, qualifiedName: String): Attr {
        val localName = qualifiedName.substringAfterLast(':', qualifiedName)
        val prefix = qualifiedName.substringBeforeLast(':', "").takeUnless { it.isEmpty() }
        return AttrImpl(this, namespace, localName, prefix, "")
    }

    override fun createTextNode(data: String): Text {
        return TextImpl(this, data)
    }

    override fun createCDATASection(data: String): CDATASection {
        return CDATASectionImpl(this, data)
    }

    override fun createComment(data: String): Comment {
        return CommentImpl(this, data)
    }

    override fun createProcessingInstruction(target: String, data: String): ProcessingInstruction {
        return ProcessingInstructionImpl(this, target, data)
    }

    override fun lookupPrefix(namespace: String?): String? {
        return (_documentElement ?: return null).lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String?): String? {
        return (_documentElement ?: return null).lookupNamespaceURI(prefix)
    }

    override fun toString(): String = when (val e = _documentElement){
        null -> "<Empty Document>"
        else -> e.toString()
    }
}

internal fun Node.checkNode(node: Node): NodeImpl {
    if (node.ownerDocument != ownerDocument) throw DOMException("Node not owned by this document")
    if (node !is NodeImpl) throw DOMException("Unexpected node implementation, try importing")
    return node
}
