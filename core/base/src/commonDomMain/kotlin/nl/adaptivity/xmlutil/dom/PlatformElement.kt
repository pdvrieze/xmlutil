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

import nl.adaptivity.xmlutil.core.impl.idom.IAttr
import nl.adaptivity.xmlutil.core.impl.idom.INamedNodeMap
import nl.adaptivity.xmlutil.core.impl.idom.INodeList

@Suppress("DEPRECATION")
public actual interface PlatformElement : PlatformNode {
    public val namespaceURI: String?
    public val prefix: String?
    public val localName: String
    public val tagName: String

    public val attributes: INamedNodeMap
    public fun getAttribute(qualifiedName: String): String?
    public fun getAttributeNS(namespace: String?, localName: String): String?

    public fun setAttribute(qualifiedName: String, value: String)
    public fun setAttributeNS(namespace: String?, cName: String, value: String)

    public fun removeAttribute(qualifiedName: String)
    public fun removeAttributeNS(namespace: String?, localName: String)

    public fun hasAttribute(qualifiedName: String): Boolean
    public fun hasAttributeNS(namespace: String?, localName: String): Boolean

    public fun getAttributeNode(qualifiedName: String): IAttr?
    public fun getAttributeNodeNS(namespace: String?, localName: String): IAttr?

    public fun setAttributeNode(attr: PlatformAttr): IAttr?
    public fun setAttributeNodeNS(attr: PlatformAttr): IAttr?
    public fun removeAttributeNode(attr: PlatformAttr): IAttr

    public fun getElementsByTagName(qualifiedName: String): INodeList
    public fun getElementsByTagNameNS(namespace: String?, localName: String): INodeList
}

