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
import nl.adaptivity.xmlutil.dom2.ProcessingInstruction as ProcessingInstruction2
import org.w3c.dom.ProcessingInstruction as DomProcessingInstruction

internal class JsWrappedProcessingInstruction(delegate: DomProcessingInstruction) :
    JsWrappedNode<DomProcessingInstruction>(delegate), ProcessingInstruction2 {

    override fun getOwnerDocument(): JsWrappedDocument = checkNotNull(super.getOwnerDocument())

    override fun getNodeValue(): String {
        return delegate.data
    }

    override fun setNodeValue(value: String?) {
        delegate.data = value ?: ""
    }

    override fun setData(data: String) {
        delegate.data = data
    }

    override fun getData(): String = delegate.data


    override fun getNamespaceURI(): Nothing? = null
    override fun getPrefix(): Nothing? = null
    override fun getLocalName(): String? = delegate.target

    override fun getTarget(): String = delegate.target

    override fun getAttributes(): Nothing? = null

    @ExperimentalXmlUtilApi
    override fun hasAttributes(): Boolean = false

    override fun cloneNode(deep: Boolean): JsWrappedProcessingInstruction {
        return JsWrappedProcessingInstruction(delegate.cloneNode(deep) as DomProcessingInstruction)
    }

    override fun appendChild(node: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in processing instruction")

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in processing instruction")

    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing =
        throw DOMException.hierarchyRequestErr("No children in processing instruction")

    override fun removeChild(node: PlatformNode): Nothing =
        throw DOMException.hierarchyRequestErr("No children in processing instruction")

}
