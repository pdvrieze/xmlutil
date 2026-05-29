/*
 * Copyright (c) 2026.
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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformNode

@ExperimentalXmlUtilApi
public object EmptyNamedNodeMap: NamedNodeMap<Nothing> {
    override fun getLength(): Int = 0
    override fun get(index: Int): Nothing? = null
    override val size: Int get() = 0
    override fun item(index: Int): Nothing? = null
    override fun getNamedItem(qualifiedName: String): Nothing? = null
    override fun getNamedItemNS(namespace: String?, localName: String): Nothing? = null
    override fun setNamedItem(attr: PlatformNode): Nothing? = null
    override fun setNamedItemNS(attr: PlatformNode): Nothing? = null
    override fun removeNamedItem(qualifiedName: String): Nothing? = null
    override fun removeNamedItemNS(namespace: String?, localName: String): Nothing? = null
    override fun iterator(): Iterator<Nothing> = emptyList<Nothing>().iterator()
}
