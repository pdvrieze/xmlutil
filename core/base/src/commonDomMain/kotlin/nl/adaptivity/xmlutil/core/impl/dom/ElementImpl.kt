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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.core.impl.idom.*
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformElement
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.NodeType

internal class ElementImpl(
    private var ownerDocument: DocumentImpl,
    private val namespaceURI: String?,
    private val localName: String,
    private val prefix: String?
) : NodeImpl(), IElement {
    constructor(ownerDocument: DocumentImpl, original: PlatformElement) : this(
        ownerDocument,
        original.getNamespaceURI(),
        original.getLocalName(),
        original.getPrefix()
    )

    override fun getOwnerDocument(): DocumentImpl = ownerDocument

    override fun setOwnerDocument(ownerDocument: DocumentImpl) {
        if (this.ownerDocument !== ownerDocument) {
            setParentNode(null)
            this.ownerDocument = ownerDocument
        }
    }

    override fun getNamespaceURI(): String? = namespaceURI

    override fun getPrefix(): String? = prefix

    override fun getLocalName(): String = localName

    override fun getNodetype(): NodeType = NodeType.ELEMENT_NODE

    override fun getNodeName(): String = getTagName()

    override fun getTagName(): String = when (prefix) {
        null, "" -> localName
        else -> "$prefix:$localName"
    }

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

    override fun setTextContent(value: String) {
        _childNodes.elements.clear()
        appendChild(getOwnerDocument().createTextNode(value))
    }

    override fun getElementsByTagName(qualifiedName: String): INodeList {
        val matchAll = qualifiedName == "*"
        val elems = mutableListOf<NodeImpl>()

        fun collect(p: ElementImpl) {
            for (c in p.getChildNodes()) {
                if (c is ElementImpl) {
                    if (matchAll || c.getTagName() == qualifiedName) {
                        elems.add(c)
                    }
                    collect(c)
                }
            }
        }
        collect(this)
        return NodeListImpl(elems)
    }

    override fun getElementsByTagNameNS(namespace: String?, localName: String): INodeList {
        val _namespace = namespace ?: ""

        val matchAllNs = namespace == "*"
        val matchAllLocalname = localName == "*"
        val elems = mutableListOf<NodeImpl>()

        fun collect(p: ElementImpl) {
            for (it in p.getChildNodes()) {
                if (it is ElementImpl) {
                    if ((matchAllNs || ((it.getNamespaceURI() ?: "") == _namespace)) &&
                        (matchAllLocalname || it.getLocalName() == localName)) {
                        elems.add(it)
                    }
                    collect(it)
                }
            }
        }
        collect(this)
        return NodeListImpl(elems)
    }

    override fun appendChild(node: PlatformNode): INode {
        val n = checkNode(node)
        when (n) {
            is DocumentFragmentImpl -> {
                val nodes = _childNodes.elements.toList()
                _childNodes.elements.clear()
                for (n2 in nodes) {
                    appendChild(n2)
                }
            }

            else -> {
                n.getParentNode()?.removeChild(n)

                n.setParentNode(this)

                _childNodes.elements.add(n)
            }
        }
        return n
    }

    override fun replaceChild(oldChild: PlatformNode, newChild: PlatformNode): INode {
        val old = checkNode(oldChild)
        val idx = _childNodes.indexOf(old)
        if (idx < 0) throw DOMException()
        val newNode = checkNode(newChild)

        _childNodes.elements[idx].setParentNode(null)
        _childNodes.elements[idx] = newNode
        newNode.setParentNode(this)

        return old
    }

    override fun removeChild(node: PlatformNode): INode {
        val n = checkNode(node)

        if (!_childNodes.elements.remove(n)) throw DOMException("Node to remove not found")

        n.setParentNode(null)
        return n
    }

    private fun getAttrIdxNS(namespaceURI: String?, localName: String) = _attributes.indexOfFirst {
        (it.getNamespaceURI() ?: "") == (namespaceURI ?: "") && it.getLocalName() == localName
    }

    private fun getAttrIdx(name: String) = _attributes.indexOfFirst { it.getName() == name }

    private fun setAttrAt(elementIdx: Int, newAttr: AttrImpl): AttrImpl? {
        val oldAttr = getAttr(elementIdx)

        when (oldAttr) {
            null -> _attributes.add(newAttr)
            else -> {
                oldAttr.setOwnerElement(null)
                _attributes[elementIdx] = newAttr
            }
        }

        newAttr.setOwnerElement(this@ElementImpl)
        return oldAttr
    }

    private fun removeAttrAt(idx: Int): AttrImpl? {
        return when {
            idx < 0 -> null
            else -> _attributes[idx].apply {
                setOwnerElement(null)
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
            idx >= 0 -> _attributes[idx].setValue(value)
            else -> _attributes.add(
                AttrImpl(getOwnerDocument(), namespaceURI, localName, prefix, value).apply {
                    setOwnerElement(this@ElementImpl)
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

    override fun setAttributeNode(attr: PlatformAttr): IAttr? {
        return getAttributes().setNamedItem(attr)
    }

    override fun setAttributeNodeNS(attr: PlatformAttr): IAttr? {
        return getAttributes().setNamedItemNS(attr)
    }

    override fun removeAttributeNode(attr: PlatformAttr): IAttr {
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

        // Cast is needed as we don't want to match Document/DocumentFragment (infinite recursion)
        return (getParentNode() as? PlatformElement)?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        if (getNamespaceURI() != null && this.getPrefix() == prefix) return getNamespaceURI()

        if (prefix.isBlank()) {
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

        // Cast is needed as we don't want to match Document/DocumentFragment (infinite recursion)
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

        override fun setNamedItem(attr: PlatformAttr): IAttr? {
            val a = checkNode(attr)
            if (a !is AttrImpl) throw DOMException("")

            return setAttrAt(getAttrIdx(a.getName()), a)
        }

        override fun setNamedItemNS(attr: PlatformAttr): IAttr? {
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
