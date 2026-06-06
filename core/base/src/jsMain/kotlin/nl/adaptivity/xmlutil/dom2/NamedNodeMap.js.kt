/*
 * Copyright (c) 2025-2026.
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

import nl.adaptivity.xmlutil.dom.PlatformNamedNodeMap
import nl.adaptivity.xmlutil.dom.PlatformNode

public actual interface NamedNodeMap<out T: Node> : Iterable<T>, PlatformNamedNodeMap {
    public actual val size: Int

    public actual fun getLength(): Int
    public actual override fun item(index: Int): T?
    public actual operator fun get(index: Int): T?
    public actual override fun getNamedItem(qualifiedName: String): T?
    public actual override fun getNamedItemNS(namespace: String?, localName: String): T?

    actual override fun setNamedItem(attr: PlatformNode): T?// = setNamedItem(attr.unWrap() as Attr)

    actual override fun setNamedItemNS(attr: PlatformNode): T?

    public actual override fun removeNamedItem(qualifiedName: String): T?
    public actual override fun removeNamedItemNS(namespace: String?, localName: String): T?
    public actual override operator fun iterator(): Iterator<T>
}

internal fun addNamedNodeMapPropertiesToPrototype(prototype: dynamic) {
    js("Object").defineProperty(prototype, "length", jsProperty<NamedNodeMap<*>> { getLength() })
}
