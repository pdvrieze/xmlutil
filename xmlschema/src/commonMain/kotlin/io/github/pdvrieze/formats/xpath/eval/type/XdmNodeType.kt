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

package io.github.pdvrieze.formats.xpath.eval.type

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.NodeKindTest
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.formats.xpath.impl.token.NodeType

class XdmNodeType(val nodeType: NodeType) : XdmSingleType() {
    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
        return when (expectedType) {
            is XdmTypeTest.AnyItem -> true
            is XdmNodeKindTest -> TODO("Node type needs a more precise check")
            else -> false
        }
    }

    @XPathInternal
    override fun toTypeTest(occurrence: OccurrenceType): XdmNodeKindTest {
        return XdmNodeKindTest(NodeKindTest.of(nodeType), occurrence)
    }

    @XPathInternal
    override fun toTypeTest(): XdmNodeKindTest = toTypeTest(OccurrenceType.SINGLE)
    /*
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isAssignableTo(receiver: XdmSingleType): Boolean = when (receiver) {
            ANY -> return true
            is XdmNodeType -> nodeType.isAssignableTo(receiver.nodeType)
            else -> false
        }
    */

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmValue<*> {
        TODO("not implemented")
    }

    companion object {
        val NODE = XdmNodeType(NodeType.ANY_NODE)

        @OptIn(NeedsXPath2::class)
        val ELEMENT = XdmNodeType(NodeType.ELEMENT)
    }
}
