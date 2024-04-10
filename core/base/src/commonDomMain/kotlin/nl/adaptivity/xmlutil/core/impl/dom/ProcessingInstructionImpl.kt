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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IProcessingInstruction
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom.ProcessingInstruction as ProcessingInstruction1
import nl.adaptivity.xmlutil.dom2.ProcessingInstruction as ProcessingInstruction2

internal class ProcessingInstructionImpl(
    ownerDocument: DocumentImpl,
    override val target: String,
    data: String
) : CharacterDataImpl(ownerDocument, data), IProcessingInstruction {
    constructor(ownerDocument: DocumentImpl, original: ProcessingInstruction1) :
            this(ownerDocument, original.nodeName, original.data)

    constructor(ownerDocument: DocumentImpl, original: ProcessingInstruction2) :
            this(ownerDocument, original.getNodeName(), original.getData())

    override val nodetype: NodeType get() = NodeType.PROCESSING_INSTRUCTION_NODE

    override fun getNodeName(): String = getTarget()
}
