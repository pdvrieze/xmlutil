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

@file:Suppress("DEPRECATION", "EXTENSION_SHADOWED_BY_MEMBER")
package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.XmlUtilInternal

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.NamedNodeMap", "nl.adaptivity.xmlutil.dom2")
)
public expect interface PlatformNamedNodeMap

@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.item(index: Int): PlatformNode?
@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.getNamedItem(qualifiedName: String): PlatformNode?
@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.getNamedItemNS(namespace: String?, localName: String): PlatformNode?
@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.setNamedItem(attr: PlatformNode): PlatformNode?
@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.setNamedItemNS(attr: PlatformNode): PlatformNode?
@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.removeNamedItem(qualifiedName: String): PlatformNode?
@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.removeNamedItemNS(namespace: String?, localName: String): PlatformNode?

@XmlUtilInternal
public expect inline fun PlatformNamedNodeMap.getLength(): Int

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getLength()"))
public inline val PlatformNamedNodeMap.length: Int get() = getLength()

public operator fun PlatformNamedNodeMap.get(index: Int): PlatformAttr? = item(index)?.asAttr()

public operator fun PlatformNamedNodeMap.iterator(): Iterator<PlatformAttr> {
    return NamedNodeMapIterator(this)
}

private class NamedNodeMapIterator(private val map: PlatformNamedNodeMap) : Iterator<PlatformAttr> {

    private var pos = 0

    override fun hasNext(): Boolean = pos < map.getLength()

    override fun next(): PlatformAttr = map[pos++]!!.asAttr()
}
