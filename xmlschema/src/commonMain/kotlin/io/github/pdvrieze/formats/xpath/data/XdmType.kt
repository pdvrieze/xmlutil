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
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.*

@OptIn(XPathInternal::class)
sealed class XdmType {

    context(ctx: ExprEvalContext)
    abstract fun isSubtypeOf(other: XdmType): Boolean

    context(ctx: ExprEvalContext)
    fun isSubtypeOf(expectedType: AnyType): Boolean =
        isSubtypeOf(XdmSchemaType(expectedType))

    context(ctx: ExprEvalContext)
    abstract fun fromString(value: String): XdmValue


    object EmptySequenceType : XdmType() {
        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(other: XdmType): Boolean {
            when {
                other == EmptySequenceType -> return true
                other is XdmSequenceType -> return other.cardinality.allowsEmpty
                else -> return false
            }
        }

        override fun toString(): String = "EmptySequence()"

        context(ctx: ExprEvalContext)
        override fun fromString(value: String): XdmValue {
            var state: Int = 0
            for (c in value) {
                when (c) {
                    ' ', '\t', '\n', '\r' -> {}
                    '(' if state == 0 -> state = 1
                    ')' if state == 1 -> state = 2

                    else -> {
                        state = -1
                        break
                    }
                }
            }
            if (state != 2) throw EvaluationException(ctx.expr, "Cannot convert string to empty sequence")

            return XdmSequence.EMPTY
        }
    }

    companion object {
        val ATOMIC = XdmSchemaType(AnyAtomicType.Instance)
        val STRING = XdmSchemaType(StringType.Instance)
        val BOOLEAN = XdmSchemaType(BooleanType.Instance)
        val INTEGER = XdmSchemaType(IntegerType.Instance)
        val NODE = XdmTypeTest(ItemTypeTest.node)
        val ITEM = XdmTypeTest(ItemTypeTest.ItemTestTest)
        val NUMERIC = XdmSchemaType(NumericType.Instance)
    }
}
