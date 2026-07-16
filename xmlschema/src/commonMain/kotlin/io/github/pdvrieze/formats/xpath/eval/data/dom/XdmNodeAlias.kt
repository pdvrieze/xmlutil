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

package io.github.pdvrieze.formats.xpath.eval.data.dom

import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeFriend
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.impl.AbstractElement
import nl.adaptivity.xmlutil.dom2.impl.AbstractNodeList

@OptIn(XPathInternal::class)
internal class XdmNodeAlias<T: XdmNode<T>>(
    val base: XdmNode<T>,
    override val dynamicType: XdmSingleType
) : XdmNode<T> {
    init {
        require(base !is XdmNodeAlias<*>) { "Cannot alias an alias" }
    }

    override val staticType: XdmSingleType get() = base.staticType

    override fun asT(): T = base.asT()

    @OptIn(XdmNodeFriend::class)
    override var posInParent: Int
        get() = base.posInParent
        set(value) { base.posInParent = value }

    override fun getOwnerDocument(): XdmDocument? = base.getOwnerDocument()

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument) {
        base.setOwnerDocument(ownerDocument)
    }

    override fun getNodetype(): NodeType = base.getNodetype()

    override fun getNodeName(): String = getNodeName()

    @ExperimentalXmlUtilApi
    override fun getNodeValue(): String? = base.getNodeValue()

    @ExperimentalXmlUtilApi
    override fun setNodeValue(value: String?) {
        base.setNodeValue(value)
    }

    @ExperimentalXmlUtilApi
    override fun getAttributes(): NamedNodeMap<Attr>? = base.getAttributes()

    @ExperimentalXmlUtilApi
    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Node? {
        return base.insertBefore(newChild, refChild)
    }

    @ExperimentalXmlUtilApi
    override fun hasChildNodes(): Boolean = base.hasChildNodes()

    override fun hasAttributes(): Boolean = base.hasAttributes()

    override fun getNamespaceURI(): String? = base.getNamespaceURI()

    override fun getPrefix(): String? = base.getPrefix()

    override fun getLocalName(): String? = base.getLocalName()

    @ExperimentalXmlUtilApi
    override fun normalize() {
        base.normalize()
    }

    @ExperimentalXmlUtilApi
    override fun isSameNode(other: PlatformNode): Boolean {
        return base.isSameNode(other)
    }

    @ExperimentalXmlUtilApi
    override fun isEqualNode(other: PlatformNode): Boolean {
        return base.isEqualNode(other)
    }

    @ExperimentalXmlUtilApi
    override fun getBaseURI(): String? = base.getBaseURI()

    override fun getTextContent(): String? = base.getTextContent()

    override fun setTextContent(value: String?) = base.setTextContent(value)

    @ExperimentalXmlUtilApi
    override fun lookupPrefix(namespace: String): String? = base.lookupPrefix(namespace)

    @ExperimentalXmlUtilApi
    override fun lookupNamespaceURI(prefix: String): String? {
        return lookupNamespaceURI(prefix)
    }

    @ExperimentalXmlUtilApi
    override fun isDefaultNamespace(namespaceURI: String): Boolean {
        return base.isDefaultNamespace(namespaceURI)
    }

    override fun getParentNode(): XdmParentNode<*>? = base.getParentNode()

    override fun getChildNodes(): AbstractNodeList<XdmNode<*>, XdmParentNode<*>> {
        return base.getChildNodes()
    }

    override fun getFirstChild(): XdmNode<*>? {
        // TODO add type casting (if needed)
        return base.getFirstChild()
    }

    override fun getLastChild(): XdmNode<*>? {
        // TODO add type casting (if needed)
        return base.getLastChild()
    }

    override fun getPreviousSibling(): XdmNode<*>? = base.getPreviousSibling()

    override fun getNextSibling(): XdmNode<*>? = base.getNextSibling()

    override fun getParentElement(): AbstractElement<XdmNode<*>, XdmParentNode<*>>? {
        return base.getParentElement()
    }

    override fun replaceChild(
        newChild: PlatformNode,
        oldChild: PlatformNode
    ): XdmNode<*> {
        // TODO add type casting (if needed)
        return base.replaceChild(newChild, oldChild)
    }

    override fun appendChild(node: PlatformNode): XdmNode<*> {
        // TODO add type casting (if needed)
        return base.appendChild(node)
    }

    override fun removeChild(node: PlatformNode): XdmNode<*> {
        return base.removeChild(node)
    }

    override fun cloneNode(deep: Boolean): XdmNodeAlias<T> {
        return XdmNodeAlias(base.cloneNode(deep), dynamicType)
    }

    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmNodeAlias<T> {
        return base.treatAsNonEmpty(type)
    }
}
