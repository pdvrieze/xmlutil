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
import nl.adaptivity.xmlutil.dom2.CharacterData

@ExperimentalXmlUtilApi
public abstract class AbstractCharacterData<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>,
    parentNode: P? = null
) : AbstractLeafNode<N, P>(ownerDocument, parentNode), CharacterData {

    override fun getOwnerDocument(): AbstractDocument<N, P> {
        return checkNotNull(super.getOwnerDocument()) { "Attributes cannot have a null owner document" }
    }

    final override fun getNodeValue(): String = getData()

    final override fun getTextContent(): String = getData()

    final override fun setTextContent(value: String?) {
        setData(value ?: "")
    }
}
