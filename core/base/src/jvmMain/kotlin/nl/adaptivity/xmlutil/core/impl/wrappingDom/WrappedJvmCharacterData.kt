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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformCharacterData
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.CharacterData

internal abstract class WrappedJvmCharacterData<N : PlatformCharacterData>(delegate: N) : WrappedJvmNode<N>(delegate),
    CharacterData {
    override fun getData(): String = delegate.data

    final override fun setData(data: String) {
        delegate.data = data
    }

    override fun getOwnerDocument(): WrappedJvmDocument {
        return checkNotNull(super.getOwnerDocument())
    }

    override fun getNodeValue(): String = delegate.nodeValue
    override fun setNodeValue(value: String?) {
        data = value ?: ""
    }

    override fun getLength(): Int = delegate.length

    override fun normalize() {
        delegate.normalize()
    }

    override fun substringData(offset: Int, count: Int): String =
        delegate.substringData(offset, count)

    override fun appendData(data: String) {
        delegate.appendData(data)
    }

    override fun insertData(offset: Int, data: String) {
        delegate.insertData(offset, data)
    }

    override fun deleteData(offset: Int, count: Int) {
        delegate.deleteData(offset, count)
    }

    override fun replaceData(offset: Int, count: Int, data: String) {
        delegate.replaceData(offset, count, data)
    }

    override fun getFirstChild(): Nothing? = null

    override fun getLastChild(): Nothing? = null

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character data")

    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character data")

    @IgnorableReturnValue
    override fun appendChild(node: WrappedJvmNode<*>): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character data")

    @IgnorableReturnValue
    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character data")

    @IgnorableReturnValue
    override fun removeChild(node: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character data")

    abstract override fun cloneNode(deep: Boolean): WrappedJvmCharacterData<N>
}
