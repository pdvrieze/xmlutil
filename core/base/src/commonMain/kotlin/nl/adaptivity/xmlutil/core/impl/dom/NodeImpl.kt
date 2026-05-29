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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.impl.AbstractAttrStorage
import nl.adaptivity.xmlutil.dom2.impl.IAbstractNode
import nl.adaptivity.xmlutil.dom2.impl.LinearNodeStorage

internal interface NodeImpl : IAbstractNode<NodeImpl, ParentNodeImpl> {
    override fun getOwnerDocument(): DocumentImpl?

    companion object {

        internal class StorageAdapter(private val ownerDocument: DocumentImpl): LinearNodeStorage.Adapter<NodeImpl, ParentNodeImpl>, AbstractAttrStorage.Adapter<AttrImpl> {
            override fun checkTypeAndOwner(node: PlatformNode): NodeImpl = when (node) {
                !is NodeImpl -> throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
                else if node.getOwnerDocument() != ownerDocument -> throw DOMException.wrongDocumentErr("Node not owned by this document")
                else -> node
            }

            override fun checkAttr(a: PlatformNode): AttrImpl {
                return when (a) {
                    is AttrImpl -> a
                    else -> throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
                }
            }
        }
    }

}
