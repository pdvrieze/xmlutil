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

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.DocumentFragment

@XmlUtilInternal
public interface MutableAbstractNodeStorage<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>> :
    AbstractNodeStorage<N, P> {

    override fun iterator(): MutableIterator<N>

    public fun appendChild(parent: @UnsafeVariance P, node: PlatformNode): N = checkTypeAndOwner(node).also {
        when (it) {
            is DocumentFragment -> for (child in it.getChildNodes()) {
                val _ = appendChild(parent, child)
            }

            else -> appendChild(parent, it)
        }
    }

    public fun appendChild(parent: @UnsafeVariance P, node: @UnsafeVariance N)

    public fun removeChild(parent: @UnsafeVariance P, node: PlatformNode): N = removeChild(parent, checkTypeAndOwner(
        node
    ))

    public fun removeChild(parent: @UnsafeVariance P, node: @UnsafeVariance N): N

    public fun replaceChild(parent: @UnsafeVariance P, newChild: PlatformNode, oldChild: PlatformNode): N {
        return replaceChild(parent, checkTypeAndOwner(newChild), checkTypeAndOwner(oldChild))
    }

    public fun replaceChild(parent: @UnsafeVariance P, newChild: @UnsafeVariance N, oldChild: @UnsafeVariance N): N

    public fun insertBefore(newChild: @UnsafeVariance N, refChild: @UnsafeVariance N)
    public fun insertBefore(newChild: PlatformNode, refChild: PlatformNode): N {
        return checkTypeAndOwner(newChild).also { new ->
            insertBefore(new, checkTypeAndOwner(refChild))
        }
    }

    public fun clear()
}
