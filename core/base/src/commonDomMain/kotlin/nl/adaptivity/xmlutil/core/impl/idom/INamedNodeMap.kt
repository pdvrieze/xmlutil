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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNamedNodeMap

public interface INamedNodeMap : PlatformNamedNodeMap, Collection<PlatformAttr> {

    override fun item(index: Int): IAttr?

    override fun getLength(): Int = size

    public override operator fun get(index: Int): IAttr? = item((index))

    override fun getNamedItem(qualifiedName: String): IAttr?

    override fun getNamedItemNS(namespace: String?, localName: String): IAttr?

    override fun setNamedItem(attr: PlatformAttr): IAttr?

    override fun setNamedItemNS(attr: PlatformAttr): IAttr?

    override fun removeNamedItem(qualifiedName: String): IAttr?

    override fun removeNamedItemNS(namespace: String?, localName: String): IAttr?

    override fun iterator(): Iterator<IAttr>

    override fun contains(element: PlatformAttr): Boolean {
        return asSequence().contains(element)
    }

    override fun containsAll(elements: Collection<PlatformAttr>): Boolean {
        return elements.all { contains(it) } // This is far from optimized
    }

    override fun isEmpty(): Boolean = size == 0
}
