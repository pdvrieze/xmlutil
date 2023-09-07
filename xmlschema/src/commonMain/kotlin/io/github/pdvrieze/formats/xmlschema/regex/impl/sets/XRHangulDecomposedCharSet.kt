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

@file:Suppress("DEPRECATION") // Char.toInt()
package io.github.pdvrieze.formats.xmlschema.regex.impl

import kotlin.text.*

/**
 * Represents canonical decomposition of Hangul syllable. Is used when
 * CANON_EQ flag of Pattern class is specified.
 */
internal class XRHangulDecomposedCharSet(
        /**
         * Decomposed Hangul syllable.
         */
        private val decomposedChar: CharArray,
        /**
         * Length of useful part of decomposedChar
         * decomposedCharLength <= decomposedChar.length
         */
        private val decomposedCharLength: Int
) : XRSimpleSet() {

    /**
     * String representing syllable
     */
    private val decomposedCharUTF16: String by lazy {
        decomposedChar.concatToString(0, decomposedChar.size)
    }

    override val name: String
            get() = "decomposed Hangul syllable: $decomposedCharUTF16"

    override val consumesFixedLength: Boolean
        get() = true

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        var index = startIndex

        /*
         * All decompositions for Hangul syllables have length that
         * is less or equal Lexer.MAX_DECOMPOSITION_LENGTH
         */
        val rightBound = testString.length
        var SyllIndex = 0
        val decompSyllable = IntArray(XRLexer
                .MAX_HANGUL_DECOMPOSITION_LENGTH)
        val decompCurSymb: IntArray?
        var curSymb: Char

        /*
         * For details about Hangul composition and decomposition see
         * http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
         * "3.12 Conjoining Jamo Behavior"
         */
        var LIndex: Int
        var VIndex = -1
        var TIndex = -1

        if (index >= rightBound) {
            return -1
        }
        curSymb = testString[index++]
        decompCurSymb = XRLexer.getHangulDecomposition(curSymb.toInt())

        if (decompCurSymb == null) {

            /*
             * We deal with ordinary letter or sequence of jamos
             * at index at testString.
             */
            decompSyllable[SyllIndex++] = curSymb.toInt()
            LIndex = curSymb.toInt() - XRLexer.LBase

            if (LIndex < 0 || LIndex >= XRLexer.LCount) {

                /*
                 * Ordinary letter, that doesn't match this
                 */
                return -1
            }

            if (index < rightBound) {
                curSymb = testString[index]
                VIndex = curSymb.toInt() - XRLexer.VBase
            }

            if (VIndex < 0 || VIndex >= XRLexer.VCount) {

                /*
                 * Single L jamo doesn't compose Hangul syllable,
                 * so doesn't match
                 */
                return -1
            }
            index++
            decompSyllable[SyllIndex++] = curSymb.toInt()

            if (index < rightBound) {
                curSymb = testString[index]
                TIndex = curSymb.toInt() - XRLexer.TBase
            }

            if (TIndex < 0 || TIndex >= XRLexer.TCount) {

                /*
                 * We deal with LV syllable at testString, so
                 * compare it to this
                 */
                return if (decomposedCharLength == 2
                        && decompSyllable[0] == decomposedChar[0].toInt()
                        && decompSyllable[1] == decomposedChar[1].toInt())
                    next.matches(index, testString, matchResult)
                else
                    -1
            }
            index++
            decompSyllable[@Suppress("UNUSED_CHANGED_VALUE")SyllIndex++] = curSymb.toInt()

            /*
             * We deal with LVT syllable at testString, so
             * compare it to this
             */
            return if (decomposedCharLength == 3
                    && decompSyllable[0] == decomposedChar[0].toInt()
                    && decompSyllable[1] == decomposedChar[1].toInt()
                    && decompSyllable[2] == decomposedChar[2].toInt())
                next.matches(index, testString, matchResult)
            else
                -1
        } else {

            /*
             * We deal with Hangul syllable at index at testString.
             * So we decomposed it to compare with this.
             */
            var i = 0

            if (decompCurSymb.size != decomposedCharLength) {
                return -1
            }

            while (i < decomposedCharLength) {
                if (decompCurSymb[i] != decomposedChar[i].toInt()) {
                    return -1
                }
                i++
            }
            return next.matches(index, testString, matchResult)
        }
    }

    override fun first(set: XRAbstractSet): Boolean {
        return if (set is XRHangulDecomposedCharSet)
            set.decomposedCharUTF16 == decomposedCharUTF16
        else
            true
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
        return true
    }
}

