/*
 * Copyright (c) 2022.
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

package nl.adaptivity.xmlutil.core.impl.dom

import org.w3c.dom.Document
import org.w3c.dom.Node

internal abstract class NodeImpl(
    ownerDocument: Document
) : Node {

    final override var ownerDocument = ownerDocument
        internal set

    abstract override var parentNode: Node?
        internal set

    override val previousSibling: Node?
        get() {
            val siblings = (parentNode ?: return null).childNodes
            if (siblings.item(0) == this || siblings.length <= 1) return null
            for (idx in 1 until siblings.length) {
                if (siblings.item(idx) == this) {
                    return siblings.item(idx - 1)
                }
            }
            return null
        }

    override val nextSibling: Node?
        get() {
            val siblings = (parentNode ?: return null).childNodes
            if (siblings.item(siblings.length - 1) == this || siblings.length <= 1) return null
            for (idx in 0 until (siblings.length-1)) {
                if (siblings.item(idx) == this) {
                    return siblings.item(idx + 1)
                }
            }
            return null
        }
}

