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
internal class LookupExpr(val context: Expr?, val key: KeySpecifier): ExprSingle() {
    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        context?.appendToString(builder)
        builder.append('?')
        key.appendToString(builder)
    }

    sealed class KeySpecifier {
        context(c: OutputContext)
        abstract fun appendToString(builder: Appendable)
        override fun toString(): String = buildString {
            context(OutputContext.EMPTY) {
                appendToString(this)
            }
        }
    }

    class IntegerKey(val value: Int) : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            when (builder) {
                is StringBuilder -> builder.append(value)
                else -> builder.append(value.toString())
            }
        }
    }

    class NCNameKey(val value: String) : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append(value)
        }
    }
    object AnyKey : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('*')
        }
    }
    class ParenKey(val expr: Expr) : KeySpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('(')
            expr.appendToString(builder)
            builder.append(')')
        }
    }
}
