/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xmlschema.regex.impl

/**
 * Represents node accepting single character from the given char class.
 */
open internal class XRRangeSet(charClass: XRAbstractCharClass, val ignoreCase: Boolean = false) : XRLeafSet() {

    val chars: XRAbstractCharClass = charClass.instance

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        if (ignoreCase) {
            val char = testString[startIndex]
            return if (chars.contains(char.uppercaseChar()) || chars.contains(char.lowercaseChar())) 1 else -1
        } else {
            return if (chars.contains(testString[startIndex])) 1 else -1
        }
    }

    override val name: String
        get() = "range:" + (if (chars.alt) "^ " else " ") + chars.toString()

    override fun first(set: XRAbstractSet): Boolean {
        @Suppress("DEPRECATION")
        return when (set) {
            is XRCharSet -> XRAbstractCharClass.intersects(chars, set.char.toInt())
            is XRRangeSet -> XRAbstractCharClass.intersects(chars, set.chars)
            is XRSupplementaryRangeSet -> XRAbstractCharClass.intersects(chars, set.chars)
            else -> true
        }
    }
}
