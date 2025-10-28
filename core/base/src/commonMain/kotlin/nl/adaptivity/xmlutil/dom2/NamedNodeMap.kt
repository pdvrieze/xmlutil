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

package nl.adaptivity.xmlutil.dom2

public interface NamedNodeMap : Iterable<Attr> {

    /**
     * The size function works with collection interfaces rather than the traditional getLength interface.
     */
    public val size: Int

    @Deprecated("Use size instead", ReplaceWith("size"), level = DeprecationLevel.WARNING)
    public fun getLength(): Int = size

    public fun item(index: Int): Attr?

    public operator fun get(index: Int): Attr? = item((index))

    public fun getNamedItem(qualifiedName: String): Attr?

    public fun getNamedItemNS(namespace: String?, localName: String): Attr?

    public fun setNamedItem(attr: Attr): Attr?

    public fun setNamedItemNS(attr: Attr): Attr?

    public fun removeNamedItem(qualifiedName: String): Attr?

    public fun removeNamedItemNS(namespace: String?, localName: String): Attr?

    public override operator fun iterator(): Iterator<Attr> {
        return NamedNodeMapIterator(this)
    }

}

private class NamedNodeMapIterator(private val map: NamedNodeMap) : Iterator<Attr> {

    private var pos = 0

    override fun hasNext(): Boolean = pos < map.getLength()

    override fun next(): Attr = map.get(pos++) ?: throw NoSuchElementException("Iterating beyond node map")
}
