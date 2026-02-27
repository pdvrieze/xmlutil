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

@XPathInternal
internal class ArrowFunction(val expr: ExprSingle, val functionSpecifier: ArrowFunctionSpecifier, val params: List<ExprSingle>): ExprSingle() {
    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        expr.appendToString(builder)
        builder.append(" => ")
        functionSpecifier.appendToString(builder)
        builder.append('(')
        builder.appendExprs(params)
        builder.append(')')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as ArrowFunction

        if (expr != other.expr) return false
        if (functionSpecifier != other.functionSpecifier) return false
        if (params != other.params) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + expr.hashCode()
        result = 31 * result + functionSpecifier.hashCode()
        result = 31 * result + params.hashCode()
        return result
    }


}
