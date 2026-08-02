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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.types.UnsignedLongType
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import io.github.pdvrieze.xml.schematypes.values.XsdIntegerProgression
import io.github.pdvrieze.xml.schematypes.values.XsdLong
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedInt
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedLong
import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
internal class XsdUnsignedLongImpl(override val uLongValue: ULong) : XsdUnsignedLong {
    override val xmlString: String get() = uLongValue.toString()
    override val schemaType: UnsignedLongType<*> get() = UnsignedLongType.Instance

    override fun unaryMinus(): XsdLong = XsdLong(-uLongValue.toLong())
    override fun unaryPlus(): XsdUnsignedLong = this

    override fun divRem(divider: XsdUnsignedLong): XsdUnsignedLong.DivRem {
        return DivRem(uLongValue / divider.uLongValue, uLongValue % divider.uLongValue)
    }

    override fun rangeTo(other: XsdUnsignedLong): XsdIntegerProgression<XsdUnsignedLong> {
        return Range(this.uLongValue, other.uLongValue)
    }

    override fun toString(): String {
        return "${uLongValue}u"
    }

    override fun hashCode(): Int {
        return uLongValue.hashCode()
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is XsdUnsignedLong -> uLongValue == other.uLongValue
        is XsdLong -> other.longValue>=0 && other.longValue.toULong() == uLongValue
        is XsdDecimal -> other == this
        else -> false
    }

    data class DivRem(override val quotient: XsdUnsignedLong, override val remainder: XsdUnsignedLong): XsdUnsignedLong.DivRem {
        constructor(quotient: ULong, remainder: ULong): this(XsdUnsignedLongImpl(quotient), XsdUnsignedLongImpl(remainder))
    }

    internal class Range(val start: ULong, val endInclusive: ULong): XsdIntegerProgression<XsdUnsignedLong> {
        override val first: XsdUnsignedLong get() = XsdUnsignedLongImpl(start)
        override val last: XsdUnsignedLong get() = XsdUnsignedLongImpl(endInclusive)

        override fun iterator(): Iterator<XsdUnsignedLong> {
            return RangeIterator(start, endInclusive)
        }
    }

    internal class RangeIterator(start: ULong, private val endInclusive: ULong): Iterator<XsdUnsignedLong> {
        var pos = start

        override fun hasNext(): Boolean {
            return pos <= endInclusive
        }

        override fun next(): XsdUnsignedLong {
            if (pos > endInclusive) throw NoSuchElementException()
            return XsdUnsignedLongImpl(pos++)
        }
    }

}
