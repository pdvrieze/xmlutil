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
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Node
import kotlin.jvm.JvmStatic

@ExperimentalXmlUtilApi
public abstract class AbstractParentNode<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>?,
    nodeStorage: (P) -> AbstractNodeStorage<N, P>,
    parentNode: P? = null,
) : AbstractNode<N, P>(ownerDocument, parentNode), IAbstractParentNode<N, P> {

    protected abstract val self: P

    @Suppress("UNCHECKED_CAST")
    private val nodeStorage: AbstractNodeStorage<N, P> = nodeStorage(this as P)

    override fun setOwnerDocument(ownerDocument: AbstractDocument<@UnsafeVariance N, @UnsafeVariance P>) {
        for (c in getChildNodes()) {
            c.setOwnerDocument(ownerDocument)
        }
        super.setOwnerDocument(ownerDocument)
    }

    override fun getChildNodes(): AbstractNodeList<N, P> = nodeStorage.getNodeList()

    override fun getFirstChild(): N? = nodeStorage.getFirstChild()

    override fun getLastChild(): N? = nodeStorage.getLastChild()

    override fun appendChild(node: PlatformNode): N = nodeStorage.appendChild(self, node)

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): N =
        nodeStorage.replaceChild(self, newChild, oldChild)

    override fun removeChild(node: PlatformNode): N = nodeStorage.removeChild(self, node)

    override public fun getSiblingBefore(ref: Node): N? = nodeStorage.getSiblingBefore(ref)

    public override fun getSiblingAfter(ref: Node): N? = nodeStorage.getSiblingAfter(ref)

    public companion object {
        @JvmStatic
        internal fun <N : IAbstractNode<N, P>, P : IAbstractParentNode<N, P>>
                IAbstractNode<N, P>.setOwnerDocument(ownerDocument: AbstractDocument<N, P>) {
            (this as AbstractNode<*, *>).setOwnerDocument(ownerDocument)
        }
    }
}

