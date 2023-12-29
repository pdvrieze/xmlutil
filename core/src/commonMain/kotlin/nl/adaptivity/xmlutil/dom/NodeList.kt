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

@file:Suppress("DEPRECATION", "EXTENSION_SHADOWED_BY_MEMBER")

package nl.adaptivity.xmlutil.dom

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.NodeList", "nl.adaptivity.xmlutil.dom2")
)
public expect interface NodeList {
    public fun item(index: Int): Node?

}

public expect inline fun NodeList.getLength(): Int

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getLength()"))
public inline val NodeList.length: Int get() = getLength()

public operator fun NodeList.get(index: Int): Node? = item(index)

public operator fun NodeList.iterator(): Iterator<Node> {
    return NodeListIterator(this)
}

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.NodeListIterator", "nl.adaptivity.xmlutil.dom2")
)
internal class NodeListIterator(private val nodeList: NodeList) : Iterator<Node> {
    private var pos: Int = 0

    override fun hasNext(): Boolean {
        return pos < nodeList.getLength()
    }

    override fun next(): Node {
        return nodeList.item(pos++) ?: throw NoSuchElementException("No item found in the iterator")
    }

}

