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

import nl.adaptivity.xmlutil.XmlWriter

@XPathInternal
internal class BinaryExpr(val operator: Operator, val left: Expr, val right: Expr): ExprSingle() {
    override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
        left.appendToString(builder, output)
        builder.append(' ').append(operator.literal).append(' ')
        right.appendToString(builder, output)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as BinaryExpr

        if (operator != other.operator) return false
        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + operator.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }


    companion object {
        fun priority(op: Operator, left: Expr, right: Expr): BinaryExpr {
            if (left !is BinaryExpr ||
                op.priority<= left.operator.priority) return BinaryExpr(op, left, right)

            return BinaryExpr(left.operator, left.left, BinaryExpr(op, left.right, right))
        }
    }
}

