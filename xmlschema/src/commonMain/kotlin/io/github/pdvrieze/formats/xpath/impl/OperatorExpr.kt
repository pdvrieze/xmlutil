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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.impl.token.Operator

@XPathInternal
internal class OperatorExpr constructor(val operator: Operator, val operands: List<ExprSingle>): AbstractExprSingle() {
    init {
        require(operands.isNotEmpty()) {"OperatorExpr must have at least one operand"}
    }

    constructor(op: Operator, vararg operands: ExprSingle): this(op, operands.asList())

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.joinHelper(operands, " ${operator.literal} ") {
            it.appendToString(builder)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as OperatorExpr

        if (operator != other.operator) return false
        if (operands != other.operands) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + operator.hashCode()
        result = 31 * result + operands.hashCode()
        return result
    }

    companion object {
        fun priority(op: Operator, left: ExprSingle, right: ExprSingle): OperatorExpr {
            when (left) {
                is BinaryExpr  -> return when {
                    left.operator == op -> when (right) {
                        is BinaryExpr if right.operator == op ->
                            OperatorExpr(op, listOf(left.left, left.right, right.left, right.right))

                        is OperatorExpr if right.operator == op ->
                            OperatorExpr(op, listOf(left.left, left.right) + right.operands)

                        else ->
                            OperatorExpr(op, listOf(left.left, left.right, right))
                    }

                    op.priority > left.operator.priority ->
                        OperatorExpr(op, left, right)

                    else ->
                        OperatorExpr(left.operator, left.left, OperatorExpr(op, left.right, right))
                }

                is OperatorExpr -> return when {
                    left.operator == op -> when (right) {
                        is BinaryExpr if right.operator == op ->
                            OperatorExpr(op, left.operands + listOf(right.left, right.right))

                        is OperatorExpr if right.operator == op ->
                            OperatorExpr(op, left.operands + right.operands)

                        else ->
                            OperatorExpr(op, left.operands + listOf(right))

                    }

                    op.priority < left.operator.priority ->
                        OperatorExpr(op, left, right)

                    else ->
                        OperatorExpr(left.operator, buildList {
                            left.operands.asSequence().take(left.operands.size - 1).forEach {
                                add(it)
                            }
                            add(OperatorExpr(op, left.operands.last(), right))
                        })

                }

                else -> return OperatorExpr(op, left, right)
            }
        }

    }

}
