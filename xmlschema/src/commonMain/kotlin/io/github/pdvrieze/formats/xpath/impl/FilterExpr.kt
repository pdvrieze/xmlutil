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

@OptIn(XPathInternal::class)
internal class FilterExpr(val primaryExpr: ExprSingle, val predicates: List<Expr> = emptyList()): PrimaryOrStep() {
    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        primaryExpr.appendToString(builder)
        for (p in predicates) {
            builder.append('[')
            p.appendToString(builder)
            builder.append(']')
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as FilterExpr

        if (primaryExpr != other.primaryExpr) return false
        if (predicates != other.predicates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + primaryExpr.hashCode()
        result = 31 * result + predicates.hashCode()
        return result
    }


}
