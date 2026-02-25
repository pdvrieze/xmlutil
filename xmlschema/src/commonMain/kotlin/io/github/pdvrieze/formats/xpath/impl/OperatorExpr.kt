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

import nl.adaptivity.xmlutil.XmlWriter

@XPathInternal
internal class OperatorExpr(val operator: Operator, val operands: List<ExprSingle>): ExprSingle() {
    init {
        require(operands.isNotEmpty()) {"OperatorExpr must have at least one operand"}
    }

    override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
        val it = operands.iterator()
        it.next().appendToString(builder, output)
        while (it.hasNext()) {
            builder.append(' ').append(operator.literal).append(' ')
            it.next().appendToString(builder, output)
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

}
