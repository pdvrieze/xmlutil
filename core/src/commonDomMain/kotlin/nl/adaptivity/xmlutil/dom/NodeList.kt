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

@Deprecated(
    "Use DOMNodeList that contains extended functions",
    ReplaceWith("DOMNodeList", "nl.adaptivity.xmlutil.dom.DOMNodeList")
)
public actual interface NodeList {
    public actual fun item(index: Int): Node?
}

@Suppress("DEPRECATION")
public interface DOMNodeList : NodeList, Collection<Node> {
    @Deprecated("Use size", ReplaceWith("size"))
    public val length: Int get() = size

    override fun iterator(): Iterator<Node> {
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

@Suppress("NOTHING_TO_INLINE", "DEPRECATION")
public actual inline fun NodeList.getLength(): Int = (this as DOMNodeList).size
