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

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.nodeType
import nl.adaptivity.xmlutil.dom2.DocumentType
import nl.adaptivity.xmlutil.dom2.NodeType

@ExperimentalXmlUtilApi
public abstract class AbstractDocumentType<out N: IAbstractNode<N, P>, out P: IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>?,
    parentNode: P? = null,
) : AbstractLeafNode<N, P>(ownerDocument, parentNode), DocumentType {

    final override fun getNodetype(): NodeType = NodeType.DOCUMENT_TYPE_NODE
    final override fun getNodeValue(): Nothing? = null
    final override fun getNodeName(): String = getName()

    final override fun setTextContent(value: String?) {/* Defined as NO-OP*/ }
    final override fun getTextContent(): Nothing? = null

    abstract override fun cloneNode(deep: Boolean): AbstractDocumentType<N, P>

    override fun isEqualNode(other: PlatformNode): Boolean {
        return when {
            this === other -> true
            nodeType != other.nodeType -> false //handle javascript instance check issues
            other !is PlatformDocumentType -> false
            else -> true // TODO have more expansive documentType
        }

    }
}
