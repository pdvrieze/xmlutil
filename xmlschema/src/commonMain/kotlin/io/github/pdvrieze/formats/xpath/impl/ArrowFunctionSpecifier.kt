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

import io.github.pdvrieze.formats.xpath.XPathVersion
import nl.adaptivity.xmlutil.QName

@XPathInternal
internal sealed class ArrowFunctionSpecifier @NeedsXPath3_1 constructor() {


    abstract fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    )


    context(c: OutputContext)
    abstract fun appendToString(builder: Appendable)

    internal class QNameFunc @NeedsXPath3_1 constructor(val qname: QName) : ArrowFunctionSpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.appendQName(qname)
        }

        override fun collectUnsupportedExprs(
            xPathVersion: XPathVersion,
            isXQuery: Boolean,
            collector: MutableList<Any>
        ) {}

    }

    internal class SeqFunc @NeedsXPath3_1 internal constructor(val elements: List<ExprSingle>): ArrowFunctionSpecifier() {
        init {
            require(elements.isNotEmpty()) {"SeqFunc must have at least one element"}
        }
        @NeedsXPath3_1
        internal constructor(p: ParenExpr): this(
            when(val c = p.expr) {
                is SequenceExpr -> c.elements
                is ExprSingle -> listOf(c)
            }
        )

        override fun collectUnsupportedExprs(
            xPathVersion: XPathVersion,
            isXQuery: Boolean,
            collector: MutableList<Any>
        ) {
            elements.forEach { it.collectUnsupportedExprs(xPathVersion, isXQuery, collector) }
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('(')
            builder.appendExprs(elements)
            builder.append(')')
        }
    }

    class VarRefFunc @NeedsXPath3_1 internal constructor(val varName: String): ArrowFunctionSpecifier() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('$').append(varName)
        }

        override fun collectUnsupportedExprs(
            xPathVersion: XPathVersion,
            isXQuery: Boolean,
            collector: MutableList<Any>
        ) {}
    }
}

