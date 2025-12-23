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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import org.w3c.dom.NodeList as DomNodeList

internal class WrappingNodeList(val delegate: DomNodeList) : INodeList {
    override val size: Int
        get() = delegate.length

    override fun item(index: Int): INode = delegate.item(index).wrap()
    override fun get(index: Int): INode = item(index)

    @Deprecated("Use size", replaceWith = ReplaceWith("size"))
    override fun getLength(): Int = delegate.length
}
