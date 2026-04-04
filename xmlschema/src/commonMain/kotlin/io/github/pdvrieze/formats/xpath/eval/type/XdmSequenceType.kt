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
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.types.IntegerType

@OptIn(XPathInternal::class)
class XdmSequenceType(val baseType: XdmSingleType, val cardinality: OccurrenceType): XdmType() {
    override val single: XdmSingleType get() = baseType

    protected fun isCardinalSubtype(other: OccurrenceType): Boolean {
        return when (cardinality) {
            OccurrenceType.SINGLE -> true
            OccurrenceType.OPTIONAL -> other.allowsEmpty
            OccurrenceType.ANY -> other == OccurrenceType.ANY
            OccurrenceType.AT_LEAST_ONE -> other == OccurrenceType.AT_LEAST_ONE || other == OccurrenceType.ANY
        }
    }

    override fun toTypeTest(): XdmSequenceTypeTest {
        return baseType.toTypeTest(cardinality)
    }

    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
        return expectedType is XdmTypeTest &&
                isCardinalSubtype(expectedType.cardinality) &&
                baseType.isAssignableTo(expectedType)
    }

    /*
    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmType): Boolean {
        return expectedType is XdmSequenceType && isCardinalSubtype(expectedType.cardinality) && baseType.isAssignableTo(
            expectedType.baseType)
    }
*/

    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmValue {
        throw EvaluationException(ctx.expr, "Sequences cannot be created from strings")
    }

    override fun toString(): String = buildString{
        append(baseType.toString())
        append(cardinality.literal)
    }

    companion object {
        val UNTYPED: XdmSchemaType get() = XdmSchemaType.UNTYPED
        val ANYSEQ = XdmSequenceType(XdmSchemaType.UNTYPED, OccurrenceType.ANY)
        internal val boolean = XdmSchemaType(BooleanType.Instance)
        internal val integer = XdmSchemaType(IntegerType.Instance)

        operator fun invoke(type: AnyType): XdmSchemaType = XdmSchemaType(type)

        operator fun invoke(type: AnyType, cardinality: OccurrenceType = OccurrenceType.SINGLE): XdmSequenceType =
            XdmSequenceType(XdmSchemaType(type), cardinality)
    }
}
