/*
 * Copyright (c) 2024.
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
    ReplaceWith("nl.adaptivity.xmlutil.dom2.NamedNodeMap", "nl.adaptivity.xmlutil.dom2")
)
public expect interface NamedNodeMap {
    public fun item(index: Int): Node?

    public fun getNamedItem(qualifiedName: String): Node?

    public fun getNamedItemNS(namespace: String?, localName: String): Node?

    public fun setNamedItem(attr: Node): Node?

    public fun setNamedItemNS(attr: Node): Node?

    public fun removeNamedItem(qualifiedName: String): Node?

    public fun removeNamedItemNS(namespace: String?, localName: String): Node?
}

public expect inline fun NamedNodeMap.getLength(): Int

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getLength()"))
public inline val NamedNodeMap.length: Int get() = getLength()

public operator fun NamedNodeMap.get(index: Int): Attr? = item(index)?.asAttr()

public operator fun NamedNodeMap.iterator(): Iterator<Attr> {
    return NamedNodeMapIterator(this)
}

private class NamedNodeMapIterator(private val map: NamedNodeMap) : Iterator<Attr> {

    private var pos = 0

    override fun hasNext(): Boolean = pos < map.getLength()

    override fun next(): Attr = map[pos++]!!.asAttr()
}
