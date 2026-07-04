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

internal abstract class WrappedJvmNode<N : PlatformNode>(delegate: N) : Node {
    @Suppress("UNCHECKED_CAST")
    val delegate: N = delegate.unWrap()

    override fun getOwnerDocument(): WrappedJvmDocument? = delegate.ownerDocument.wrap()

    override fun getParentElement(): WrappedJvmElement? {
        return (delegate.parentNode as PlatformElement?)?.wrap()
    }

    final override fun getParentNode(): WrappedJvmNode<*>? = delegate.parentNode?.wrap()

    override fun getFirstChild(): WrappedJvmNode<*>? = delegate.firstChild?.wrap()

    override fun getLastChild(): WrappedJvmNode<*>? = delegate.lastChild?.wrap()

    final override fun getPreviousSibling(): WrappedJvmNode<*>? = delegate.previousSibling?.wrap()

    final override fun getNextSibling(): WrappedJvmNode<*>? = delegate.nextSibling?.wrap()

    final override fun getNodeName(): String = delegate.nodeName
    final override fun getNodetype(): NodeType = NodeType(delegate.nodeType)

    final override fun getNodeType(): Short = delegate.nodeType

    final override fun getTextContent(): String? = delegate.textContent

    final override fun setTextContent(value: String?) {
        delegate.textContent = value
    }

    final override fun getChildNodes(): WrappingNodeList = WrappingNodeList(delegate.childNodes)

    override fun getNodeValue(): String? = delegate.nodeValue

    override fun setNodeValue(value: String?) {
        delegate.nodeValue = value
    }

    fun insertBefore(newChild: PlatformNode?, refChild: PlatformNode?): WrappedJvmNode<*> {
        return delegate.insertBefore(newChild?.unWrap(), refChild?.unWrap()).wrap()
    }

    final override fun hasChildNodes(): Boolean = delegate.hasChildNodes()

    override fun cloneNode(deep: Boolean): WrappedJvmNode<*> {
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

    final override fun isDefaultNamespace(namespaceURI: String): Boolean = delegate.isDefaultNamespace(namespaceURI)

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
    open fun appendChild(node: WrappedJvmNode<*>): WrappedJvmNode<*> {
        return delegate.appendChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): WrappedJvmNode<*> {
        return delegate.appendChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    fun replaceChild(newChild: WrappedJvmNode<*>, oldChild: WrappedJvmNode<*>): WrappedJvmNode<*> {
        return delegate.replaceChild(newChild.unWrap(), oldChild.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): WrappedJvmNode<*> {
        return delegate.replaceChild(newChild.unWrap(), oldChild.unWrap()).wrap()
    }

    @IgnorableReturnValue
    fun removeChild(node: WrappedJvmNode<*>): WrappedJvmNode<*> {
        return delegate.removeChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun removeChild(node: PlatformNode): WrappedJvmNode<*> {
        return delegate.removeChild(node.unWrap()).wrap()
    }

    @IgnorableReturnValue
    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Node? {
        return delegate.insertBefore(newChild.unWrap(), refChild?.unWrap())?.wrap()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WrappedJvmNode<*>

        return delegate === other.delegate
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun toString(): String = delegate.toString()

}

internal fun <T : PlatformNode> WrappedJvmNode<T>.unWrap(): T = delegate

internal fun <T : PlatformNode> T.unWrap(): T =
    @Suppress("UNCHECKED_CAST")
    when (this) {
        is WrappedJvmNode<*> -> delegate as T
        else -> this
    }

internal fun PlatformAttr.unWrap(): PlatformAttr = when (this) {
    is WrappedJvmAttr -> delegate
    else -> this
}

internal fun Attr.unWrap(): PlatformAttr = when (this) {
    is WrappedJvmAttr -> delegate
    else -> this
}

internal fun Node.unWrap(): PlatformNode = when (this) {
    is WrappedJvmNode<*> -> delegate
    else -> this.wrap() // has to be actually wrapped to "work"
}

internal fun PlatformNode.wrap(): WrappedJvmNode<*> = when (this) {
    is WrappedJvmNode<*> -> this
    is PlatformNotation -> WrappedJvmNotation(this)
    is PlatformAttr -> WrappedJvmAttr(this)
    is PlatformCDATASection -> WrappedJvmCDATASection(this)
    is PlatformComment -> WrappedJvmComment(this)
    is PlatformDocument -> WrappedJvmDocument(this)
    is PlatformDocumentFragment -> WrappedJvmDocumentFragment(this)
    is PlatformDocumentType -> WrappedJvmDocumentType(this)
    is PlatformElement -> WrappedJvmElement(this)
    is PlatformProcessingInstruction -> WrappedJvmProcessingInstruction(this)
    is Text -> WrappedJvmText(this)
    else -> error("Node type ${NodeType(nodeType)} not supported")
}

internal fun Node.wrap(): WrappedJvmNode<*> = when (this) {
    is WrappedJvmNode<*> -> this
    else -> error("Node type ${getNodetype()} not supported")
}

internal fun PlatformDocument.wrap(): WrappedJvmDocument = when (this) {
    is WrappedJvmDocument -> this
    else -> WrappedJvmDocument(this)
}

internal fun PlatformElement.wrap(): WrappedJvmElement = when (this) {
    is WrappedJvmElement -> this
    else -> WrappedJvmElement(this)
}

internal fun Text.wrap(): WrappedJvmText = when (this) {
    is WrappedJvmText -> this
    else -> WrappedJvmText(this)
}

internal fun PlatformDocumentType.wrap(): WrappedJvmDocumentType = when (this) {
    is WrappedJvmDocumentType -> this
    else -> WrappedJvmDocumentType(this)
}

internal fun PlatformAttr.wrap(): WrappedJvmAttr = when (this) {
    is WrappedJvmAttr -> this
    else -> WrappedJvmAttr(this)
}
