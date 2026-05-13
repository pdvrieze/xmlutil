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
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.length

@ExperimentalXmlUtilApi
public abstract class AbstractNode<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>
internal constructor(
    private var _ownerDocument: AbstractDocument<N, P>?,
    private var _parentNode: P? = null
) : IAbstractNode<N, P> {

    override fun getParentNode(): P? = _parentNode

    @XmlUtilInternal
    internal fun setParentNode(parentNode: @UnsafeVariance P?) {
        _parentNode = parentNode
    }

    override fun getOwnerDocument(): AbstractDocument<N, P>? {
        return _ownerDocument
    }

    internal open fun setOwnerDocument(ownerDocument: AbstractDocument<@UnsafeVariance N, @UnsafeVariance P>) {
        _ownerDocument = ownerDocument
    }

    override fun getParentElement(): AbstractElement<N, P>? {
        @Suppress("UNCHECKED_CAST")
        return _parentNode as? AbstractElement<N, P>
    }


    override fun getPreviousSibling(): N? {
        @Suppress("UNCHECKED_CAST")
        return (_parentNode ?: return null).getSiblingBefore(this as N)
    }

    override fun getNextSibling(): N? {
        return getParentNode()?.getSiblingAfter(this)
    }

    public override fun getAttributes(): NamedNodeMap? = null

    override fun hasChildNodes(): Boolean = getChildNodes().length > 0

    abstract override fun getFirstChild(): N?

    abstract override fun getLastChild(): N?

    abstract override fun appendChild(node: PlatformNode): N

    abstract override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): N

    abstract override fun removeChild(node: PlatformNode): N

    abstract override fun cloneNode(deep: Boolean): AbstractNode<N, P>

    /**
     * By default compare by identity.
     */
    override fun isSameNode(other: PlatformNode): Boolean {
        return this === other
    }

    override fun normalize() {
        // Do nothing
    }
}
