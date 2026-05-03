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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.CharacterData as CharacterData2
import org.w3c.dom.CharacterData as DOMCharacterData

internal abstract class CharacterDataImpl<N : DOMCharacterData>(delegate: N) : NodeImpl<N>(delegate), CharacterData2 {
    override var data: String
        get() = delegate.data
        set(value) {
            delegate.data = value
        }

    override val ownerDocument: DocumentImpl get() = checkNotNull(super<NodeImpl>.ownerDocument)
    override fun getOwnerDocument(): DocumentImpl = checkNotNull(super.getOwnerDocument())

    override fun getData(): String = data

    override fun setData(data: String) {
        this.data = data
    }

    override fun getNodeValue(): String = data

//    override fun getLength(): Int = delegate.length

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

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in character nodes")

    @IgnorableReturnValue
    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in character nodes")

    @IgnorableReturnValue
    override fun removeChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in character nodes")

    override fun getFirstChild(): Nothing? = null
    override fun getLastChild(): Nothing? = null

}
