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

import io.github.pdvrieze.formats.xpath.XPath3_0
import nl.adaptivity.xmlutil.QName

sealed class FunctionItem: ExprSingle() {

    class NamedRef @XPath3_0 constructor(val name: QName, val index: Int) : FunctionItem() {
        context(c: OutputContext)
        @XPathInternal
        override fun appendToString(builder: Appendable) {
            builder.appendQName(name).append('#').append(index.toString())
        }
    }

    class Inline @XPath3_0 constructor(val params: List<Param>, val returnType: QName?, body: Expr) : FunctionItem() {
        context(c: OutputContext)
        @XPathInternal
        override fun appendToString(builder: Appendable) {
            builder.append("function(")
            builder.joinHelper(params) { (n, t) ->
                builder.appendQName(n)
                if (t != null) builder.append(" as ").appendQName(t)
            }
            builder.append(')')
            if (returnType != null) builder.append(" as ").appendQName(returnType)
        }

        data class Param(val name: QName, val type: QName?)
    }
}
