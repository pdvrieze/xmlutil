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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.PlatformText
import nl.adaptivity.xmlutil.dom.getData
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.impl.AbstractLeafNode
import nl.adaptivity.xmlutil.dom2.impl.AbstractText

@XmlUtilInternal
public open class TextImpl internal constructor(ownerDocument: DocumentImpl, data: String) :
    CharacterDataImpl(ownerDocument, data), AbstractText<NodeImpl, ParentNodeImpl> {

    internal constructor(ownerDocument: DocumentImpl, original: PlatformText) : this(ownerDocument, original.getData())

    override fun getNodetype(): NodeType = NodeType.TEXT_NODE

    override fun getNodeName(): String = "#text"

    override fun cloneNode(deep: Boolean): TextImpl {
        return TextImpl(getOwnerDocument(), getData())
    }

    override fun toString(): String {
        return getData()
    }
}
