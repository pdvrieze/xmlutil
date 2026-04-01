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

package io.github.pdvrieze.formats.xpath.eval.typeTest

import io.github.pdvrieze.formats.xpath.eval.type.XdmArrayType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.IntegerType

@OptIn(XPathInternal::class)
sealed class XdmArrayTypeTest(itemType: XdmSequenceTypeTest, cardinality: OccurrenceType) : XdmFunctionTypeTest.Typed(
    listOf(XdmSchemaTypeTest(IntegerType.Instance, OccurrenceType.SINGLE)),
    itemType,
    cardinality
) {

    val itemType: XdmSequenceTypeTest get() = returnType

    override fun toValueType(fallbackType: XdmSingleType): XdmType {
        return super.toValueType(fallbackType)
    }

    object ANY {
        val single: Any = Any(OccurrenceType.SINGLE)
        val opt: Any = Any(OccurrenceType.OPTIONAL)
        val any: Any = Any(OccurrenceType.ANY)
        val atLeastOne: Any = Any(OccurrenceType.AT_LEAST_ONE)
    }

    class Any(cardinality: OccurrenceType) : XdmArrayTypeTest(XdmTypeTest.ANY.any, cardinality) {
        override val opt: Any get() = Any(OccurrenceType.OPTIONAL)
        override val single: Any get() = Any(OccurrenceType.SINGLE)
        override val any: Any get() = Any(OccurrenceType.ANY)
        override val atLeastOne: Any get() = Any(OccurrenceType.AT_LEAST_ONE)

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean {
            return source is XdmArrayTypeTest
        }

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return fallbackType.cardinality(cardinality)
        }

        context(ctxt: ExprEvalContext)
        @XPathInternal
        override fun isAssignableToSingle(receiver: XdmTypeTest): Boolean {
            return receiver is Any || receiver is XdmTypeTest.Any
        }
    }

    class Typed(itemType: XdmSequenceTypeTest, cardinality: OccurrenceType) : XdmArrayTypeTest(itemType, cardinality) {
        override val opt: Typed get() = Typed(itemType, OccurrenceType.OPTIONAL)
        override val single: Typed get() = Typed(itemType, OccurrenceType.SINGLE)
        override val any: Typed get() = Typed(itemType, OccurrenceType.ANY)
        override val atLeastOne: Typed get() = Typed(itemType, OccurrenceType.AT_LEAST_ONE)

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return XdmArrayType(itemType).cardinality(cardinality)
        }

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean = when {
            source !is Typed -> false
            else -> itemType.isAssignableFrom(source.itemType)
        }
    }
}
