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

import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmSingleValue
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.type.XdmEmptySequenceType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSequenceType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.type.XdmType
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.StringType

sealed class XdmSequenceTypeTest {
    @XPathInternal
    context(ctx: ExprEvalContext)
    open fun isAssignableFrom(source: XdmSequenceTypeTest): Boolean = source.isAssignableTo(this)

    @XPathInternal
    context(ctx: ExprEvalContext)
    abstract fun isAssignableTo(receiver: XdmSequenceTypeTest): Boolean

    @XPathInternal
    context(ctx: ExprEvalContext)
    abstract fun isInstance(value: XdmValue<*>): Boolean

    abstract fun toValueType(fallbackType: XdmSingleType): XdmType

    @XPathInternal
    context(ctx: ExprEvalContext)
    abstract fun sharedBaseType(other: XdmSequenceTypeTest): XdmSequenceTypeTest

    object EMPTY : XdmSequenceTypeTest() {
        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isAssignableTo(receiver: XdmSequenceTypeTest): Boolean = when (receiver) {
            is EMPTY -> true
            is XdmTypeTest -> receiver.cardinality.allowsEmpty
        }

        override fun toValueType(fallbackType: XdmSingleType): XdmEmptySequenceType = XdmEmptySequenceType

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun sharedBaseType(other: XdmSequenceTypeTest): XdmSequenceTypeTest {
            return when (other) {
                is EMPTY -> EMPTY
                is XdmTypeTest -> when (other.cardinality) {
                    OccurrenceType.SINGLE,
                    OccurrenceType.OPTIONAL -> XdmTypeTest.AnyItem(OccurrenceType.OPTIONAL)

                    OccurrenceType.ANY,
                    OccurrenceType.AT_LEAST_ONE -> XdmTypeTest.AnyItem(OccurrenceType.ANY)
                }
            }
        }

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun isInstance(value: XdmValue<*>): Boolean = value.size == 0
    }
}


sealed class XdmTypeTest(val cardinality: OccurrenceType) : XdmSequenceTypeTest() {

    @XPathInternal
    context(ctxt: ExprEvalContext)
    abstract fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean

    @XPathInternal
    context(ctxt: ExprEvalContext)
    open fun isAssignableToSingle(receiver: XdmTypeTest): Boolean = receiver.isAssignableFromSingle(this)

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun sharedBaseType(other: XdmSequenceTypeTest): XdmSequenceTypeTest = when (other){
        EMPTY -> EMPTY.sharedBaseType(this)
        is XdmTypeTest -> sharedBaseType(other, cardinality.union(other.cardinality))
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    abstract fun sharedBaseType(other: XdmTypeTest, neededCardinality: OccurrenceType): XdmSequenceTypeTest

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun isAssignableFrom(source: XdmSequenceTypeTest): Boolean {
        val r = when (source) {
            EMPTY -> return cardinality.allowsEmpty
            is XdmTypeTest -> when (cardinality) {
                OccurrenceType.SINGLE -> source.cardinality == OccurrenceType.SINGLE
                OccurrenceType.OPTIONAL -> source.cardinality == OccurrenceType.OPTIONAL || source.cardinality == OccurrenceType.SINGLE
                OccurrenceType.ANY -> true
                OccurrenceType.AT_LEAST_ONE -> !source.cardinality.allowsEmpty
            }
        }
        if (!r) return false
        return isAssignableFromSingle(source)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun isAssignableTo(receiver: XdmSequenceTypeTest): Boolean {
        if (receiver !is XdmTypeTest) return false

        val r = when (cardinality) {
            OccurrenceType.SINGLE -> true
            OccurrenceType.OPTIONAL -> receiver.cardinality.allowsEmpty
            OccurrenceType.ANY -> receiver.cardinality == OccurrenceType.ANY
            OccurrenceType.AT_LEAST_ONE -> receiver.cardinality.allowsMultiple
        }
        if (!r) return false
        return isAssignableToSingle(receiver)
    }

    @XPathInternal
    context(ctx: ExprEvalContext)
    abstract fun isSingleInstance(value: XdmSingleValue<*>): Boolean

    @XPathInternal
    context(ctx: ExprEvalContext)
    override fun isInstance(value: XdmValue<*>): Boolean {
        return when (value) {
            is XdmSequence.EMPTY -> cardinality.allowsEmpty
            is XdmSequence<*> if (value.size > 1) -> (cardinality.allowsMultiple) &&
                    value.all { isSingleInstance(it) }
            else -> isSingleInstance(value as XdmSingleValue<*>) // The cast should always succceed
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XdmTypeTest

        return cardinality == other.cardinality
    }

    override fun hashCode(): Int {
        return cardinality.hashCode()
    }

    object ANY_ITEM {
        val single: AnyItem = AnyItem(OccurrenceType.SINGLE)
        val opt: AnyItem = AnyItem(OccurrenceType.OPTIONAL)
        val any: AnyItem = AnyItem(OccurrenceType.ANY)
        val atLeastOne: AnyItem = AnyItem(OccurrenceType.AT_LEAST_ONE)
    }

    class AnyItem(cardinality: OccurrenceType): XdmTypeTest(cardinality) {
        override val opt: AnyItem get() = AnyItem(OccurrenceType.OPTIONAL)
        override val single: AnyItem get() = AnyItem(OccurrenceType.SINGLE)
        override val any: AnyItem get() = AnyItem(OccurrenceType.ANY)
        override val atLeastOne: AnyItem get() = AnyItem(OccurrenceType.AT_LEAST_ONE)

        context(ctx: ExprEvalContext)
        @XPathInternal
        override fun isSingleInstance(value: XdmSingleValue<*>): Boolean = true

        context(ctxt: ExprEvalContext)
        @XPathInternal
        override fun isAssignableToSingle(receiver: XdmTypeTest): Boolean {
            return receiver is AnyItem
        }

        @XPathInternal
        context(ctxt: ExprEvalContext)
        override fun isAssignableFromSingle(source: XdmSequenceTypeTest): Boolean = true

        @XPathInternal
        context(ctx: ExprEvalContext)
        override fun sharedBaseType(other: XdmTypeTest, neededCardinality: OccurrenceType): AnyItem {
            return AnyItem(neededCardinality)
        }

        override fun toValueType(fallbackType: XdmSingleType): XdmType {
            return when (cardinality) {
                OccurrenceType.SINGLE -> fallbackType
                else -> XdmSequenceType(fallbackType, cardinality)
            }
        }
    }

    abstract val opt: XdmTypeTest
    abstract val single: XdmTypeTest
    abstract val any: XdmTypeTest
    abstract val atLeastOne: XdmTypeTest


    companion object {
        val STRING = XdmSchemaTypeTest(StringType.Instance, OccurrenceType.SINGLE)
    }
}
