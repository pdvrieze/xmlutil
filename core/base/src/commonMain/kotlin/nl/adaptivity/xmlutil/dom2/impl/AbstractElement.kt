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
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.NodeType

@ExperimentalXmlUtilApi
public abstract class AbstractElement<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>,
    nodeStorage: (P) -> AbstractNodeStorage<N, P>,
    parentNode: P? = null,
) : AbstractParentNode<N, P>(ownerDocument, nodeStorage, parentNode), Element {

    final override fun getNodetype(): NodeType = NodeType.ELEMENT_NODE
    final override fun getNodeValue(): Nothing? = null

    final override fun getNodeName(): String = when (val p = getPrefix()) {
        null, "" -> getLocalName()
        else -> "$p:${getLocalName()}"
    }

    override fun getOwnerDocument(): AbstractDocument<N, P> {
        return checkNotNull(super.getOwnerDocument()) { "Elements cannot have a null owner document" }
    }

}
