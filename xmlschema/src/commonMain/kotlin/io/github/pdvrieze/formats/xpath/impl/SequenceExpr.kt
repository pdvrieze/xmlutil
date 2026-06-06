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
class SequenceExpr(elements: List<Expr>) : Expr() {
    constructor(vararg elements: Expr): this(elements.toList())

    val elements = elements.toList()

    operator fun plus(expr: Expr): SequenceExpr = when (expr) {
        is SequenceExpr -> SequenceExpr(elements + expr.elements)
        else -> SequenceExpr(elements + expr)
    }

    override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
        val it = elements.iterator()
        it.next().appendToString(builder, output)
        while (it.hasNext()) {
            builder.append(", ")
            it.next().appendToString(builder, output)
        }
    }
}
