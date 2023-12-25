/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.NodeList
import nl.adaptivity.xmlutil.dom2.NodeListIterator

@Suppress("DEPRECATION")
public interface INodeList : NodeList, nl.adaptivity.xmlutil.dom2.NodeList, Collection<Node> {
    @Deprecated("Use size", ReplaceWith("size"))
    public override fun getLength(): Int = size

    override fun item(index: Int): INode?

    override fun iterator(): Iterator<INode> {
        return NodeListIterator(this)
    }

    override fun contains(element: Node): Boolean {
        return asSequence().contains(element)
    }

    override fun containsAll(elements: Collection<Node>): Boolean {
        return elements.all { contains(it) } // inefficient
    }

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
    override fun isEmpty(): Boolean = size == 0
}
