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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2

public interface INamedNodeMap : NamedNodeMap, nl.adaptivity.xmlutil.dom2.NamedNodeMap, Collection<Attr> {
    @Deprecated("Use size instead", ReplaceWith("size"))
    public override fun getLength(): Int = size

    override val size: Int

    override fun item(index: Int): IAttr?

    public override operator fun get(index: Int): IAttr? = item((index))

    override fun getNamedItem(qualifiedName: String): IAttr?

    override fun getNamedItemNS(namespace: String?, localName: String): IAttr?

    override fun setNamedItem(attr: Node1): IAttr?

    override fun setNamedItem(attr: Attr2): IAttr?

    override fun setNamedItemNS(attr: Node1): IAttr?

    override fun setNamedItemNS(attr: Attr2): IAttr?

    override fun removeNamedItem(qualifiedName: String): IAttr?

    override fun removeNamedItemNS(namespace: String?, localName: String): IAttr?

    override fun iterator(): Iterator<IAttr>

    override fun contains(element: Attr): Boolean {
        return asSequence().contains(element)
    }

    override fun containsAll(elements: Collection<Attr>): Boolean {
        return elements.all { contains(it) } // This is far from optimized
    }

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
    override fun isEmpty(): Boolean {
        return size == 0
    }
}
