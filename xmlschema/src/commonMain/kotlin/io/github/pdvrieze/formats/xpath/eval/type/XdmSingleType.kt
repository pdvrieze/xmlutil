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
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

sealed class XdmSingleType : XdmType() {
/*
    @OptIn(XPathInternal::class)
    context(ctx: ExprEvalContext)
    final override fun isAssignableTo(expectedType: XdmType): Boolean {
        return when (expectedType) {
            XdmEmptySequenceType -> false
            is XdmSequenceType -> isAssignableTo(expectedType.baseType) // all cardinalities allow single values
            is XdmSingleType -> isAssignableTo(expectedType)
        }
    }

    @OptIn(XPathInternal::class)
    context(ctx: ExprEvalContext)
    abstract fun isAssignableTo(receiver: XdmSingleType): Boolean
*/

    val opt: XdmSequenceType get() = XdmSequenceType(this, OccurrenceType.OPTIONAL)
    val atLeastOne: XdmSequenceType get() = XdmSequenceType(this, OccurrenceType.AT_LEAST_ONE)
    val any: XdmSequenceType get() = XdmSequenceType(this, OccurrenceType.ANY)
    override val single: XdmSingleType get() = this

    override fun toTypeTest(): XdmSequenceTypeTest {
        return toTypeTest(OccurrenceType.SINGLE)
    }

    abstract fun toTypeTest(occurrence: OccurrenceType): XdmSequenceTypeTest

    fun cardinality(occurrence: OccurrenceType): XdmType = when (occurrence) {
        OccurrenceType.SINGLE -> this
        else -> XdmSequenceType(this, occurrence)
    }

    object ANY: XdmSingleType() {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isAssignableTo(expectedType: XdmSequenceTypeTest): Boolean {
            return XdmTypeTest.ANY.single.isAssignableTo(expectedType)
        }

        override fun toTypeTest(occurrence: OccurrenceType): XdmTypeTest.Any {
            return XdmTypeTest.Any(occurrence)
        }

        override fun toTypeTest(): XdmTypeTest.Any = toTypeTest(OccurrenceType.SINGLE)

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun fromString(value: String): XdmValue<*> {
            TODO("not implemented")
        }

    }
}

