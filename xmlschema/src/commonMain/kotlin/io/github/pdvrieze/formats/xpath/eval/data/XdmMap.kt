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

package io.github.pdvrieze.formats.xpath.eval.data

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.type.XdmMapType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmMapTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@XPathInternal
class XdmMap(val content: Map<XdmAtomic<*>, XdmValue>, override val staticType: XdmMapType) : XdmFunction<XdmMap>() {
    override fun asT(): XdmMap = this

    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue {
        if (type !is XdmMapTypeTest) throw EvaluationException(ctx.expr, "Cannot cast map to $type")
        return XdmMap(content, type.toValueType(staticType).single as XdmMapType)
    }

    override fun isValEqual(expected: XdmValue): Boolean {
        if (expected !is XdmMap) return false
        if (expected.content.size != content.size) return false

        for ((k, v) in content) {
            if (expected.content[k] != v) return false
        }
        return true
    }

    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: MutableList<in XdmAtomic<*>>): Nothing {
        throw EvaluationException(ErrorCodes.FOTY0013, "Cannot atomize a map")
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): Nothing {
        throw EvaluationException(ErrorCodes.FOTY0013, "Cannot atomize a map")
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Nothing {
        throw EvaluationException(
            ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE,
            ctx.expr,
            "Cannot cast maps to boolean"
        )
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun normalizeToArithmetic(): XdmValue {
        throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Maps are not compatible with an arithmetic operator")
    }
}
