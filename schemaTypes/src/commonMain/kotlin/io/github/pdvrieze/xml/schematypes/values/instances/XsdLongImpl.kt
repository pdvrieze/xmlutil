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

import io.github.pdvrieze.xml.schematypes.types.LongType
import io.github.pdvrieze.xml.schematypes.values.*
import kotlin.math.absoluteValue

internal class XsdLongImpl(override val longValue: Long) : XsdLong {

    constructor(value: CharSequence): this(value.xsToLong())

    override fun toInt(): Int = longValue.toInt()

    override val xmlString: String get() = longValue.toString()
    override val schemaType: LongType<*> get() = LongType.Instance

    override fun toString(): String = xmlString

    override fun unaryMinus(): XsdLong = XsdLongImpl(-longValue)
    override fun unaryPlus(): XsdLong = this

    override fun plus(other: Long): XsdLong {
        return XsdLong(longValue + other)
    }

    override fun plus(other: XsdInteger): XsdInteger = when (other) {
        is XsdLong -> XsdLongImpl(longValue + other.longValue)
        else -> other.plus(this)
    }

    override fun minus(other: Long): XsdLong {
        return XsdLong(longValue - other)
    }

    override fun minus(other: XsdInteger): XsdInteger = when (other) {
        is XsdLong -> XsdLongImpl(longValue - other.longValue)
        else -> other.unaryMinus().plus(this)
    }

    override fun times(multiplier: Long): XsdLong {
        return XsdLong(longValue * multiplier)
    }

    override fun div(other: XsdLong): XsdLong {
        return XsdLong(longValue / other.longValue)
    }

    override fun rem(other: XsdLong): XsdLong {
        return XsdLong(longValue.rem(other.longValue))
    }

    override fun divRem(other: XsdLong): XsdLong.DivRem {
        val quotient = longValue / other.longValue
        val rem = longValue - (quotient*other.longValue)
        return DivRem(quotient, rem)
    }

    override fun abs(): XsdUnsignedLong {
        return XsdUnsignedLong(longValue.absoluteValue.toULong())
    }

    override fun compareTo(other: XsdInteger): Int = when (other) {
        is XsdNonNegativeInteger -> if (longValue < 0L) -1 else longValue.toULong().compareTo(other.toULong())
        else -> longValue.compareTo(other.toLong())
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is XsdLong -> longValue == other.longValue
        other is XsdUnsignedLong -> longValue >=0 && longValue.toULong() == other.toULong()
        other is XsdDecimal -> other == this
        else -> false
    }

    override fun hashCode(): Int {
        return longValue.hashCode()
    }

    data class DivRem(override val quotient: XsdLong, override val remainder: XsdLong) : XsdLong.DivRem {
        constructor(quotient: Long, remainder: Long): this(XsdLong(quotient), XsdLong(remainder))
    }

}
