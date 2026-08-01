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

import io.github.pdvrieze.formats.xpath.eval.data.XdmFunction
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmFunctionType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType.SINGLE
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType

@XPathInternal
sealed class XdmFunctionTypeTest(cardinality: OccurrenceType) : XdmTypeTest(cardinality) {
    object ANY_FUNCTION{
        val single: AnyFunction = AnyFunction(SINGLE)
        val opt: AnyFunction = AnyFunction(OccurrenceType.OPTIONAL)
        val any: AnyFunction = AnyFunction(OccurrenceType.ANY)
        val atLeastOne: AnyFunction = AnyFunction(OccurrenceType.AT_LEAST_ONE)
    }

    class AnyFunction(cardinality: OccurrenceType): XdmFunctionTypeTest(cardinality) {
        override val opt: AnyFunction get() = AnyFunction(OccurrenceType.OPTIONAL)
        override val single: AnyFunction get() = AnyFunction(OccurrenceType.SINGLE)
        override val any: AnyFunction get() = AnyFunction(OccurrenceType.ANY)
        override val atLeastOne: AnyFunction get() = AnyFunction(OccurrenceType.AT_LEAST_ONE)

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean = when (source){
            is XdmFunctionTypeTest -> true
            is XdmMapTypeTest -> true
            is XdmArrayTypeTest -> true
            else -> false
        }

        context(ctx: ExprEvalContext)
        override fun sharedBaseType(other: XdmTypeTest, neededCardinality: SequenceType.OccurrenceType): XdmSequenceTypeTest {
            return when (other) {
                !is XdmFunctionTypeTest -> XdmTypeTest.AnyItem(neededCardinality)
                else -> AnyFunction(neededCardinality)
            }
        }

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isSingleInstance(value: XdmSingleValue<*>): Boolean {
            return value is XdmFunction<*>
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

        context(ctx: ExprEvalContext)
        override fun sharedBaseType(
            other: XdmTypeTest,
            neededCardinality: OccurrenceType
        ): XdmSequenceTypeTest {
            if (other is AnyFunction) return AnyFunction(neededCardinality)
            else if (other !is Typed) return XdmTypeTest.AnyItem(neededCardinality)
            if (argTypes.size != other.argTypes.size) return AnyFunction(neededCardinality)
            var leftWorks: Boolean = true
            var rightWorks: Boolean = true
            for (i in argTypes.indices) {
                if (leftWorks && !other.argTypes[i].isAssignableFrom(argTypes[i])) {
                    leftWorks = false
                    if (!rightWorks) return AnyFunction(neededCardinality)
                }
                if (rightWorks && !argTypes[i].isAssignableFrom(other.argTypes[i])) {
                    rightWorks = false
                    if (!leftWorks) return AnyFunction(neededCardinality)
                }
            }

            val sharedReturnType = returnType.sharedBaseType(other.returnType)
            return when {
                leftWorks -> Typed(argTypes, sharedReturnType, neededCardinality)
                rightWorks -> Typed(other.argTypes, sharedReturnType, neededCardinality)
                else -> AnyFunction(neededCardinality) // this should be superfluous
            }

        }

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isSingleInstance(value: XdmSingleValue<*>): Boolean {
            if (value !is XdmFunction<*>) return false

            val funType = value.dynamicType
            if (! returnType.isAssignableFrom(funType.returnType)) return false
            // TODO handle vararg
            if (argTypes.size != funType.argTypes.size) return false
            for (i in argTypes.indices) {
                val origArg = argTypes[i]
                val newFuncArg = funType.argTypes[i]
                val assignable = when (origArg) {
                    NONE -> throw IllegalStateException("None type can not be argument type")
                    EMPTY -> EMPTY.isAssignableFrom(newFuncArg)
                    is XdmTypeTest -> origArg.isAssignableFromSingle(newFuncArg)
                }
                if (!assignable) return false
            }

            return true
        }

        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean = when {
            source !is Typed -> false

            argTypes.size != source.argTypes.size -> false

            !returnType.isAssignableFrom(source.returnType) -> false

            else -> argTypes.asSequence().zip(source.argTypes.asSequence())
                .all { it.second.isAssignableFrom(it.first) }
        }

        override fun toString(): String = buildString {
            append("function(")
            argTypes.joinTo(this)
            append(") as ")
            append(returnType)
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
