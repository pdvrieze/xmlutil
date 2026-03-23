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

import io.github.pdvrieze.formats.xpath.data.XdmSequenceType
import io.github.pdvrieze.formats.xpath.data.XdmType


@OptIn(XPathInternal::class)
sealed class SequenceType @XPathInternal @NeedsXPath2 constructor() {
    @XPathInternal
    context(c: OutputContext)
    abstract fun appendToString(builder: Appendable)

    context(ctx: ExprEvalContext)
    abstract fun eval(): XdmType

    @NeedsXPath2
    object EmptySequence : SequenceType() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("empty-sequence()")
        }

        context(ctx: ExprEvalContext)
        override fun eval(): XdmType = XdmType.EmptySequence
    }

    class ItemTypeSequence @NeedsXPath2 constructor(val itemType: ItemTypeTest, val occurrence: OccurrenceType = OccurrenceType.SINGLE) : SequenceType() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            itemType.appendToString(builder)
            builder.append(occurrence.literal)
        }

        context(ctx: ExprEvalContext)
        override fun eval(): XdmType {
            when (itemType) {
                is AtomicOrUnionTypeTest -> return XdmSequenceType.Schema(itemType.eval(), occurrence)
            }

            return XdmSequenceType.ItemType(itemType, occurrence)
        }
    }

    enum class OccurrenceType(val literal: String, val allowsEmpty: Boolean) {
        SINGLE("", false),
        OPTIONAL("?", true),
        ANY("*", true),
        AT_LEAST_ONE("+", false);
    }
}
