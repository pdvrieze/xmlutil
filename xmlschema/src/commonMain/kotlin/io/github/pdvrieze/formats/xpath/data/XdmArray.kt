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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xmlschema.types.isContentEqual
import io.github.pdvrieze.formats.xpath.data.ErrorCodes.FORG0006_INVALID_ARGUMENT_TYPE
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@XPathInternal
class XdmArray(val content: List<XdmValue>, override val type: XdmArrayType) : XdmFunction<XdmArray>() {
    override fun asT(): XdmArray = this

    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: MutableList<in XdmSingleValue<*>>) {
        for (c in content) c.atomizeTo(receiver)
    }

    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
        if (type !is XdmArrayType) throw EvaluationException(ctx.expr, "Cannot cast array to $type")
        //TODO check type compatibility
        return XdmArray(content, type)
    }

    override fun isValEqual(expected: XdmValue): Boolean {
        return expected is XdmArray &&
                content.isContentEqual(expected.content)
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean {
        throw EvaluationException(FORG0006_INVALID_ARGUMENT_TYPE, "Cannot convert array to boolean")
    }
}


