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
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType

@XPathInternal
object XdmErrorType : XdmSingleType() {
    context(ctx: ExprEvalContext)
    override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean = when {
        // 2.5.6.2 #3 Generalized atomic types
        expectedType is XdmSchemaTypeTest && expectedType.schemaType is AnySimpleType.AtomicOrUnion<*> -> true
        else -> false
    }

    override fun toTypeTest(occurrence: SequenceType.OccurrenceType): Nothing {
        TODO("not implemented")
    }

    /*
        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(other: XdmType): Boolean = when {
            cardinality.allowsEmpty -> EmptySequence.isSubtypeOf(other)
            else -> true
        }
    */
    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmValue {
        throw EvaluationException(ctx.expr, "Errors cannot be created from strings")
    }

    override fun toString(): String = "xs:error"

}
