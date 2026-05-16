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
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.isXmlWhitespace
import kotlin.jvm.JvmStatic

@ExperimentalXmlUtilApi
public abstract class AbstractParentNode<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>?,
    nodeStorage: (P) -> MutableAbstractNodeStorage<N, P>,
    parentNode: P? = null,
) : AbstractNode<N, P>(ownerDocument, parentNode), IAbstractParentNode<N, P> {

    protected abstract val self: P

    @Suppress("UNCHECKED_CAST")
    private val _nodeStorage: MutableAbstractNodeStorage<N, P> = nodeStorage(this as P)
    protected val nodeStorage: AbstractNodeStorage<N, P> get() = _nodeStorage

    override fun setOwnerDocument(ownerDocument: AbstractDocument<@UnsafeVariance N, @UnsafeVariance P>) {
        for (c in getChildNodes()) {
            c.setOwnerDocument(ownerDocument)
        }
        super.setOwnerDocument(ownerDocument)
    }

    final override fun getChildNodes(): AbstractNodeList<N, P> = nodeStorage.getNodeList()

    final override fun getFirstChild(): N? = nodeStorage.getFirstChild()

    final override fun getLastChild(): N? = nodeStorage.getLastChild()

    final override fun appendChild(node: PlatformNode): N {
        return _nodeStorage.appendChild(self, node)
    }

    final override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): N =
        _nodeStorage.replaceChild(self, newChild, oldChild)

    final override fun removeChild(node: PlatformNode): N = _nodeStorage.removeChild(self, node)

    public final override fun getSiblingBefore(ref: Node): N? = nodeStorage.getSiblingBefore(ref)

    public final override fun getSiblingAfter(ref: Node): N? = nodeStorage.getSiblingAfter(ref)

    private fun getTextContent(receiver: StringBuilder, node: IAbstractNode<N, P>) {
        when (node) {
            is AbstractComment<*, *> -> {}
            is AbstractCharacterData<N, P> -> if (!isXmlWhitespace(node.getTextContent())) {
                receiver.append(node.getTextContent())
            }

            is AbstractDocumentFragment<*, *>,
            is AbstractElement<N, *> -> {
                for (c in getChildNodes()) {
                    getTextContent(receiver, c)
                }
            }
        }
    }

    protected fun getTextContentImpl(): String = buildString {
        getTextContent(this, this@AbstractParentNode)
    }

    override fun setTextContent(value: String?) {
        _nodeStorage.clear()
        if (! value.isNullOrEmpty()) {
            val doc = checkNotNull(getOwnerDocument() ?: this as? AbstractDocument<N, P>)
            appendChild((doc.createTextNode(value)))
        }
    }

    abstract override fun cloneNode(deep: Boolean): AbstractParentNode<N, P>

    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Node? {
        if (refChild == null) return appendChild(newChild)

        @Suppress("UNCHECKED_CAST")
        return _nodeStorage.insertBefore(newChild, refChild)
    }

    override fun normalize() {
        var prev: N? = null

        val it = _nodeStorage.iterator()
        for (n in it) {
            n.normalize()
            if (n.getNodetype() == NodeType.TEXT_NODE) {
                if (prev != null) {
                    prev.setTextContent(prev.getTextContent() + n.getTextContent())
                    it.remove()
                } else {
                    prev = n
                }
            }
        }
    }

    public companion object {
        @JvmStatic
        internal fun <N : IAbstractNode<N, P>, P : IAbstractParentNode<N, P>>
                IAbstractNode<N, P>.setOwnerDocument(ownerDocument: AbstractDocument<N, P>) {
            (this as AbstractNode<*, *>).setOwnerDocument(ownerDocument)
        }
    }
}

