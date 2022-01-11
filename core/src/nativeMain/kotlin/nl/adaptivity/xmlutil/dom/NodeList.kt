/*
 * Copyright (c) 2022.
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

package nl.adaptivity.xmlutil.dom

public actual interface NodeList : Iterable<Node> {
    public val length: Int
    public actual fun item(index: Int): Node?

    override fun iterator(): Iterator<Node> {
        return NodeListIterator(this)
    }

    private class NodeListIterator(private val nodeList: NodeList) : Iterator<Node> {
        private var pos: Int = 0

        override fun hasNext(): Boolean {
            return pos < nodeList.length
        }

        override fun next(): Node {
            return nodeList.item(pos++) ?: throw NoSuchElementException("No item found in the iterator")
        }

    }
}

public actual inline fun NodeList.getLength(): Int = length
