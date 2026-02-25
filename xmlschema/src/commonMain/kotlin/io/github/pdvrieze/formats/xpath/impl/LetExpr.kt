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
class LetExpr(val bindings: List<Binding>, val returnExp: ExprSingle): ExprSingle() {
    override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
        builder.append("let ")
        val it = bindings.iterator()
        it.next().appendToString(builder, output)
        while (it.hasNext()) {
            builder.append(", ")
            it.next().appendToString(builder, output)
        }
        builder.append(" return ")
        returnExp.appendToString(builder, output)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as LetExpr

        if (bindings != other.bindings) return false
        if (returnExp != other.returnExp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + bindings.hashCode()
        result = 31 * result + returnExp.hashCode()
        return result
    }


    class Binding(val varName: String, val expr: ExprSingle) {
        fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append('$').append(varName).append(" := ")
            expr.appendToString(builder, output)
        }

        override fun toString(): String = buildString {
            appendToString(this, null)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Binding

            if (varName != other.varName) return false
            if (expr != other.expr) return false

            return true
        }

        override fun hashCode(): Int {
            var result = varName.hashCode()
            result = 31 * result + expr.hashCode()
            return result
        }

    }
}
