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

@XPathInternal
class QuantifiedExpr(
    val kind: Kind,
    val bindings: List<Binding>,
    val condition: Expr
) : ExprSingle() {
    init {
        require(bindings.isNotEmpty()) { "Must have at least one binding" }
    }

    enum class Kind(val literal: String) {
        EVERY("every"),
        SOME("some");
    }

    data class Binding(val varName: String, val source: ExprSingle) {
        context(c: OutputContext)
        fun appendToString(builder: Appendable) {
            builder.append('$').append(varName).append(" in ")
            source.appendToString(builder)
        }

        override fun toString(): String = buildString {
            context(OutputContext.EMPTY) {
                appendToString(this)
            }
        }
    }

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.joinHelper(bindings) {
            it.appendToString(builder)
        }

        builder.append(" satisfies ")
        condition.appendToString(builder)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as QuantifiedExpr

        if (kind != other.kind) return false
        if (bindings != other.bindings) return false
        if (condition != other.condition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + bindings.hashCode()
        result = 31 * result + condition.hashCode()
        return result
    }


}
