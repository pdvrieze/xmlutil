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

import io.github.pdvrieze.xml.schematypes.types.DecimalType
import io.github.pdvrieze.xml.schematypes.values.*
import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
interface XsdBigDecimal : Comparable<XsdDecimal>, XsdDecimal {
    val isInteger: Boolean get() = '.' !in xmlString
    override val schemaType: DecimalType<*> get() = DecimalType.Instance

    /**
     * The base 10 exponent of this number
     */
    val exponent: Int
    /**
     * Determine the amount of decimal digits this number contains.
     */
    val precisionDigits: Int

    override fun round(precision: Int): XsdBigDecimal

    override fun roundToHalfEven(precision: Int): XsdBigDecimal

    /**
     * Exponentize the decimal with base 10 (moves the decimal point n places to the right)
     */
    fun exp10(n: Int): XsdBigDecimal

    override fun plus(other: XsdDecimal): XsdBigDecimal
    override operator fun plus(other: Int): XsdBigDecimal = plus(XsdInt(other))
    override operator fun plus(other: Long): XsdBigDecimal = plus(XsdLong(other))
    override operator fun plus(other: UInt): XsdBigDecimal = plus(XsdUnsignedInt(other))
    override operator fun plus(other: ULong): XsdBigDecimal = plus(XsdUnsignedLong(other))

    override fun minus(other: XsdDecimal): XsdBigDecimal
    override operator fun minus(other: Int): XsdBigDecimal = minus(XsdInt(other))
    override operator fun minus(other: Long): XsdBigDecimal = minus(XsdLong(other))
    override operator fun minus(other: UInt): XsdBigDecimal = minus(XsdUnsignedInt(other))
    override operator fun minus(other: ULong): XsdBigDecimal = minus(XsdUnsignedLong(other))

    override fun times(other: XsdDecimal): XsdBigDecimal
    override operator fun times(multiplier: Int): XsdBigDecimal = times(XsdInt(multiplier))
    override operator fun times(multiplier: Long): XsdBigDecimal = times(XsdLong(multiplier))
    override operator fun times(multiplier: UInt): XsdBigDecimal = times(XsdUnsignedInt(multiplier))
    override operator fun times(multiplier: ULong): XsdBigDecimal = times(XsdUnsignedLong(multiplier))

    override fun divRem(divider: XsdDecimal): DivRem

    override fun divRem(divider: ULong): DivRem = divRem(XsdUnsignedLong(divider))

    override fun divRem(divider: UInt): DivRem = divRem(XsdUnsignedLong(divider))

    override fun div(divider: XsdDecimal): XsdBigDecimal = divRem(divider).quotient

    override fun rem(divider: XsdDecimal): XsdBigDecimal = divRem(divider).remainder

    override fun round(): XsdBigDecimal

    override fun round(precision: XsdInteger): XsdBigDecimal =
        round(precision.toInt())

    override fun roundToHalfEven(): XsdBigDecimal

    override fun roundToHalfEven(precision: XsdInteger): XsdBigDecimal =
        roundToHalfEven(precision.toInt())

    override fun abs(): XsdBigDecimal

    override fun unaryMinus(): XsdBigDecimal

    override fun unaryPlus(): XsdBigDecimal

    override fun ceiling(): XsdBigDecimal

    override fun floor(): XsdBigDecimal

    interface DivRem: XsdDecimal.DivRem {
        override val quotient: XsdBigDecimal
        override val remainder: XsdBigDecimal

        override operator fun component1(): XsdBigDecimal = quotient
        override operator fun component2(): XsdBigDecimal = remainder
    }


    companion object {
        operator fun invoke(i: Int): XsdBigDecimal = BigDecimal(i)
        operator fun invoke(i: UInt): XsdBigDecimal = BigDecimal(i)
        operator fun invoke(l: Long): XsdBigDecimal = BigDecimal(l)
        operator fun invoke(l: ULong): XsdBigDecimal = BigDecimal(l)
        operator fun invoke(s: String): XsdBigDecimal = BigDecimal(s)
    }

    fun getDecimalDigit(pos: Int): Char
}
