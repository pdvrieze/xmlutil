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

import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.ItemTypeTest
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.types.IntegerType

sealed class XdmSingleType : XdmType() {
    @OptIn(XPathInternal::class)
    context(ctx: ExprEvalContext)
    final override fun isSubtypeOf(other: XdmType): Boolean {
        return when (other) {
            EmptySequenceType -> false
            is XdmSequenceType -> isSubtypeOf(other.baseType) // all cardinalities allow single values
            is XdmSingleType -> isSubtypeOf(other)
        }
    }

    @OptIn(XPathInternal::class)
    context(ctx: ExprEvalContext)
    abstract fun isSubtypeOf(other: XdmSingleType): Boolean
}

@OptIn(XPathInternal::class)
class XdmSequenceType(val baseType: XdmSingleType = ANY, val cardinality: OccurrenceType): XdmType() {

    protected fun isCardinalSubtype(other: OccurrenceType): Boolean {
        return when (cardinality) {
            OccurrenceType.SINGLE -> true
            OccurrenceType.OPTIONAL -> other.allowsEmpty
            OccurrenceType.ANY -> other == OccurrenceType.ANY
            OccurrenceType.AT_LEAST_ONE -> other == OccurrenceType.AT_LEAST_ONE || other == OccurrenceType.ANY
        }
    }

    context(ctx: ExprEvalContext)
    override fun isSubtypeOf(other: XdmType): Boolean {
        return other is XdmSequenceType && isCardinalSubtype(other.cardinality) && baseType.isSubtypeOf(other.baseType)
    }

    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmValue {
        throw EvaluationException(ctx.expr, "Sequences cannot be created from strings")
    }

    companion object {
        val ANY: XdmTypeTest = XdmTypeTest(ItemTypeTest.ItemTestTest)
        val ANYSEQ = XdmSequenceType(ANY, OccurrenceType.ANY)
        internal val boolean = XdmSchemaType(BooleanType.Instance)
        internal val integer = XdmSchemaType(IntegerType.Instance)
        internal val node = XdmTypeTest(ItemTypeTest.ItemTestTest)

        operator fun invoke(type: AnyType): XdmSchemaType = XdmSchemaType(type)

        operator fun invoke(type: AnyType, cardinality: OccurrenceType = OccurrenceType.SINGLE): XdmSequenceType =
            XdmSequenceType(XdmSchemaType(type), cardinality)
    }
}
