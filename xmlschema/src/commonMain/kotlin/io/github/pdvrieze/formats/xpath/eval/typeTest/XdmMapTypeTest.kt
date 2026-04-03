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

import io.github.pdvrieze.formats.xpath.eval.type.XdmMapType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType

@OptIn(XPathInternal::class)
sealed class XdmMapTypeTest(keyType: XdmSchemaTypeTest, valueType: XdmSequenceTypeTest, cardinality: OccurrenceType): XdmFunctionTypeTest.Typed(
    listOf(keyType),
    valueType,
    cardinality) {

    init {
        require(keyType.cardinality == OccurrenceType.SINGLE) { "Key type must be single" }
    }

    val keyType: XdmSchemaTypeTest get() = argTypes.single() as XdmSchemaTypeTest
    val valueType: XdmSequenceTypeTest get() = returnType

    object ANY{
        val single: Any = Any(OccurrenceType.SINGLE)
        val opt: Any = Any(OccurrenceType.OPTIONAL)
        val any: Any = Any(OccurrenceType.ANY)
        val atLeastOne: Any = Any(OccurrenceType.AT_LEAST_ONE)
    }

    class Any(cardinality: OccurrenceType) : XdmMapTypeTest(
        XdmSchemaTypeTest(AnySimpleType.Instance, OccurrenceType.SINGLE),
        XdmTypeTest.ANY.any,
        cardinality
    ) {

        override val opt: Any get() = Any(OccurrenceType.OPTIONAL)
        override val single: Any get() = Any(OccurrenceType.SINGLE)
        override val any: Any get() = Any(OccurrenceType.ANY)
        override val atLeastOne: Any get() = Any(OccurrenceType.AT_LEAST_ONE)

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return fallbackType.cardinality(cardinality)
        }

        context(ctx: ExprEvalContext)
        override fun sharedBaseType(
            other: XdmTypeTest,
            neededCardinality: OccurrenceType
        ): XdmSequenceTypeTest = when (other) {
            !is XdmMapTypeTest -> super.sharedBaseType(other, neededCardinality)
            else -> Any(neededCardinality)
        }
    }

    class Typed(keyType: XdmSchemaTypeTest, valueType: XdmSequenceTypeTest, cardinality: OccurrenceType) :
        XdmMapTypeTest(keyType, valueType, cardinality) {

        override val opt: Typed get() = Typed(keyType, valueType, OccurrenceType.OPTIONAL)
        override val single: Typed get() = Typed(keyType, valueType, OccurrenceType.SINGLE)
        override val any: Typed get() = Typed(keyType, valueType, OccurrenceType.ANY)
        override val atLeastOne: Typed get() = Typed(keyType, valueType, OccurrenceType.AT_LEAST_ONE)

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return XdmMapType(keyType, valueType).cardinality(cardinality)
        }

        context(ctx: ExprEvalContext)
        override fun sharedBaseType(
            other: XdmTypeTest,
            neededCardinality: OccurrenceType
        ): XdmSequenceTypeTest {
            if (other !is XdmMapTypeTest) return super.sharedBaseType(other, neededCardinality)
            if (other is Any) return Any(neededCardinality)

            val sharedKeyType = when {
                keyType.isAssignableFrom(other.keyType) -> other.keyType
                other.keyType.isAssignableFrom(keyType) -> keyType
                else -> return Any(neededCardinality)
            }

            val sharedValueType = valueType.sharedBaseType(other.valueType)
            return Typed(sharedKeyType, sharedValueType, neededCardinality)
        }
    }
}
