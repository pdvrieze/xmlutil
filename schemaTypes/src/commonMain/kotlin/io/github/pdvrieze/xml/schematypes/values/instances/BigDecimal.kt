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

import io.github.pdvrieze.xml.schematypes.requireRange
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInteger


@OptIn(ExperimentalUnsignedTypes::class)
class BigDecimal(sign: Int, ints: UIntArray, decimalPositions: Long) :
    AbstractBigDecimal<BigDecimal>(sign, ints, decimalPositions) {

    init {
        val specialSign = _sign ushr 29
        requireRange(specialSign == 0b000 || specialSign == 0b111) {
            ""
        }
        if (! (specialSign == 0b000 || specialSign == 0b111)) throw NumberFormatException("Invalid special value")
    }

    constructor(value: CharSequence): this(parse(value))

    constructor(bigDecimal: InfBigDecimal): this(bigDecimal._sign, bigDecimal.ints, bigDecimal.decimalPositions)

    private constructor(parseResult: ParseResult) :
            this(parseResult.sign, parseResult.ints, parseResult.decimalDigits)

    constructor(value: UInt): this(
        sign = if (value == 0u) 0 else 1,
        ints = uintArrayOf(value),
        decimalPositions = 0L
    )

    constructor(value: ULong) : this(
        sign = if (value == 0uL) 0 else 1,
        ints = when {
            value <= UInt.MAX_VALUE -> uintArrayOf(value.toUInt())
            else -> uintArrayOf(value.toUInt(), (value shr 32).toUInt())
        },
        decimalPositions = 0L
    )

    override val self: BigDecimal get() = this
    override val isNaN: Boolean get() = false
    override val isInfinity: Boolean get() = false
    override val isNegativeInfinity: Boolean get() = false

    override val companion: Companion get() = Companion

    override fun XsdDecimal.asT(): BigDecimal = when (this) {
        is BigDecimal -> this
        is AbstractBigDecimal<*> -> {
            requireRange(isFinite) { "Cannot convert non-finite XsdBigDecimal to BigDecimal" }
            BigDecimal(sign, ints, decimalPositions)
        }
        is XsdInteger -> BigDecimal(sign, UIntArray(size.toInt()) { get(it) }, 0L)
        else -> BigDecimal(xmlString) // fallback to parsing
    }

    companion object : CompanionBase<BigDecimal>() {
        override val ZERO = BigDecimal(0, uintArrayOf(0u), 0)
        override val ONE = BigDecimal(1, uintArrayOf(1u), 0)
        override val MINUSONE = BigDecimal(-1, uintArrayOf(1u), 0)
//        val NaN = BigDecimal(NAN_BIT, UIntArray(0), 0)
//        val POSITIVE_INFINITY = BigDecimal(INFINITY_BIT, NaN.ints, 0)
//        val NEGATIVE_INFINITY = BigDecimal(-1 xor INFINITY_BIT, NaN.ints, 0)

        override fun newInstance(sign: Int, ints: UIntArray, decimalPositions: Long): BigDecimal {
            return BigDecimal(sign, ints, decimalPositions)
        }


    }

}
