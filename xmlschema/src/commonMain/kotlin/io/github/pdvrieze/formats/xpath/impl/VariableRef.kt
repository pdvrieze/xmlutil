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

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import nl.adaptivity.xmlutil.QName

@OptIn(XPathInternal::class)
@XPathInternal
@NeedsXPath1
internal class VariableRef(val varName: QName): AbstractExprSingle() {
    @XPathInternal
    context(ctx: EvalContext)
    override fun eval(): XdmValue<*> {
        return ctx.resolveVar(varName)
            ?: throw EvaluationException(ErrorCodes.XPST0008_INVALID_NAME, "Undeclared variable: $varName")
    }

    override fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    ) {}

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.append('$').appendQName(varName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as VariableRef

        return varName == other.varName
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + varName.hashCode()
        return result
    }

}
