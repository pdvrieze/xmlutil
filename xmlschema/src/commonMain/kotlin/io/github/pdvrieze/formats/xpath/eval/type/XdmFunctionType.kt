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
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmFunctionTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType.SINGLE
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyType

open class XdmFunctionType(
    val argTypes: List<XdmSequenceTypeTest>,
    val returnType: XdmSequenceTypeTest
) : XdmSingleType() {

    constructor(returnType: XdmTypeTest, vararg argTypes: XdmTypeTest) : this(
        argTypes.toList(),
        returnType
    )

    @OptIn(XPathInternal::class)
    constructor(returnType: AnyType, vararg argTypes: AnyType) : this(
        argTypes.map { XdmSchemaTypeTest(it, SINGLE) },
        XdmSchemaTypeTest(returnType, SINGLE),
    )

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
        when (expectedType) {
            is XdmFunctionTypeTest.Any -> return true
            !is XdmFunctionTypeTest.Typed -> return false
            else -> {}
        }


        if (argTypes.size != expectedType.argTypes.size) return false
        if (!returnType.isAssignableTo(expectedType.returnType)) return false

        return expectedType.argTypes.asSequence()
            .zip(argTypes.asSequence())
            .all { (a, b) -> b.isAssignableTo(a) }
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmValue {
        throw EvaluationException(ctx.expr, "Functions cannot be created from strings")
    }

    override fun toString(): String {
        return "function(${argTypes.joinToString(", ") { it.toString() }}) as $returnType)"
    }
}
