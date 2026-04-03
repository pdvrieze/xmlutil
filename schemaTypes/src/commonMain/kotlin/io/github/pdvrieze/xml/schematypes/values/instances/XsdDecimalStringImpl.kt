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

import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.roundToLong

internal class XsdDecimalStringImpl(override val xmlString: String) : XsdBigDecimal {
    override val sign: Int

    init {
        var next = 1
        var isZero = true
        when (xmlString[0]) {
            '-', '+' -> next = 2
            in '0'..'9' -> Unit
            else -> throw NumberFormatException("Decimals start with digit or sign")
        }

        if (next == 2 && xmlString[1] !in '0'..'9') { // sign
            throw NumberFormatException("Decimal signs should be followed by a digit")
        }
        val len = xmlString.length
        while (next < len && xmlString[next] != '.') {
            when (xmlString[next]) {
                '0' -> {}
                in '1'..'9' -> isZero = false
                else -> throw NumberFormatException("Decimals must only contain digits or a single .")
            }
            ++next
        }
        ++next
        while (next < len) {
            when (xmlString[next]){
                '0' -> {}
                in '1'..'9' -> isZero = false
                else -> throw NumberFormatException("Decimal digits (after dot) must only be  digits")
            }
            ++next
        }
        sign = if (isZero) 0 else if (xmlString[0] == '-') -1 else 1
    }

    override fun toLong(): Long {
        return xmlString.toLong()
    }

    override fun toInt(): Int {
        return xmlString.toInt()
    }

    override fun toBigDecimal(): BigDecimal {
        return BigDecimal(xmlString)
    }

    override fun round(precision: Int): XsdDecimal {
        return toBigDecimal().round(precision)
    }

    override fun roundToHalfEven(precision: Int): XsdDecimal {
        return toBigDecimal().roundToHalfEven(precision)
    }

    override fun abs(): XsdDecimal = when {
        xmlString.startsWith('-') -> XsdDecimalStringImpl(xmlString.substring(1))
        else -> this
    }

    override fun unaryMinus(): XsdDecimal = when {
        xmlString.startsWith('-') -> XsdDecimalStringImpl(xmlString.substring(1))
        else -> XsdDecimalStringImpl("-$xmlString")
    }

    override fun round(): XsdInteger {
        val decPos: Int = when {
            xmlString.contains('e', true) -> return XsdInteger(xmlString.toDouble().roundToLong())
            else -> xmlString.lastIndexOf('.')
        }
        return when {
            decPos >= 0 -> XsdInteger(xmlString.substring(0, decPos))
            else -> XsdInteger(xmlString)
        }
    }

    override fun roundToHalfEven(): XsdInteger {
        TODO("not implemented")
    }

    override fun ceiling(): XsdInteger {
        val decPos: Int = when {
            xmlString.contains('e', true) -> return XsdInteger(xmlString.toDouble().nextUp().roundToLong())
            else -> xmlString.lastIndexOf('.')
        }
        return when {
            decPos < 0 -> XsdInteger(xmlString)

            ((decPos + 1) until xmlString.length).all { xmlString[it] == '0' } ->
                XsdInteger(xmlString.substring(0, decPos))

            else -> XsdInteger(xmlString.substring(0, decPos)).plus(XsdInt(1))
        }
    }

    override fun floor(): XsdInteger {
        val decPos: Int = when {
            xmlString.contains('e', true) -> return XsdInteger(xmlString.toDouble().nextDown().roundToLong())
            else -> xmlString.lastIndexOf('.')
        }
        return when {
            decPos < 0 -> XsdInteger(xmlString)

            else -> XsdInteger(xmlString.substring(0, decPos))
        }

    }

    override fun compareTo(other: XsdDecimal): Int = when (other){
        is XsdBigDecimal -> compareTo(other)
        else -> compareTo(XsdDecimalStringImpl(other.xmlString))
    }

    override fun compareTo(other: XsdBigDecimal): Int {
        var left = xmlString
        var right = other.xmlString
        when (left[0]) {
            '-' -> return when {
                right[0] == '-' -> XsdDecimalStringImpl(right.substring(1))
                    .compareTo(XsdDecimalStringImpl(left.substring(1)))

                else -> -1 // We are certainly smaller
            }

            '+' -> left = left.substring(1)
        }

        when (right[0]) {
            '-' -> return 1
            '+' -> right = right.substring(1)
        }
        // At this point there should not be any prefixes anymore.

        val leftDot = left.indexOf('.')
        val rightDot = right.indexOf('.')

        val leftDecimalDigits: Int
        if (leftDot < 0) {
            left = left + '.'
            leftDecimalDigits = 0
        } else {
            leftDecimalDigits = xmlString.length - leftDot
        }

        val rightDecimalDigits: Int
        if (rightDot < 0) {
            right = right + '.'
            rightDecimalDigits = 0
        } else {
            rightDecimalDigits = xmlString.length - rightDot
        }

        val leftRightPad = maxOf(leftDecimalDigits, rightDecimalDigits) - leftDecimalDigits
        val rightRightPad = maxOf(leftDecimalDigits, rightDecimalDigits) - rightDecimalDigits
        if (leftRightPad>0) { left = left.padEnd(left.length+leftRightPad, '0') }
        if (rightRightPad>0) { right = right.padEnd(right.length+rightRightPad, '0') }
        val totalLen = maxOf(left.length, right.length)
        left = left.padStart(totalLen, '0')
        right = right.padStart(totalLen, '0')
        return left.compareTo(right)
    }

    override fun toString(): String = xmlString
}
