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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.core.impl.dom.NodeImpl
import org.w3c.dom.Node
import org.w3c.dom.EntityReference as DOMEntityReference

internal class EntityReferenceImpl(delegate: DOMEntityReference) : AbstractNodeImpl<DOMEntityReference>(delegate),
    DOMEntityReference {

    override fun insertBefore(newChild: Node, refChild: Node?): NodeImpl? {
        return delegate.insertBefore(newChild.unWrap(), refChild?.unWrap()).wrap() as NodeImpl?
    }
}
