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

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*

@OptIn(XPathInternal::class)
sealed class XdmType {
    /**
     * Determine whether this type is assignable to variables of the given type. This is either
     * the same type or a subtype of the given type.
     *
     * @param expectedType the expected parent type.
     */

    context(ctx: ExprEvalContext)
    abstract fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean

    /**
     * Determine whether this type is assignable to variables of the given type. This is either
     * the same type or a subtype of the given type.
     *
     * @param expectedType the expected parent type.
     */

    context(ctx: ExprEvalContext)
    fun isAssignableTo(expectedType: AnyType, cardinality: OccurrenceType = OccurrenceType.SINGLE): Boolean =
        isAssignableTo(XdmSchemaTypeTest(expectedType, cardinality))

    context(ctx: ExprEvalContext)
    abstract fun fromString(value: String): XdmValue<*>

    abstract fun toTypeTest(): XdmSequenceTypeTest

    abstract val single: XdmSingleType

    context(ctx: ExprEvalContext)
    abstract fun sharedBaseType(other: XdmType): XdmType

    companion object {
        val ATOMIC = XdmSchemaType(AnyAtomicType.Instance)
        val STRING = XdmSchemaType(StringType.Instance)
        val BOOLEAN = XdmSchemaType(BooleanType.Instance)
        val INTEGER = XdmSchemaType(IntegerType.Instance)
        val NUMERIC = XdmSchemaType(NumericType.Instance)
    }
}
