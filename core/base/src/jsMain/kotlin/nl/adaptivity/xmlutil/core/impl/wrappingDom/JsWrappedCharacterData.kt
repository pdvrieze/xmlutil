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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.CharacterData as CharacterData2
import org.w3c.dom.CharacterData as DOMCharacterData

internal abstract class JsWrappedCharacterData<N : DOMCharacterData>(delegate: N) : JsWrappedNode<N>(delegate), CharacterData2 {
    var data: String
        get() = delegate.data
        set(value) {
            delegate.data = value
        }

    override fun getNamespaceURI(): Nothing? = null
    override fun getPrefix(): Nothing? = null
    override fun getLocalName(): Nothing? = null

    override fun getOwnerDocument(): JsWrappedDocument = checkNotNull(super.getOwnerDocument())

    override fun getData(): String = delegate.data

    override fun setData(data: String) {
        delegate.data = data
    }

    override fun getNodeValue(): String = data

    @ExperimentalXmlUtilApi
    override fun setNodeValue(value: String?) {
        data = value ?: ""
    }
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
        throw DOMException.hierarchyRequestErr("No children in character nodes")

    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character nodes")

    @IgnorableReturnValue
    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character nodes")

    @IgnorableReturnValue
    override fun removeChild(node: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in character nodes")

    override fun getFirstChild(): Nothing? = null
    override fun getLastChild(): Nothing? = null

    override fun getAttributes(): Nothing? = null
    @ExperimentalXmlUtilApi
    override fun hasAttributes(): Boolean = false

    abstract override fun cloneNode(deep: Boolean): JsWrappedCharacterData<N>
}
