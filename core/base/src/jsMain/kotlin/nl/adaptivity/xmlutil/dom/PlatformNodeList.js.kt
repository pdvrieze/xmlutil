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

import nl.adaptivity.xmlutil.dom2.NodeList
import org.w3c.dom.NodeList as DomNodeList

@JsName("NodeList")
public actual external interface PlatformNodeList {
/*
    public val length: Int
*/
    public fun item(index: Int): PlatformNode?
}

public actual val PlatformNodeList.length: Int get() = when (this) {
    is NodeList -> getLength()
    else -> unsafeCast<DomNodeList>().length
}

private class NodeListIterator(private val list: PlatformNodeList) : Iterator<PlatformNode> {
    private var index = 0
    override fun hasNext(): Boolean = index < list.length
    override fun next(): PlatformNode = list.item(index++)!!
}

public actual operator fun PlatformNodeList.iterator(): Iterator<PlatformNode> = when (this) {
    is NodeList -> iterator()
    else -> NodeListIterator(this)
}
