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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.PlatformNodeList
import nl.adaptivity.xmlutil.dom2.NodeListIterator

public interface INodeList : PlatformNodeList, Collection<PlatformNode> {
    // Deprecation retained to ensure getLength is still implemented. I
    @Deprecated("Use size", ReplaceWith("size"), DeprecationLevel.HIDDEN)
    public override fun getLength(): Int = size

    override fun item(index: Int): INode?

    override fun get(index: Int): INode? {
        return item(index)
    }

    override fun iterator(): Iterator<INode> {
        return NodeListIterator(this)
    }

    public override fun contains(element: PlatformNode): Boolean {
        return asSequence().contains(element)
    }

    public override fun containsAll(elements: Collection<PlatformNode>): Boolean {
        return elements.all { contains(it) } // inefficient
    }

    public override fun isEmpty(): Boolean = size == 0
}
