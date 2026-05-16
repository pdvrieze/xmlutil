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
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.PlatformProcessingInstruction
import nl.adaptivity.xmlutil.dom.getData
import nl.adaptivity.xmlutil.dom.getNodeName
import nl.adaptivity.xmlutil.dom.nodeType
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.ProcessingInstruction

@ExperimentalXmlUtilApi
public abstract class AbstractProcessingInstruction<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>,
    parentNode: P? = null
) : AbstractLeafNode<N, P>(ownerDocument, parentNode), ProcessingInstruction {
    override fun getOwnerDocument(): AbstractDocument<N, P> {
        return checkNotNull(super.getOwnerDocument())
    }

    final override fun getNodetype(): NodeType = NodeType.PROCESSING_INSTRUCTION_NODE

    final override fun getNamespaceURI(): Nothing? = null

    final override fun getPrefix(): Nothing? = null

    final override fun getLocalName(): Nothing? = null

    final override fun getNodeName(): String = getTarget()

    final override fun getNodeValue(): String = getData()

    @ExperimentalXmlUtilApi
    final override fun setNodeValue(value: String?) {
        setData(value ?: "")
    }

    final override fun getTextContent(): String = getData()

    final override fun setTextContent(value: String?) {
        setData(value ?: "")
    }

    override fun cloneNode(deep: Boolean): AbstractProcessingInstruction<N, P> {
        return getOwnerDocument().createProcessingInstruction(getTarget(), getData())
    }

    override fun isEqualNode(other: PlatformNode): Boolean {
        return when {
            this === other -> true
            nodeType != other.nodeType -> false //handle javascript instance check issues
            other !is PlatformProcessingInstruction -> false
            getTarget() != other.getNodeName() -> false

            else -> getData() == other.getData()
        }

    }
}
