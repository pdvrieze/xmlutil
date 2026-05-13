/*
 * Copyright (c) 2024-2026.
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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.NodeType
import org.w3c.dom.Text
import org.w3c.dom.UserDataHandler

internal abstract class AbstractNodeImpl<N : PlatformNode>(delegate: N) : Node {
    @Suppress("UNCHECKED_CAST")
    val delegate: N = delegate.unWrap()

    override fun getOwnerDocument(): DocumentImpl? = delegate.ownerDocument.wrap()

    override fun getParentElement(): ElementImpl? {
        return (delegate.parentNode as PlatformElement?)?.wrap()
    }

    final override fun getParentNode(): AbstractNodeImpl<*>? = delegate.parentNode?.wrap()

    override fun getFirstChild(): AbstractNodeImpl<*>? = delegate.firstChild?.wrap()

    override fun getLastChild(): AbstractNodeImpl<*>? = delegate.lastChild?.wrap()

    final override fun getPreviousSibling(): AbstractNodeImpl<*>? = delegate.previousSibling?.wrap()

    final override fun getNextSibling(): AbstractNodeImpl<*>? = delegate.nextSibling?.wrap()

    final override fun getNodeName(): String = delegate.nodeName
    final override fun getNodetype(): NodeType = NodeType(delegate.nodeType)

    final override fun getNodeType(): Short = delegate.nodeType

    final override fun getTextContent(): String? = delegate.textContent

    final override fun setTextContent(value: String?) {
        delegate.textContent = value
    }

    final override fun getChildNodes(): WrappingNodeList = WrappingNodeList(delegate.childNodes)

    override fun getNodeValue(): String? = delegate.nodeValue

    override fun setNodeValue(nodeValue: String?) {
        delegate.nodeValue = nodeValue
    }

    fun insertBefore(newChild: PlatformNode?, refChild: PlatformNode?): AbstractNodeImpl<*> {
        return delegate.insertBefore(newChild?.unWrap(), refChild?.unWrap()).wrap()
    }

    final override fun hasChildNodes(): Boolean = delegate.hasChildNodes()

    override fun cloneNode(deep: Boolean): AbstractNodeImpl<*> {
        return delegate.cloneNode(deep).wrap()
    }

    @Deprecated("No-op for now")
    override fun normalize() {
        delegate.normalize()
    }

    final override fun isSupported(feature: String?, version: String?): Boolean {
        return delegate.isSupported(feature, version)
    }

    final override fun getNamespaceURI(): String? = delegate.namespaceURI

    final override fun getPrefix(): String? = delegate.prefix

    final override fun setPrefix(prefix: String?) {
        delegate.prefix = prefix
    }

    override fun getLocalName(): String? = delegate.localName

    override fun hasAttributes(): Boolean = delegate.hasAttributes()

    override fun getBaseURI(): String = delegate.baseURI

    final override fun compareDocumentPosition(other: PlatformNode): Short {
        return delegate.compareDocumentPosition(other.unWrap())
    }

    final override fun isSameNode(other: PlatformNode): Boolean = delegate.isSameNode(other.unWrap())

    final override fun lookupPrefix(namespace: String): String? = delegate.lookupPrefix(namespace)

    final override fun isDefaultNamespace(namespaceURI: String?): Boolean = delegate.isDefaultNamespace(namespaceURI)

    final override fun lookupNamespaceURI(prefix: String): String? = delegate.lookupNamespaceURI(prefix)

    final override fun isEqualNode(other: PlatformNode): Boolean {
        return delegate.isEqualNode(other.unWrap())
    }

    final override fun getFeature(feature: String?, version: String?): Any? {
        return delegate.getFeature(feature, version)
    }

    final override fun setUserData(key: String, data: Any?, handler: UserDataHandler?): Any? {
        return delegate.setUserData(key, data, handler)
    }

    final override fun getUserData(key: String): Any? {
        return delegate.getUserData(key)
    }

    @IgnorableReturnValue
    open fun appendChild(node: AbstractNodeImpl<*>): AbstractNodeImpl<*> {
        return delegate.appendChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): AbstractNodeImpl<*> {
        return delegate.appendChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    fun replaceChild(newChild: AbstractNodeImpl<*>, oldChild: AbstractNodeImpl<*>): AbstractNodeImpl<*> {
        return delegate.replaceChild(newChild.unWrap(), oldChild.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): AbstractNodeImpl<*> {
        return delegate.replaceChild(newChild.unWrap(), oldChild.unWrap()).wrap()
    }

    @IgnorableReturnValue
    fun removeChild(node: AbstractNodeImpl<*>): AbstractNodeImpl<*> {
        return delegate.removeChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun removeChild(node: PlatformNode): AbstractNodeImpl<*> {
        return delegate.removeChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Node? {
        return delegate.insertBefore(newChild.unWrap(), refChild?.unWrap())?.wrap()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractNodeImpl<*>

        return delegate === other.delegate
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun toString(): String = delegate.toString()

}

internal fun <T : PlatformNode> AbstractNodeImpl<T>.unWrap(): T = delegate

internal fun <T : PlatformNode> T.unWrap(): T =
    @Suppress("UNCHECKED_CAST")
    when (this) {
        is AbstractNodeImpl<*> -> delegate as T
        else -> this
    }

internal fun PlatformAttr.unWrap(): PlatformAttr = when (this) {
    is AttrImpl -> delegate
    else -> this
}

internal fun Attr.unWrap(): PlatformAttr = when (this) {
    is AttrImpl -> delegate
    else -> this
}

internal fun Node.unWrap(): PlatformNode = when (this) {
    is AbstractNodeImpl<*> -> delegate
    else -> this.wrap() // has to be actually wrapped to "work"
}

internal fun PlatformNode.wrap(): AbstractNodeImpl<*> = when (this) {
    is AbstractNodeImpl<*> -> this
    is PlatformAttr -> AttrImpl(this)
    is PlatformCDATASection -> CDATASectionImpl(this)
    is PlatformComment -> CommentImpl(this)
    is PlatformDocument -> DocumentImpl(this)
    is PlatformDocumentFragment -> DocumentFragmentImpl(this)
    is PlatformDocumentType -> DocumentTypeImpl(this)
    is PlatformElement -> ElementImpl(this)
    is PlatformProcessingInstruction -> ProcessingInstructionImpl(this)
    is Text -> TextImpl(this)
    else -> error("Node type ${NodeType(nodeType)} not supported")
}

internal fun Node.wrap(): AbstractNodeImpl<*> = when (this) {
    is AbstractNodeImpl<*> -> this
    else -> error("Node type ${getNodetype()} not supported")
}

internal fun PlatformDocument.wrap(): DocumentImpl = when (this) {
    is DocumentImpl -> this
    else -> DocumentImpl(this)
}

internal fun PlatformElement.wrap(): ElementImpl = when (this) {
    is ElementImpl -> this
    else -> ElementImpl(this)
}

internal fun Text.wrap(): TextImpl = when (this) {
    is TextImpl -> this
    else -> TextImpl(this)
}

internal fun PlatformDocumentType.wrap(): DocumentTypeImpl = when (this) {
    is DocumentTypeImpl -> this
    else -> DocumentTypeImpl(this)
}

internal fun PlatformAttr.wrap(): AttrImpl = when (this) {
    is AttrImpl -> this
    else -> AttrImpl(this)
}
