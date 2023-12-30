/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xpath.impl

@XPathInternal
internal class AxisStep(
    val axis: Axis,
    val test: NodeTest,
    val predicates: List<Expr>
) : PrimaryOrStep() {
    constructor(test: NodeTest) : this(Axis.CHILD, test, emptyList())

    constructor(axis: Axis, test: NodeTest) : this(axis, test, emptyList())

    override fun toString(): String = buildString {
        when (axis) {
            Axis.ATTRIBUTE -> append('@')
            Axis.CHILD -> {}
            else -> append(axis.literal).append("::")
        }
        append(test.toString())
        for (p in predicates) {
            append("[").append(p).append("]")
        }
    }
}

@XPathInternal
internal sealed class PrimaryOrStep

@OptIn(XPathInternal::class)
internal class FilterExpr(val primaryExpr: Expr, val predicates: List<Expr>): PrimaryOrStep() {
    override fun toString(): String = buildString {
        append(primaryExpr)
        if (predicates.isNotEmpty()) {
            append('[')
            predicates.joinTo(this)
            append(']')
        }
    }
}
