/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

interface VDecimal : VAnyAtomicType {
    fun toLong(): Long
    fun toInt(): Int
    fun toDouble(): Double = xmlString.toDouble()
    fun toVDecimal(): VBigDecimal = VBigDecimalImpl(xmlString)

    operator fun compareTo(other: VDecimal): Int
}

interface VBigDecimal : Comparable<VDecimal>, VDecimal {
    val isInteger: Boolean get() = '.' !in xmlString

    override fun toVDecimal(): VBigDecimal = this

    operator fun compareTo(other: VBigDecimal): Int
}

internal class VBigDecimalImpl(override val xmlString: String) : VBigDecimal {

    init {
        var next = 1
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
            if (xmlString[next] !in '0'..'9') { // sign
                throw NumberFormatException("Decimals must only contain digits or a single .")
            }
            ++next
        }
        ++next
        while (next < len) {
            if (xmlString[next] !in '0'..'9') { // sign
                throw NumberFormatException("Decimal digits (after dot) must only be  digits")
            }
            ++next
        }
    }

    override fun toLong(): Long {
        return xmlString.toLong()
    }

    override fun toInt(): Int {
        return xmlString.toInt()
    }

    override fun compareTo(other: VDecimal): Int = when (other){
        is VBigDecimal -> compareTo(other)
        else -> compareTo(VBigDecimalImpl(other.xmlString))
    }

    override fun compareTo(other: VBigDecimal): Int {
        var left = xmlString
        var right = other.xmlString
        when (left[0]) {
            '-' -> return when {
                right[0] == '-' -> VBigDecimalImpl(right.substring(1))
                    .compareTo(VBigDecimalImpl(left.substring(1)))

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
