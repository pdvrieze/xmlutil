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
import nl.adaptivity.xmlutil.dom2.*

@ExperimentalXmlUtilApi
public abstract class AbstractElement<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>,
    nodeStorage: (P) -> MutableAbstractNodeStorage<N, P>,
    attrStorage: (P) -> AbstractAttrStorage<AbstractAttr<N, P>>,
    parentNode: P? = null,
) : AbstractParentNode<N, P>(ownerDocument, nodeStorage, parentNode), Element {

    @Suppress("UNCHECKED_CAST")
    private val _attrStorage = attrStorage(this as P)

    final override fun getNodetype(): NodeType = NodeType.ELEMENT_NODE
    final override fun getNodeValue(): Nothing? = null

    final override fun getNodeName(): String = when (val p = getPrefix()) {
        null, "" -> getLocalName()
        else -> "$p:${getLocalName()}"
    }

    override fun getOwnerDocument(): AbstractDocument<N, P> {
        return checkNotNull(super.getOwnerDocument()) { "Elements cannot have a null owner document" }
    }

    final override fun getTextContent(): String = getTextContentImpl()

    final override fun getAttributes(): NamedNodeMap = _attrStorage

    final override fun getAttribute(qualifiedName: String): String? {
        return _attrStorage.getNamedItem(qualifiedName)?.getValue()
    }

    final override fun getAttributeNS(namespace: String?, localName: String): String? {
        return _attrStorage.getNamedItemNS(namespace, localName)?.getValue()
    }

    final override fun setAttribute(qualifiedName: String, value: String) {
        val attr = getOwnerDocument().createAttribute(qualifiedName)
        attr.setValue(value)
        _attrStorage.setNamedItem(attr)
    }

    final override fun setAttributeNS(namespace: String?, cName: String, value: String) {
        val attr = getOwnerDocument().createAttributeNS(namespace, cName)
        attr.setValue(value)
        _attrStorage.setNamedItem(attr)
    }

    final override fun removeAttribute(qualifiedName: String) {
        _attrStorage.removeNamedItem(qualifiedName)
    }

    final override fun removeAttributeNS(namespace: String?, localName: String) {
        _attrStorage.removeNamedItemNS(namespace, localName)
    }

    final override fun hasAttribute(qualifiedName: String): Boolean {
        return _attrStorage.getNamedItem(qualifiedName) != null
    }

    final override fun hasAttributeNS(namespace: String?, localName: String): Boolean {
        return _attrStorage.getNamedItemNS(namespace, localName) != null
    }

    final override fun getAttributeNode(qualifiedName: String): AbstractAttr<N, P>? {
        return _attrStorage.getNamedItem(qualifiedName)
    }

    final override fun getAttributeNodeNS(namespace: String?, localName: String): AbstractAttr<N, P>? {
        return _attrStorage.getNamedItemNS(namespace, localName)
    }

    final override fun setAttributeNode(attr: PlatformAttr): AbstractAttr<N, P>? {
        return _attrStorage.setNamedItem(attr)
    }

    final override fun setAttributeNodeNS(attr: PlatformAttr): AbstractAttr<N, P>? {
        return _attrStorage.setNamedItemNS(attr)
    }

    final override fun removeAttributeNode(attr: PlatformAttr): AbstractAttr<N, P> {
        return _attrStorage.removeAttr(attr)
    }

    override fun getElementsByTagName(qualifiedName: String): AbstractNodeList<N, P> {
        val matchAll = qualifiedName == "*"
        val elems = mutableListOf<N>()

        fun collect(p: AbstractElement<N, P>) {
            for (c in p.getChildNodes()) {
                if (c is AbstractElement<N, P>) {
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

    override fun getElementsByTagNameNS(namespace: String?, localName: String): NodeListImpl<N, P> {
        val _namespace = namespace ?: ""

        val matchAllNs = namespace == "*"
        val matchAllLocalname = localName == "*"
        val elems = mutableListOf<N>()

        fun collect(p: AbstractElement<N, P>) {
            for (it in p.getChildNodes()) {
                if (it is AbstractElement<N, P>) {
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


    override fun lookupPrefix(namespace: String): String? {
        when (namespace) {
            XMLConstants.XML_NS_URI -> return XMLConstants.XML_NS_PREFIX
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> return XMLConstants.XMLNS_ATTRIBUTE
        }

        val attr = getAttributes().asSequence()
            .firstOrNull {
                it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI && it.getValue() == namespace
            }

        return when (attr) {
            null -> getParentElement()?.lookupPrefix(namespace)
            else -> when (val l = attr.localName) {
                XMLConstants.XMLNS_ATTRIBUTE -> XMLConstants.DEFAULT_NS_PREFIX
                else -> l
            }
        }
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        when (prefix) {
            getPrefix() -> return getNamespaceURI()
            XMLConstants.XML_NS_PREFIX -> return XMLConstants.XML_NS_URI
            XMLConstants.XMLNS_ATTRIBUTE -> return XMLConstants.XMLNS_ATTRIBUTE_NS_URI
        }

        val nsAttrSeq = getAttributes().asSequence().filter { it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
        val attr = when {
            prefix.isBlank() -> nsAttrSeq.firstOrNull { it.localName == XMLConstants.XMLNS_ATTRIBUTE }
            else -> nsAttrSeq.firstOrNull { it.localName == prefix }
        }
        return when (attr) {
            null -> getParentElement()?.lookupNamespaceURI(prefix)
            else -> attr.value
        }
    }

    override fun cloneNode(deep: Boolean): AbstractElement<N, P> {
        val ownerDocument = getOwnerDocument()
        val e = when (val u = namespaceURI) {
            null, "" -> ownerDocument.createElement(localName)
            else -> ownerDocument.createElementNS(u, tagName)
        }
        for (a in attributes) when (val n = a.namespaceURI) {
            null, "" -> setAttribute(a.name, a.value)
            else -> setAttributeNS(n, a.name, a.value)
        }

        if (deep) for (c in getChildNodes()) e.appendChild(c.cloneNode(true))
        return e
    }
}
