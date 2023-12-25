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
import nl.adaptivity.xmlutil.core.impl.idom.*
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom.Attr as Attr1
import nl.adaptivity.xmlutil.dom.Element as Element1
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2
import nl.adaptivity.xmlutil.dom2.Element as Element2
import nl.adaptivity.xmlutil.dom2.Node as Node2

internal class ElementImpl(
    ownerDocument: DocumentImpl,
    override val namespaceURI: String?,
    override val localName: String,
    override val prefix: String?
) : NodeImpl(ownerDocument), IElement {
    constructor(ownerDocument: DocumentImpl, original: Element1) : this(
        ownerDocument,
        original.namespaceURI,
        original.localName,
        original.prefix
    )

    constructor(ownerDocument: DocumentImpl, original: Element2) : this(
        ownerDocument,
        original.getNamespaceURI(),
        original.getLocalName(),
        original.getPrefix()
    )

    override val tagName: String get() = getTagName()

    override val nodetype: NodeType get() = NodeType.ELEMENT_NODE

    override fun getNodeName(): String = getTagName()

    override fun getTagName(): String = when (getPrefix()) {
        null -> getLocalName()
        else -> "${getPrefix()}:${getLocalName()}"
    }

    override var parentNode: INode? = null

    private val _childNodes: NodeListImpl = NodeListImpl()

    override fun getChildNodes(): INodeList = _childNodes

    private val _attributes: MutableList<AttrImpl> = mutableListOf()

    override fun getAttributes(): INamedNodeMap = AttrMap()

    override fun getFirstChild(): INode? = _childNodes.elements.firstOrNull()

    override fun getLastChild(): INode? = _childNodes.elements.lastOrNull()

    override fun getTextContent(): String = buildString {
        for (n in getChildNodes()) {
            appendTextContent(n)
        }
    }

    override fun getElementsByTagName(qualifiedName: String): INodeList {
        val elems = _childNodes.elements
            .filter { it is Element1 && it.tagName == qualifiedName }
            .toMutableList()
        return NodeListImpl(elems)
    }

    override fun getElementsByTagNameNS(namespace: String?, localName: String): INodeList {
        val elems = _childNodes.elements
            .filter {
                it is Element1 &&
                        (it.namespaceURI ?: "") == (getNamespaceURI() ?: "") &&
                        it.localName == localName
            }.toMutableList()
        return NodeListImpl(elems)
    }

    override fun appendChild(node: INode): INode {
        val n = checkNode(node as Node2)
        when (n) {
            is DocumentFragmentImpl -> {
                val nodes = _childNodes.elements.toList()
                _childNodes.elements.clear()
                for (n2 in nodes) {
                    appendChild(n2)
                }
            }

            else -> {
                n.parentNode?.removeChild(n)

                n.parentNode = this

                _childNodes.elements.add(n)
            }
        }
        return n
    }

    override fun replaceChild(oldChild: INode, newChild: INode): INode {
        val idx = _childNodes.indexOf(checkNode(oldChild))
        if (idx < 0) throw DOMException()
        val newNode = checkNode(newChild)

        _childNodes.elements[idx].parentNode = null
        _childNodes.elements[idx] = newNode
        newNode.parentNode = this

        return oldChild
    }

    override fun removeChild(node: INode): INode {
        val n = checkNode(node)

        if (!_childNodes.elements.remove(n)) throw DOMException("Node to remove not found")

        n.parentNode = null
        return n
    }

    private fun getAttrIdxNS(namespaceURI: String?, localName: String) = _attributes.indexOfFirst {
        (it.getNamespaceURI() ?: "") == (namespaceURI ?: "") && it.getLocalName() == localName
    }

    private fun getAttrIdx(name: String) = _attributes.indexOfFirst { it.getName() == name }

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
        return getAttr(getAttrIdx(qualifiedName))?.getValue()
    }

    override fun getAttributeNS(namespace: String?, localName: String): String? {
        return getAttr(getAttrIdxNS(namespace, localName))?.getValue()
    }

    override fun setAttribute(qualifiedName: String, value: String) {
        val prefix = qualifiedName.substringBeforeLast(':', "")
        val namespaceURI = if (prefix.isEmpty()) "" else lookupNamespaceURI(prefix)
        setAttributeAt(
            getAttrIdx(qualifiedName),
            namespaceURI,
            qualifiedName.substringAfterLast(':', qualifiedName),
            prefix,
            value
        )
    }

    override fun setAttributeNS(namespace: String?, cName: String, value: String) {
        val localName = cName.substringAfterLast(':', cName)
        val prefix = cName.substringBeforeLast(':', "")
        setAttributeAt(
            getAttrIdxNS(namespace, localName),
            namespace,
            localName,
            prefix,
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
                AttrImpl(getOwnerDocument(), namespaceURI, localName, prefix, value).apply {
                    ownerElement = this@ElementImpl
                }
            )
        }
    }


    override fun removeAttribute(qualifiedName: String) {
        getAttributes().removeNamedItem(qualifiedName)
    }

    override fun removeAttributeNS(namespace: String?, localName: String) {
        getAttributes().removeNamedItemNS(namespace, localName)
    }

    override fun hasAttribute(qualifiedName: String): Boolean {
        return getAttrIdx(qualifiedName) >= 0
    }

    override fun hasAttributeNS(namespace: String?, localName: String): Boolean {
        return getAttrIdxNS(namespace, localName) >= 0
    }

    override fun getAttributeNode(qualifiedName: String): IAttr? {
        return getAttributes().getNamedItem(qualifiedName)
    }

    override fun getAttributeNodeNS(namespace: String?, localName: String): IAttr? {
        return getAttributes().getNamedItemNS(namespace, localName)
    }

    override fun setAttributeNode(attr: Attr1): IAttr? {
        return getAttributes().setNamedItem(attr)
    }

    override fun setAttributeNode(attr: Attr2): IAttr? {
        return getAttributes().setNamedItem(attr)
    }

    override fun setAttributeNodeNS(attr: Attr1): IAttr? {
        return getAttributes().setNamedItemNS(attr)
    }

    override fun setAttributeNodeNS(attr: Attr2): IAttr? {
        return getAttributes().setNamedItemNS(attr)
    }

    override fun removeAttributeNode(attr: Attr1): IAttr {
        val a = checkNode(attr) as AttrImpl
        if (!_attributes.remove(a)) {
            throw DOMException("Missing attribute for removal")
        }
        return a
    }

    override fun removeAttributeNode(attr: Attr2): IAttr {
        val a = checkNode(attr) as AttrImpl
        if (!_attributes.remove(a)) {
            throw DOMException("Missing attribute for removal")
        }
        return a
    }

    override fun lookupPrefix(namespace: String): String? {
        if (getNamespaceURI() == namespace && getPrefix() != null) return getPrefix()

        _attributes.firstOrNull { it.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI && it.getValue() == namespace }
            ?.let { return it.getLocalName() }

        return (getParentNode() as? Element1)?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        if (getNamespaceURI() != null && this.getPrefix() == prefix) return getNamespaceURI()

        if (prefix.isNullOrBlank()) {
            _attributes.firstOrNull {
                it.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI &&
                        it.getPrefix().isNullOrBlank() &&
                        it.getLocalName() == XMLConstants.XMLNS_ATTRIBUTE
            }?.let { return it.getValue().takeUnless(String::isEmpty) }
        } else {
            _attributes.firstOrNull {
                it.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI &&
                        it.getPrefix() == XMLConstants.XMLNS_ATTRIBUTE &&
                        it.getLocalName() == prefix
            }?.let { return it.getValue().takeUnless(String::isEmpty) }
        }

        return (getParentNode() as? IElement)?.lookupNamespaceURI(prefix)
    }

    override fun toString(): String {
        return buildString {
            append('<')
            val tagName = when {
                getPrefix().isNullOrEmpty() -> getLocalName()
                else -> "${getPrefix()}:${getLocalName()}"
            }
            append(tagName)

            if (_attributes.isNotEmpty()) append(' ')

            _attributes.joinTo(this, " ")

            if (_childNodes.isEmpty()) {
                append("/>")
            } else {
                append(">")
                _childNodes.elements.joinTo(this, "")
                append("</").append(tagName).append('>')
            }
        }
    }

    inner class AttrMap : INamedNodeMap {
        override val size: Int get() = _attributes.size

        override fun item(index: Int): IAttr? = when (index) {
            in 0 until _attributes.size -> _attributes[index]
            else -> null
        }

        override fun iterator(): Iterator<IAttr> = AttrIterator()

        override fun getNamedItem(qualifiedName: String): IAttr? = _attributes.firstOrNull {
            it.getName() == qualifiedName
        }

        override fun getNamedItemNS(namespace: String?, localName: String): IAttr? = _attributes.firstOrNull {
            (it.getNamespaceURI() ?: "") == (namespace ?: "") && it.getLocalName() == localName
        }

        override fun setNamedItem(attr: Node1): IAttr? {
            val a = checkNode(attr)
            if (a !is AttrImpl) throw DOMException("")

            return setAttrAt(getAttrIdx(a.getName()), a)
        }

        override fun setNamedItem(attr: Attr2): IAttr? {
            val a = checkNode(attr)
            if (a !is AttrImpl) throw DOMException("")

            return setAttrAt(getAttrIdx(a.getName()), a)
        }

        override fun setNamedItemNS(attr: Node1): IAttr? {
            val a = checkNode(attr)
            if (a !is AttrImpl) throw DOMException("")

            return setAttrAt(getAttrIdxNS(a.getNamespaceURI(), a.getLocalName()), a)
        }

        override fun setNamedItemNS(attr: Attr2): IAttr? {
            val a = checkNode(attr)
            if (a !is AttrImpl) throw DOMException("")

            return setAttrAt(getAttrIdxNS(a.getNamespaceURI(), a.getLocalName()), a)
        }

        override fun removeNamedItem(qualifiedName: String): IAttr? {
            return removeAttrAt(getAttrIdx(qualifiedName))
        }

        override fun removeNamedItemNS(namespace: String?, localName: String): IAttr? {
            return removeAttrAt(getAttrIdxNS(getNamespaceURI(), localName))
        }
    }

    inner class AttrIterator : Iterator<IAttr> {
        private var pos = 0

        override fun hasNext(): Boolean = pos < _attributes.size

        override fun next(): IAttr = _attributes[pos++]
    }
}
