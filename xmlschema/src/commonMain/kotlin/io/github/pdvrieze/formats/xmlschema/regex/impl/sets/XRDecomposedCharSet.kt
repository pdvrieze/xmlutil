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

/** Represents canonical decomposition of Unicode character. Is used when CANON_EQ flag of Pattern class is specified. */
open internal class XRDecomposedCharSet(
    /** Decomposition of the Unicode codepoint */
    private val decomposedChar: IntArray,
    /** Length of useful part of decomposedChar decomposedCharLength <= decomposedChar.length */
    private val decomposedCharLength: Int
) : XRSimpleSet() {

    /** Contains information about number of chars that were read for a codepoint last time */
    private var readCharsForCodePoint = 1

    /** UTF-16 encoding of decomposedChar. Regexes processing is not multi-threaded. */
    private val decomposedCharUTF16: String by lazy(LazyThreadSafetyMode.NONE) {
        val strBuff = StringBuilder()

        for (i in 0..decomposedCharLength - 1) {
            strBuff.append(Char.toChars(decomposedChar[i]))
        }
        return@lazy strBuff.toString()
    }

    override val consumesFixedLength: Boolean
        get() = true

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var strIndex = startIndex
        val rightBound = testString.length

        if (strIndex >= rightBound) {
            return -1
        }

        // We read testString and decompose it gradually to compare with this decomposedChar at position strIndex
        var curChar = codePointAt(strIndex, testString, rightBound)
        strIndex += readCharsForCodePoint
        var readCodePoints = 0
        var i = 0
        // All decompositions have length that is less or equal Lexer.MAX_DECOMPOSITION_LENGTH
        var decomposedCodePoint: IntArray = IntArray(XRLexer.MAX_DECOMPOSITION_LENGTH)
        readCodePoints += decomposeCodePoint(curChar, decomposedCodePoint, readCodePoints)

        if (strIndex < rightBound) {
            curChar = codePointAt(strIndex, testString, rightBound)

            // Read testString until we met a decomposed char boundary and decompose obtained portion of testString.
            while (readCodePoints < XRLexer.MAX_DECOMPOSITION_LENGTH && !XRLexer.isDecomposedCharBoundary(curChar)) {

                if (!XRLexer.hasDecompositionNonNullCanClass(curChar)) {
                    decomposedCodePoint[readCodePoints++] = curChar
                } else {
                    /*
                     * A few codepoints have decompositions and non null canonical classes, we have to take them into
                     * consideration, but general rule is: if canonical class != 0 then no decomposition
                     */
                    readCodePoints += decomposeCodePoint(curChar, decomposedCodePoint, readCodePoints)
                }

                strIndex += readCharsForCodePoint

                if (strIndex < rightBound) {
                    curChar = codePointAt(strIndex, testString, rightBound)
                } else {
                    break
                }
            }
        }

        // Some optimization since length of decomposed char is <= 3 usually
        when (readCodePoints) {
            0, 1, 2 -> {}

            3 -> {
                var i1 = XRLexer.getCanonicalClass(decomposedCodePoint[1])
                val i2 = XRLexer.getCanonicalClass(decomposedCodePoint[2])

                if (i2 != 0 && i1 > i2) {
                    i1 = decomposedCodePoint[1]
                    decomposedCodePoint[1] = decomposedCodePoint[2]
                    decomposedCodePoint[2] = i1
                }
            }

            else -> decomposedCodePoint = XRLexer.getCanonicalOrder(decomposedCodePoint, readCodePoints)
        }

        // Compare decomposedChar with decomposed char that was just read from testString
        if (readCodePoints != decomposedCharLength) {
            return -1
        }

        if ((0 until readCodePoints).firstOrNull { decomposedCodePoint[i] != decomposedChar[i] } != null) {
            return -1
        }
        return next.matches(strIndex, testString, matchResult)
    }

    override val name: String
        get() = "decomposed char: $decomposedChar"

    /** Reads Unicode codepoint from [testString] starting from [strIndex] until [rightBound]. */
    fun codePointAt(strIndex: Int, testString: CharSequence, rightBound: Int): Int {
        var index = strIndex

        // We store information about number of codepoints we read at variable readCharsForCodePoint.
        val curChar: Int
        readCharsForCodePoint = 1
        if (index < rightBound - 1) {
            val high = testString[index++]
            val low = testString[index]

            if (Char.isSurrogatePair(high, low)) {
                curChar = Char.toCodePoint(high, low)
                readCharsForCodePoint = 2
            } else {
                @Suppress("DEPRECATION")
                curChar = high.toInt()
            }
        } else {
            @Suppress("DEPRECATION")
            curChar = testString[index].toInt()
        }

        return curChar
    }

    override fun first(set: XRAbstractSet): Boolean {
        return if (set is XRDecomposedCharSet)
            set.decomposedChar.contentEquals(decomposedChar)
        else
            true
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean = true
}


