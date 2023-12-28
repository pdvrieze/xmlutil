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

package nl.adaptivity.xmlutil.dom2

public interface NodeList : Iterable<Node> {
    public fun getLength(): Int

    public fun item(index: Int): Node?

    public operator fun get(index: Int): Node? = item(index)

    public override operator fun iterator(): Iterator<Node> {
        return NodeListIterator(this)
    }
}

public inline val NodeList.length: Int get() = getLength()

internal class NodeListIterator<L : NodeList, N : Node>(private val nodeList: L) : Iterator<N> {
    private var pos: Int = 0

    override fun hasNext(): Boolean {
        return pos < nodeList.getLength()
    }

    override fun next(): N {
        @Suppress("UNCHECKED_CAST")
        return (nodeList.item(pos++) as? N) ?: throw NoSuchElementException("No item found in the iterator")
    }

}

