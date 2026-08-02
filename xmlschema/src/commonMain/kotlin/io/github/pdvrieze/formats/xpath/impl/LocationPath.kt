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
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeBase
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue

@OptIn(XPathInternal::class)
@NeedsXPath1
internal class LocationPath(
    val rooted: Boolean,
    val steps: List<PrimaryOrStep>,
) : AbstractExprSingle() {
    constructor(step: AxisStep) : this(false, listOf(step))

    constructor(rooted: Boolean, step: AxisStep) : this(rooted, listOf(step))

    constructor(single: ExprSingle) : this(
        false,
        listOf(FilterExpr(single))
    )

    @XPathInternal
    context(ctx: EvalContext)
    override fun eval(): XdmValue<*> {
        withExprContext {
            val base: XdmValue<*>? = steps.dropLast(1).fold(ctx.contextValue) { c, step ->
                when (val e = step.eval(c)) {
                    XdmSequence.EMPTY -> return XdmSequence.EMPTY

                    is XdmNodeBase<*> -> e

                    is XdmSequence<*> -> {
                        for (m in e) {
                            if (m !is XdmNodeBase<*>) throw EvaluationException(
                                ErrorCodes.XPTY0019_PATH_INTERMEDIATE_NOT_NODES, "Expected node as context item"
                            )
                        }
                        e
                    }

                    else -> throw EvaluationException(
                        ErrorCodes.XPTY0019_PATH_INTERMEDIATE_NOT_NODES,
                        "Expected node as context item, found: ${e.staticType}"
                    )
                } as XdmValue<*>?
            }

            val last = steps.last()
            val result = last.eval(base)

            if (result.isEmpty()) return XdmSequence.EMPTY

            if (result[0] is XdmNodeBase<*>) {
                for (i in 1 until result.size) {
                    if (result[i] !is XdmNodeBase<*>) {
                        throw EvaluationException(ErrorCodes.XPTY0018_PATH_RESULT_MISMATCH, "Expected result of path expression to be uniform in type")
                    }
                }
            } else {
                for (i in 1 until result.size) {
                    if (result[i] is XdmNodeBase<*>) {
                        throw EvaluationException(ErrorCodes.XPTY0018_PATH_RESULT_MISMATCH, "Expected result of path expression to be uniform in type")
                    }
                }
            }
            return result
        }
    }

    override fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    ) {
        steps.forEach { it.collectUnsupportedExprs(xPathVersion, isXQuery, collector) }
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        if (rooted) builder.append('/')

        builder.joinHelper(steps, "/") {
            it.appendToString(builder)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as LocationPath

        if (rooted != other.rooted) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + rooted.hashCode()
        result = 31 * result + steps.hashCode()
        return result
    }


}
