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

package io.github.pdvrieze.xml.schematypes.values.formatters

import io.github.pdvrieze.xml.schematypes.values.UnicodeChar
import nl.adaptivity.xmlutil.QName

open class DecimalFormat(
    val decimalSeparator: UnicodeChar = UnicodeChar('.'),
    val exponentSeparator: UnicodeChar = UnicodeChar('e'),
    val groupingSeparator: UnicodeChar = UnicodeChar(','),
    val percent: UnicodeChar = UnicodeChar('%'),
    val perMille: UnicodeChar = UnicodeChar('\u2030'),
    val zeroDigit: UnicodeChar = UnicodeChar('0'),
    val digit: UnicodeChar = UnicodeChar('#'),
    val minusSign: UnicodeChar = UnicodeChar('-'),
    val patternSeparator: UnicodeChar = UnicodeChar(';'),
    val infinity: String = "Infinity",
    val NaN: String = "NaN",
) {
    class Named(
        val name: QName,
        decimalSeparator: UnicodeChar = UnicodeChar('.'),
        exponentSeparator: UnicodeChar = UnicodeChar('e'),
        groupingSeparator: UnicodeChar = UnicodeChar(','),
        percent: UnicodeChar = UnicodeChar('%'),
        perMille: UnicodeChar = UnicodeChar('\u2030'),
        zeroDigit: UnicodeChar = UnicodeChar('0'),
        digit: UnicodeChar = UnicodeChar('#'),
        minusSign: UnicodeChar = UnicodeChar('-'),
        patternSeparator: UnicodeChar = UnicodeChar(';'),
        infinity: String = "Infinity",
        NaN: String = "NaN",
    ) : DecimalFormat(decimalSeparator, exponentSeparator, groupingSeparator, percent, perMille, zeroDigit, digit, minusSign, patternSeparator, infinity, NaN)
}
