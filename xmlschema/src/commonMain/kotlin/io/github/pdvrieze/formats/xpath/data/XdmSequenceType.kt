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

import io.github.pdvrieze.formats.xpath.data.XdmSequenceType.Schema
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.ItemTypeTest
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath3_0
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*

@OptIn(XPathInternal::class)
sealed class XdmType {

    context(ctx: ExprEvalContext)
    abstract fun isSubtypeOf(other: XdmType): Boolean

    context(ctx: ExprEvalContext)
    fun isSubtypeOf(expectedType: AnyType): Boolean =
        isSubtypeOf(Schema(expectedType))


    object EmptySequence : XdmType() {
        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(other: XdmType): Boolean {
            when {
                other == EmptySequence -> return true
                other is XdmSequenceType -> return other.cardinality.allowsEmpty
                else -> return false
            }
        }

        override fun toString(): String = "EmptySequence()"


    }
}

@OptIn(XPathInternal::class)
sealed class XdmSequenceType(val cardinality: OccurrenceType): XdmType() {

    protected fun isCardinalSubtype(other: OccurrenceType): Boolean {
        return when (cardinality) {
            OccurrenceType.SINGLE -> true
            OccurrenceType.OPTIONAL -> other.allowsEmpty
            OccurrenceType.ANY -> other == OccurrenceType.ANY
            OccurrenceType.AT_LEAST_ONE -> other == OccurrenceType.AT_LEAST_ONE || other == OccurrenceType.ANY
        }
    }

    context(ctx: ExprEvalContext)
    abstract fun isSubtypeItemType(other: XdmSequenceType): Boolean

    context(ctx: ExprEvalContext)
    override fun isSubtypeOf(other: XdmType): Boolean {
        return other is XdmSequenceType && isCardinalSubtype(other.cardinality) && isSubtypeItemType(other)
    }

    @OptIn(XPathInternal::class, NeedsXPath3_0::class)
    class ItemType(val itemType: ItemTypeTest, cardinality: OccurrenceType = OccurrenceType.SINGLE) :
        XdmSequenceType(cardinality) {
        context(ctxt: ExprEvalContext)
        override fun isSubtypeItemType(other: XdmSequenceType): Boolean {
            if (other !is ItemType) {
                TODO()
            }
            return itemType.isSubtypeOf(other.itemType)
        }

        override fun toString(): String = "$itemType${cardinality.literal}"
    }

    class Function(
        val argTypes: List<XdmType>,
        val returnType: XdmType,
        cardinality: OccurrenceType = OccurrenceType.SINGLE
    ) : XdmSequenceType(cardinality) {
        context(ctxt: ExprEvalContext)
        override fun isSubtypeItemType(other: XdmSequenceType): Boolean {
            TODO("not implemented")
        }

        override fun toString(): String {
            return "function(${argTypes.joinToString(", ") { it.toString() }}) as $returnType)"
        }
    }

    class Schema(
        val schemaType: AnyType,
        cardinality: OccurrenceType = OccurrenceType.SINGLE
    ) : XdmSequenceType(cardinality) {
        context(ctxt: ExprEvalContext)
        override fun isSubtypeItemType(other: XdmSequenceType): Boolean {
            when {
                other !is Schema -> return false

                // 2.5.6.2 #1 union type derivation
                other.schemaType is AnySimpleType.AtomicOrUnion<*> &&
                        schemaType.derivesFrom(other.schemaType) -> return true

                // 2.5.6.2 #2 all union elements
                schemaType is AnySimpleUnion<*> && schemaType.members.all { Schema(it).isSubtypeItemType(other,) } ->
                    return true

            }

            return false
        }

        val isGeneralizedAtomic: Boolean by lazy {
            when (schemaType) {
                is AnyAtomicType<*> -> true
                is AnySimpleUnion<*> -> schemaType.isPureUnion()
                else -> false
            }
        }

        override fun toString(): String {
            return schemaType.name.toString()
        }
    }

    class Error(cardinality: OccurrenceType = OccurrenceType.SINGLE): XdmSequenceType(cardinality) {
        context(ctxt: ExprEvalContext)
        override fun isSubtypeItemType(other: XdmSequenceType): Boolean {
            when {
                // 2.5.6.2 #3 Generalized atomic types
                other is Schema && other.schemaType is AnySimpleType.AtomicOrUnion<*> -> return true
            }
            return false
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(other: XdmType): Boolean = when {
            cardinality.allowsEmpty -> EmptySequence.isSubtypeOf(other)
            else -> true
        }

        override fun toString(): String = "xs:error"

    }

    companion object {
        val ANY: ItemType = ItemType(ItemTypeTest.ItemTestTest, OccurrenceType.ANY)
        internal val boolean = Schema(BooleanType.Instance)
        internal val integer = Schema(IntegerType.Instance)
        internal val node = ItemType(ItemTypeTest.ItemTestTest)

        operator fun invoke(type: AnyType, cardinality: OccurrenceType = OccurrenceType.SINGLE): XdmSequenceType =
            Schema(type, cardinality)
    }
}
