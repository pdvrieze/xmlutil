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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.PlatformNodeList
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.NodeList
import nl.adaptivity.xmlutil.dom2.NodeListIterator

internal class WrappingNodeList(val delegate: PlatformNodeList) : NodeList {
    val size: Int get() = delegate.length

    override fun iterator(): Iterator<Node> {
        return NodeListIterator(this)
    }

    override fun item(index: Int): WrappedJvmNode<*> = delegate.item(index).wrap()
    override fun get(index: Int): WrappedJvmNode<*> = item(index)

    @Deprecated("Use size", replaceWith = ReplaceWith("size"))
    override fun getLength(): Int = delegate.length
}
