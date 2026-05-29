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

package nl.adaptivity.xmlutil.dom

@JsName("NamedNodeMap")
public actual external interface PlatformNamedNodeMap {

    public fun item(index: Int): PlatformNode?
    public fun getNamedItem(qualifiedName: String): PlatformNode?
    public fun getNamedItemNS(namespace: String?, localName: String): PlatformNode?
    public fun setNamedItem(attr: PlatformNode): PlatformNode?
    public fun setNamedItemNS(attr: PlatformNode): PlatformNode?
    public fun removeNamedItem(qualifiedName: String): PlatformNode?
    public fun removeNamedItemNS(namespace: String?, localName: String): PlatformNode?

}

public actual val PlatformNamedNodeMap.length: Int get() = asDynamic().length
public actual fun PlatformNamedNodeMap.getNamedItemNS(namespace: String?, localName: String): PlatformNode? {
    return getNamedItemNS(namespace, localName)
}

public actual operator fun PlatformNamedNodeMap.iterator(): Iterator<PlatformNode> =
    IteratorImpl(this)

private class IteratorImpl<N: PlatformNode>(private val map: PlatformNamedNodeMap): Iterator<N> {
    var idx = 0

    override fun hasNext(): Boolean = idx < map.length

    override fun next(): N {
        @Suppress("UNCHECKED_CAST")
        return map.item(idx++) as N
    }
}
