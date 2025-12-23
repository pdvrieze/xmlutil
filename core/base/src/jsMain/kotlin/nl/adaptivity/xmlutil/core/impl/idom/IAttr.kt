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

import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom.PlatformAttr as Attr1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2

public interface IAttr : INode, Attr1, Attr2 {
    override val ownerElement: IElement?

    override fun getOwnerElement(): IElement? = ownerElement

    override fun getValue(): String = value

    override fun setValue(value: String) {
        this.value = value
    }

    override fun getPrefix(): String? = prefix

    override fun getNamespaceURI(): String? = namespaceURI

    override fun getLocalName(): String? = localName

    override fun getName(): String = name

    public override fun appendChild(node: Node): Nothing =
        throw UnsupportedOperationException("No children in attributes")

    public override fun replaceChild(newChild: Node, oldChild: Node): Nothing =
        throw UnsupportedOperationException("No children in attributes")

    public override fun removeChild(node: Node): Nothing =
        throw UnsupportedOperationException("No children in attributes")

    public override fun getFirstChild(): Nothing? = null
    public override fun getLastChild(): Nothing? = null

}
