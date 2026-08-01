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

package io.github.pdvrieze.formats.xpath.eval.typeTest

import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeBase
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmNodeType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath3_0
import io.github.pdvrieze.formats.xpath.impl.NodeKindTest
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@OptIn(XPathInternal::class, NeedsXPath3_0::class)
class XdmNodeKindTest(val nodeKind: NodeKindTest, cardinality: OccurrenceType) : XdmTypeTest(cardinality) {

    override fun toString(): String = "$nodeKind${cardinality.literal}"

    override val opt: XdmNodeKindTest get() = XdmNodeKindTest(nodeKind, OccurrenceType.OPTIONAL)
    override val single: XdmNodeKindTest get() = XdmNodeKindTest(nodeKind, OccurrenceType.SINGLE)
    override val any: XdmNodeKindTest get() = XdmNodeKindTest(nodeKind, OccurrenceType.ANY)
    override val atLeastOne: XdmNodeKindTest get() = XdmNodeKindTest(nodeKind, OccurrenceType.AT_LEAST_ONE)

    context(ctxt: ExprEvalContext)
    override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean {
        if (source !is XdmNodeKindTest) return false
        return nodeKind.isAssignableFrom(source.nodeKind)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun sharedBaseType(
        other: XdmTypeTest,
        neededCardinality: OccurrenceType
    ): XdmSequenceTypeTest {
        if (other !is XdmNodeKindTest) return AnyItem(neededCardinality)
        val neededNodeKind = when {
            nodeKind.isAssignableFrom(other.nodeKind) -> nodeKind
            other.nodeKind.isAssignableFrom(nodeKind) -> other.nodeKind
            else -> NodeKindTest.AnyNode
        }
        return XdmNodeKindTest(neededNodeKind, neededCardinality)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun isSingleInstance(value: XdmSingleValue<*>): Boolean {
        if (value !is XdmNodeBase<*>) return false
        return nodeKind.matches(value.asT())
    }

    override fun toValueType(fallbackType: XdmSingleType): XdmType {
        return XdmNodeType(nodeKind.type).cardinality(cardinality)
    }
}

