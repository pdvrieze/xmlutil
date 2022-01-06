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

import nl.adaptivity.xmlutil.XMLConstants
import org.w3c.dom.*

internal class ElementImpl(
    ownerDocument: DocumentImpl,
    override val namespaceURI: String?,
    override val localName: String,
    override val prefix: String?
) : NodeImpl(ownerDocument), Element {
    constructor(ownerDocument: DocumentImpl, original: Element) : this(
        ownerDocument,
        original.namespaceURI,
        original.localName,
        original.prefix
    )

    override val nodeType: Short get() = Node.ELEMENT_NODE

    override val nodeName: String get() = tagName

    override val tagName: String
        get() = when (prefix) {
            null -> localName
            else -> "$prefix:$localName"
        }

    final override var parentNode: Node? = null

    private val _childNodes: NodeListImpl = NodeListImpl()

    override val childNodes: NodeList
        get() = _childNodes

    private val _attributes: MutableList<AttrImpl> = mutableListOf()

    override val attributes: NamedNodeMap = AttrMap()

    override val firstChild: Node? get() = _childNodes.elements.firstOrNull()

    override val lastChild: Node? get() = _childNodes.elements.lastOrNull()

    override fun getElementsByTagName(qualifiedName: String): NodeList {
        val elems = _childNodes.elements
            .filter { it is Element && it.tagName == qualifiedName }
            .toMutableList()
        return NodeListImpl(elems)
    }

    override fun getElementsByTagNameNS(namespace: String?, localName: String): NodeList {
        val elems = _childNodes.elements
            .filter {
                it is Element &&
                        (it.namespaceURI?:"") == (namespaceURI ?:"") &&
                        it.localName == localName
            }.toMutableList()
        return NodeListImpl(elems)
    }

    override fun appendChild(node: Node): Node {
        val n = checkNode(node)
        if (n is DocumentFragmentImpl) {
            val nodes = _childNodes.elements.toList()
            _childNodes.elements.clear()
            for (n2 in nodes) {
                appendChild(n2)
            }
        } else {
            if (n.parentNode != null) n.parentNode = this

            _childNodes.elements.add(n)
        }
        return n
    }

    override fun replaceChild(oldChild: Node, newChild: Node): Node {
        val idx = _childNodes.indexOf(checkNode(oldChild))
        if (idx <0) throw DOMException()
        val newNode = checkNode(newChild)

        _childNodes.elements[idx].parentNode = null
        _childNodes.elements[idx] = newNode
        newNode.parentNode = this

        return oldChild
    }

    override fun removeChild(node: Node): Node {
        val n = checkNode(node)

        if (!_childNodes.elements.remove(n)) throw DOMException("Node to remove not found")

        n.parentNode = null
        return n
    }

    private fun getAttrIdxNS(namespaceURI: String?, localName: String) = _attributes.indexOfFirst {
        (it.namespaceURI ?: "") == (namespaceURI ?: "") && it.localName == localName
    }

    private fun getAttrIdx(name: String) = _attributes.indexOfFirst { it.name == name }

    private fun setAttrAt(elementIdx: Int, newAttr: AttrImpl): AttrImpl? {
        val oldAttr = getAttr(elementIdx)?.apply {
            ownerElement = null
        }

        when (oldAttr) {
            null -> _attributes.add(newAttr)
            else -> _attributes[elementIdx] = newAttr
        }

        newAttr.ownerElement = this@ElementImpl
        return oldAttr
    }

    private fun removeAttrAt(idx: Int): AttrImpl? {
        return when {
            idx < 0 -> null
            else -> _attributes[idx].apply {
                ownerElement = null
            }
        }
    }

    private fun getAttr(idx: Int): AttrImpl? = when {
        idx < 0 -> null
        else -> _attributes[idx]
    }

    override fun getAttribute(qualifiedName: String): String? {
        return getAttr(getAttrIdx(qualifiedName))?.value
    }

    override fun getAttributeNS(namespace: String?, localName: String): String? {
        return getAttr(getAttrIdxNS(namespace, localName))?.value
    }

    override fun setAttribute(qualifiedName: String, value: String) {
        val prefix = qualifiedName.substringBeforeLast(':', "")
        setAttributeAt(
            getAttrIdx(qualifiedName),
            lookupNamespaceURI(prefix),
            qualifiedName.substringAfterLast(':', qualifiedName),
            prefix,
            value
        )
    }

    override fun setAttributeNS(namespace: String?, localName: String, value: String) {
        setAttributeAt(
            getAttrIdxNS(namespace, localName),
            namespace,
            localName,
            lookupPrefix(namespace),
            value
        )
    }

    private fun setAttributeAt(
        idx: Int,
        namespaceURI: String?,
        localName: String,
        prefix: String?,
        value: String
    ): Any {
        return when {
            idx >= 0 -> _attributes[idx].value = value
            else -> _attributes.add(
                AttrImpl(ownerDocument, namespaceURI, localName, prefix, value).apply {
                    ownerElement = this@ElementImpl
                }
            )
        }
    }


    override fun removeAttribute(qualifiedName: String) {
        attributes.removeNamedItem(qualifiedName)
    }

    override fun removeAttributeNS(namespace: String?, localName: String) {
        attributes.removeNamedItemNS(namespace, localName)
    }

    override fun hasAttribute(qualifiedName: String): Boolean {
        return getAttrIdx(qualifiedName) >= 0
    }

    override fun hasAttributeNS(namespace: String?, localName: String): Boolean {
        return getAttrIdxNS(namespace, localName) >= 0
    }

    override fun getAttributeNode(qualifiedName: String): Attr? {
        return attributes.getNamedItem(qualifiedName) as Attr?
    }

    override fun getAttributeNodeNS(namespace: String?, localName: String): Attr? {
        return attributes.getNamedItemNS(namespace, localName) as Attr?
    }

    override fun setAttributeNode(attr: Attr) {
        attributes.setNamedItem(attr)
    }

    override fun setAttributeNodeNS(attr: Attr) {
        attributes.setNamedItemNS(attr)
    }

    override fun removeAttributeNode(attr: Attr) {
        _attributes.remove(checkNode(attr) as AttrImpl)
    }

    override fun lookupPrefix(namespace: String?): String? {
        if (namespaceURI == namespace && prefix != null) return prefix

        _attributes.firstOrNull { it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI && it.value == namespace }
            ?.let { return it.localName }

        return (parentNode as? Element)?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String?): String? {
        if (namespaceURI != null && this.prefix == prefix) return namespaceURI

        if (prefix.isNullOrBlank()) {
            _attributes.firstOrNull {
                it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI &&
                        it.prefix.isNullOrBlank() &&
                        it.localName == XMLConstants.XMLNS_ATTRIBUTE
            }?.let { return it.value.takeUnless { it.isEmpty() } }
        } else {
            _attributes.firstOrNull {
                it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI &&
                        it.prefix == XMLConstants.XMLNS_ATTRIBUTE &&
                        it.localName == prefix
            }?.let { return it.value.takeUnless { it.isEmpty() } }
        }

        return (parentNode as? Element)?.lookupNamespaceURI(prefix)
    }

    inner class AttrMap() : NamedNodeMap {
        override val length: Int get() = _attributes.size

        override fun item(index: Int): Attr? = when (index) {
            in 0 until _attributes.size -> _attributes[index]
            else -> null
        }

        override fun getNamedItem(qualifiedName: String): Attr? = _attributes.firstOrNull {
            it.name == qualifiedName
        }

        override fun getNamedItemNS(namespace: String?, localName: String): Attr? = _attributes.firstOrNull {
            (it.namespaceURI ?: "") == (namespace ?: "") && it.localName == localName
        }

        override fun setNamedItem(attr: Node): Attr? {
            val a = checkNode(attr)
            if (a !is AttrImpl) throw DOMException("")

            return setAttrAt(getAttrIdx(a.name), a)
        }

        override fun setNamedItemNS(attr: Node): Attr? {
            val a = checkNode(attr)
            if (a !is AttrImpl) throw DOMException("")

            return setAttrAt(getAttrIdxNS(a.namespaceURI, a.localName), a)
        }

        override fun removeNamedItem(qualifiedName: String): Attr? {
            return removeAttrAt(getAttrIdx(qualifiedName))
        }

        override fun removeNamedItemNS(namespace: String?, localName: String): Attr? {
            return removeAttrAt(getAttrIdxNS(namespaceURI, localName))
        }
    }
}
