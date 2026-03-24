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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.UntypedType
import nl.adaptivity.xmlutil.dom2.Node

@XPathInternal
@OptIn(NeedsXPath2::class)
class XdmNode constructor(val node: Node, override val type: XdmType = XdmSequenceType.Schema(UntypedType.Instance)) : XdmValue() {
    override fun get(index: Int): XdmNode = this

    fun descendantsSequence(): Sequence<XdmNode> {
        return sequence {
            for (c in node.getChildNodes()) {
                val value = XdmNode(c)
                yield(value)
                yieldAll(value.descendantsSequence())
            }
        }
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean {
        return false
//        if (type.isSubtypeOf(BooleanType.Instance))
    }

    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
        // TODO do some checks
        return XdmNode(node, type)
    }

    override fun isValEqual(expected: XdmValue): Boolean {
        return expected is XdmNode && node == expected.node
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmValue {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "XdmNode(" +
                "type=$type," +
                "node=$node, " +
                ")"
    }


}
