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
import io.github.pdvrieze.xml.schematypes.values.XsdFloat
import io.github.pdvrieze.xml.schematypes.values.XsdInteger


@OptIn(ExperimentalUnsignedTypes::class)
class BigDecimal(ints: UIntArray, exponent: Int) :
    AbstractBigDecimal<BigDecimal>(ints, exponent) {

    init {
        val specialSign = ints[0] and SPECIAL_BIT.toUInt()
        requireRange(specialSign == 0u) {
            "BigDecimal does not support special values (INF, -INF, NaN)"
        }
    }

    constructor(value: CharSequence) : this(parse(value))

    constructor(float: XsdFloat) : this(float.value)
    constructor(float: Float) : this(convertToDecimal(float))
    constructor(double: Double) : this(convertToDecimal(double))

    constructor(bigDecimal: InfBigDecimal): this(bigDecimal.ints, bigDecimal.exponent)

    private constructor(pr: ParseResult) : this(pr.ints, pr.decimalDigits)

    constructor(value: UInt) : this(ints = valToUInts(value), exponent = 0)

    constructor(value: ULong) : this(ints = valToUInts(value), exponent = 0)

    override val self: BigDecimal get() = this
    override val isNaN: Boolean get() = false
    override val isInfinity: Boolean get() = false
    override val isNegativeInfinity: Boolean get() = false

    override val companion: Companion get() = Companion

    override fun XsdDecimal.asT(): BigDecimal = when (this) {
        is BigDecimal -> this
        is AbstractBigDecimal<*> -> {
            requireRange(isFinite) { "Cannot convert non-finite XsdBigDecimal to BigDecimal" }
            BigDecimal(ints, exponent)
        }
        is XsdInteger -> BigDecimal(UIntArray(size.toInt()) { get(it) }, 0)
        else -> BigDecimal(xmlString) // fallback to parsing
    }

    companion object : CompanionBase<BigDecimal>() {
        override val ZERO = BigDecimal(uintArrayOf(0u), 0)
        override val NEGZERO = BigDecimal(uintArrayOf(0x8000_0000u), 0)
        override val ONE = BigDecimal(uintArrayOf(1u), 0)
        override val MINUSONE = BigDecimal(uintArrayOf(1u), 0)
//        val NaN = BigDecimal(NAN_BIT, UIntArray(0), 0)
//        val POSITIVE_INFINITY = BigDecimal(INFINITY_BIT, NaN.ints, 0)
//        val NEGATIVE_INFINITY = BigDecimal(-1 xor INFINITY_BIT, NaN.ints, 0)

        override fun newInstance(ints: UIntArray, exponent: Int): BigDecimal {
            return BigDecimal(ints, exponent)
        }


    }

}
