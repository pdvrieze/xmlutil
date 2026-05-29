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
import nl.adaptivity.xmlutil.dom.PlatformProcessingInstruction
import nl.adaptivity.xmlutil.dom.getData
import nl.adaptivity.xmlutil.dom.getNodeName
import nl.adaptivity.xmlutil.dom2.impl.AbstractProcessingInstruction

internal class ProcessingInstructionImpl internal constructor(
    ownerDocument: DocumentImpl,
    target: String,
    data: String,
    parentNode: ParentNodeImpl? = null,
) : AbstractProcessingInstruction<NodeImpl, ParentNodeImpl>(ownerDocument, parentNode), NodeImpl {
    private val _target = target
    private var _data = data

    internal constructor(ownerDocument: DocumentImpl, original: PlatformProcessingInstruction) :
            this(ownerDocument, original.getNodeName(), original.getData())

    override fun getOwnerDocument(): DocumentImpl {
        return super.getOwnerDocument() as DocumentImpl
    }

    override fun getTarget(): String = _target

    override fun getData(): String = _data

    override fun setData(data: String) {
        this._data = data
    }
}
