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

import io.github.pdvrieze.formats.xpath.eval.data.XdmArray
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
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

    object ANY_ARRAY {
        val single: AnyArray = AnyArray(OccurrenceType.SINGLE)
        val opt: AnyArray = AnyArray(OccurrenceType.OPTIONAL)
        val any: AnyArray = AnyArray(OccurrenceType.ANY)
        val atLeastOne: AnyArray = AnyArray(OccurrenceType.AT_LEAST_ONE)
    }

    class AnyArray(cardinality: OccurrenceType) : XdmArrayTypeTest(XdmTypeTest.ANY_ITEM.any, cardinality) {
        override val opt: AnyArray get() = ANY_ARRAY.opt
        override val single: AnyArray get() = ANY_ARRAY.single
        override val any: AnyArray get() = ANY_ARRAY.any
        override val atLeastOne: AnyArray get() = ANY_ARRAY.atLeastOne

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean {
            return source is XdmArrayTypeTest
        }

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return fallbackType.cardinality(cardinality)
        }

        context(ctx: ExprEvalContext)
        override fun sharedBaseType(
            other: XdmTypeTest,
            neededCardinality: OccurrenceType
        ): XdmSequenceTypeTest {
            return when (other) {
                !is XdmArrayTypeTest -> super.sharedBaseType(other, neededCardinality)
                else -> AnyArray(neededCardinality)
            }
        }

        @XPathInternal
        context(ctxt: ExprEvalContext)
        override fun isAssignableToSingle(receiver: XdmTypeTest): Boolean {
            return receiver is AnyArray || receiver is XdmTypeTest.AnyItem
        }

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isSingleInstance(value: XdmSingleValue<*>): Boolean {
            return value is XdmArray
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

        context(ctx: ExprEvalContext)
        override fun sharedBaseType(
            other: XdmTypeTest,
            neededCardinality: OccurrenceType
        ): XdmSequenceTypeTest {
            if (other !is XdmArrayTypeTest) return super.sharedBaseType(other, neededCardinality)
            else if (other is AnyArray) return AnyArray(neededCardinality)

            val sharedItemType = itemType.sharedBaseType(other.itemType)
            return Typed(sharedItemType, neededCardinality)
        }

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean = when {
            source !is Typed -> false
            else -> itemType.isAssignableFrom(source.itemType)
        }

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isSingleInstance(value: XdmSingleValue<*>): Boolean {
            if (value !is XdmArray) return false
            return value.all { itemType.isInstance(it) }
        }
    }
}
