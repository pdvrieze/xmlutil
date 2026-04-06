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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest


@OptIn(XPathInternal::class)
sealed class SequenceType @XPathInternal @NeedsXPath2 constructor() {
    @XPathInternal
    context(c: OutputContext)
    abstract fun appendToString(builder: Appendable)

    context(ctx: ExprEvalContext)
    abstract fun eval(): XdmSequenceTypeTest
    abstract fun isInstance(value: XdmValue<*>): Boolean

    @NeedsXPath2
    object EmptySequence : SequenceType() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("empty-sequence()")
        }

        override fun isInstance(value: XdmValue<*>): Boolean = value.size == 0

        context(ctx: ExprEvalContext)
        override fun eval(): XdmSequenceTypeTest.EMPTY = XdmSequenceTypeTest.EMPTY
    }

    class ItemTypeSequence @NeedsXPath2 constructor(val itemType: ItemTypeTest, val occurrence: OccurrenceType = OccurrenceType.SINGLE) : SequenceType() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            itemType.appendToString(builder)
            builder.append(occurrence.literal)
        }

        override fun isInstance(value: XdmValue<*>): Boolean {
            when {
                !occurrence.allowsEmpty && value.size == 0 -> return false
                value.size == 0 -> return true
                value.size > 1 && !occurrence.allowsMultiple -> return false
            }

            TODO("Check whether needed at all ")
/*
            for (i in 0 until value.size) {
                if (! itemType.isInstance(value[i])) return false
            }
            return true
*/
        }

        @OptIn(NeedsXPath3_1::class)
        context(ctx: ExprEvalContext)
        override fun eval(): XdmTypeTest {
            return itemType.toTypeTest(occurrence)
        }
    }

    enum class OccurrenceType(val literal: String, val allowsEmpty: Boolean, val allowsMultiple: Boolean) {
        SINGLE("", false, false) {
            override fun matches(count: Int) = count == 1
        },
        OPTIONAL("?", true, false) {
            override fun matches(count: Int) = count in 0..1
        },
        ANY("*", true, true) {
            override fun matches(count: Int) = true
        },
        AT_LEAST_ONE("+", false, true) {
            override fun matches(count: Int) = count > 0
        },
        ;

        abstract fun matches(count: Int): Boolean

        fun union(other: OccurrenceType): OccurrenceType = when(this) {
            SINGLE -> other
            OPTIONAL -> when (other) {
                SINGLE, OPTIONAL -> OPTIONAL
                AT_LEAST_ONE, ANY -> ANY
            }
            ANY -> ANY
            AT_LEAST_ONE -> if (other.allowsEmpty) ANY else AT_LEAST_ONE
        }
    }
}
