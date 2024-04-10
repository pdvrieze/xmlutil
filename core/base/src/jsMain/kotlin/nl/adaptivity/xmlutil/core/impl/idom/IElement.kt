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

import nl.adaptivity.xmlutil.dom.Attr as Attr1
import nl.adaptivity.xmlutil.dom.Element as Element1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2
import nl.adaptivity.xmlutil.dom2.Element as Element2

public interface IElement : INode, Element1, Element2 {
    override fun getNamespaceURI(): String?

    override fun getPrefix(): String?

    override fun getLocalName(): String

    override fun getTagName(): String

    override fun getAttributes(): INamedNodeMap

    override fun getAttributeNode(qualifiedName: String): IAttr?

    override fun getAttributeNodeNS(namespace: String?, localName: String): IAttr?

    override fun setAttributeNode(attr: Attr1): IAttr?

    override fun setAttributeNode(attr: Attr2): IAttr?

    override fun setAttributeNodeNS(attr: Attr1): IAttr?

    override fun setAttributeNodeNS(attr: Attr2): IAttr?

    override fun removeAttributeNode(attr: Attr1): IAttr

    override fun removeAttributeNode(attr: Attr2): IAttr

    override fun getElementsByTagName(qualifiedName: String): INodeList

    override fun getElementsByTagNameNS(namespace: String?, localName: String): INodeList
}
