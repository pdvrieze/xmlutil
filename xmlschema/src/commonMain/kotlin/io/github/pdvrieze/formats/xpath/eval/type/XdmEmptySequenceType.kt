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

package io.github.pdvrieze.formats.xpath.eval.type

import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

object XdmEmptySequenceType : XdmType() {
    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
        return expectedType is XdmTypeTest && expectedType.cardinality.allowsEmpty
    }

    override val single: XdmSingleType
        get() = throw UnsupportedOperationException("EmptySequence is not a single type")

    override fun toTypeTest(): XdmSequenceTypeTest = XdmSequenceTypeTest.EMPTY

    override fun toString(): String = "EmptySequence()"

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmValue<*> {
        var state: Int = 0
        for (c in value) {
            when (c) {
                ' ', '\t', '\n', '\r' -> {}
                '(' if state == 0 -> state = 1
                ')' if state == 1 -> state = 2

                else -> {
                    state = -1
                    break
                }
            }
        }
        if (state != 2) throw EvaluationException("Cannot convert string to empty sequence")

        return XdmSequence.EMPTY
    }
}
