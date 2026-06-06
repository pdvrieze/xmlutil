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
internal class AxisStep(
    val axis: Axis,
    val test: NodeTest,
    val predicates: List<Expr>
) : PrimaryOrStep() {
    constructor(test: NodeTest) : this(Axis.CHILD, test, emptyList())

    constructor(axis: Axis, test: NodeTest) : this(axis, test, emptyList())

    override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
        when (axis) {
            Axis.ATTRIBUTE -> builder.append('@')
            Axis.CHILD -> {}
            else -> builder.append(axis.literal).append("::")
        }
        test.appendToString(builder, output)
        for(p in predicates) {
            builder.append('[')
            p.appendToString(builder, output)
            builder.append(']')
        }
    }
}

@XPathInternal
internal sealed class PrimaryOrStep {
    abstract fun appendToString(builder: StringBuilder, output: XmlWriter?)

    override fun toString(): String = buildString {
        appendToString(this, null)
    }
}

@OptIn(XPathInternal::class)
internal class FilterExpr(val primaryExpr: Expr, val predicates: List<Expr>): PrimaryOrStep() {
    override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
        primaryExpr.appendToString(builder, output)
        if (predicates.isNotEmpty()) {
            builder.append('[')
            val it = predicates.iterator()
            while (it.hasNext()) {
                it.next().appendToString(builder, output)
                if (it.hasNext()) builder.append(", ")
            }
            builder.append(']')
        }
    }
}
