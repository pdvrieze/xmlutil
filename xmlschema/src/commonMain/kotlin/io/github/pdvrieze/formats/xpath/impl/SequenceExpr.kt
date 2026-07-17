/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue

@XPathInternal
class SequenceExpr @NeedsXPath2 constructor(elements: List<ExprSingle>) : AbstractExpr() {
    @NeedsXPath2 constructor(vararg elements: ExprSingle): this(elements.toList())

    val elements: List<ExprSingle> = elements.toList()

    fun isEmpty() = elements.isEmpty()

    @OptIn(NeedsXPath2::class)
    operator fun plus(expr: Expr): SequenceExpr = when (expr) {
        is SequenceExpr -> SequenceExpr(elements + expr.elements)
        is ExprSingle -> SequenceExpr(elements + expr)
    }

    override fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    ) {
        if (xPathVersion< XPathVersion.XPath2_0) collector.add(this)
        for (e in elements) {
            e.collectUnsupportedExprs(xPathVersion, isXQuery, collector)
        }
    }

    @XPathInternal
    context(ctx: EvalContext)
    override fun eval(): XdmValue<*> {
        val elems = buildList {
            for (e in elements) {
                when (val r = e.eval()) {
                    is XdmSequence<*> -> addAll(r.elements)
                    is XdmSingleValue<*> -> add(r)
                }
            }
        }

        return XdmSequence.fromList(elems)
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.appendExprs(elements)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SequenceExpr

        return elements == other.elements
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }


}
