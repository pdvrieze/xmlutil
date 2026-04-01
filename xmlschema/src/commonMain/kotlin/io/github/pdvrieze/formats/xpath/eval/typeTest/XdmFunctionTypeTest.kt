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

import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType.SINGLE
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType

@XPathInternal
sealed class XdmFunctionTypeTest(cardinality: OccurrenceType) : XdmTypeTest(cardinality) {
    object ANY{
        val single: Any = Any(SINGLE)
        val opt: Any = Any(OccurrenceType.OPTIONAL)
        val any: Any = Any(OccurrenceType.ANY)
        val atLeastOne: Any = Any(OccurrenceType.AT_LEAST_ONE)
    }

    class Any(cardinality: OccurrenceType): XdmFunctionTypeTest(cardinality) {
        override val opt: Any get() = Any(OccurrenceType.OPTIONAL)
        override val single: Any get() = Any(OccurrenceType.SINGLE)
        override val any: Any get() = Any(OccurrenceType.ANY)
        override val atLeastOne: Any get() = Any(OccurrenceType.AT_LEAST_ONE)

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean = when (source){
            is XdmFunctionTypeTest -> true
            is XdmMapTypeTest -> true
            is XdmArrayTypeTest -> true
            else -> false
        }

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return fallbackType.cardinality(cardinality)
        }
    }

    open class Typed(
        val argTypes: List<XdmSequenceTypeTest>,
        val returnType: XdmSequenceTypeTest,
        cardinality: OccurrenceType = SINGLE
    ) : XdmFunctionTypeTest(cardinality) {
        override val opt: Typed get() = Typed(argTypes, returnType, OccurrenceType.OPTIONAL)
        override val single: Typed get() = Typed(argTypes, returnType, OccurrenceType.SINGLE)
        override val any: Typed get() = Typed(argTypes, returnType, OccurrenceType.ANY)
        override val atLeastOne: Typed get() = Typed(argTypes, returnType, OccurrenceType.AT_LEAST_ONE)

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return XdmFunctionType(argTypes, returnType).cardinality(cardinality)
        }

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean = when {
            source !is Typed -> false

            argTypes.size != source.argTypes.size -> false

            !returnType.isAssignableFrom(source.returnType) -> false

            else -> argTypes.asSequence().zip(source.argTypes.asSequence())
                .all { it.second.isAssignableFrom(it.first) }
        }
    }

    companion object {
        operator fun invoke(
            argTypes: List<XdmTypeTest>,
            returnType: XdmTypeTest,
            cardinality: OccurrenceType = SINGLE
        ): Typed = Typed(argTypes, returnType, cardinality)


        operator fun invoke(
            returnType: XdmTypeTest,
            vararg argTypes: XdmTypeTest,
        ): Typed = Typed(argTypes.toList(), returnType, SINGLE)

        operator fun invoke(
            argTypes: List<AnySimpleType.AtomicOrUnion<*>>,
            returnType: AnySimpleType.AtomicOrUnion<*>,
            cardinality: OccurrenceType = SINGLE
        ): Typed = Typed(
            argTypes.map { XdmSchemaTypeTest(it, SINGLE) },
            XdmSchemaTypeTest(returnType, SINGLE),
            cardinality
        )


        operator fun invoke(
            returnType: AnySimpleType.AtomicOrUnion<*>,
            vararg argTypes: AnySimpleType.AtomicOrUnion<*>,
        ): Typed = Typed(
            argTypes.map { XdmSchemaTypeTest(it, SINGLE) },
            XdmSchemaTypeTest(returnType, SINGLE),
            SINGLE
        )


    }
}
