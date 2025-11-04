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

@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.XmlUtilInternal

public actual external interface PlatformNamedNodeMap {

    public fun item(index: Int): PlatformNode?
    public fun getNamedItem(qualifiedName: String): PlatformNode?
    public fun getNamedItemNS(namespace: String?, localName: String): PlatformNode?
    public fun setNamedItem(attr: PlatformNode): PlatformNode?
    public fun setNamedItemNS(attr: PlatformNode): PlatformNode?
    public fun removeNamedItem(qualifiedName: String): PlatformNode?
    public fun removeNamedItemNS(namespace: String?, localName: String): PlatformNode?

}

@Suppress("NOTHING_TO_INLINE")
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.getLength(): Int = asDynamic().length as Int

@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.item(index: Int): PlatformNode? = item(index)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.getNamedItem(qualifiedName: String): PlatformNode? = getNamedItem(qualifiedName)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.getNamedItemNS(namespace: String?, localName: String): PlatformNode? = getNamedItemNS(namespace, localName)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.setNamedItem(attr: PlatformNode): PlatformNode? = setNamedItem(attr)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.setNamedItemNS(attr: PlatformNode): PlatformNode? = setNamedItemNS(attr)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.removeNamedItem(qualifiedName: String): PlatformNode? = removeNamedItem(qualifiedName)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.removeNamedItemNS(namespace: String?, localName: String): PlatformNode? = removeNamedItemNS(namespace, localName)

