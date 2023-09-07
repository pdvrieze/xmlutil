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
 * Represents a node for a '$' sign.
 * Note: In Kotlin we use only the "anchoring bounds" mode when "$" matches the end of a match region.
 * See: http://docs.oracle.com/javase/8/docs/api/java/util/regex/Matcher.html#useAnchoringBounds-boolean-
 */
internal class XREOLSet(val consCounter: Int, val lt: XRAbstractLineTerminator, val multiline: Boolean = false) : XRSimpleSet() {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        val rightBound = testString.length
        val remainingChars = rightBound - startIndex

        when {
            startIndex >= rightBound ||
            remainingChars == 1 && lt.isLineTerminator(testString[startIndex]) ||
            remainingChars == 2 && lt.isLineTerminatorPair(testString[startIndex], testString[startIndex+1]) ||
            multiline && lt.isLineTerminator(testString[startIndex]) -> {
                matchResult.setConsumed(consCounter, 0)
                return next.matches(startIndex, testString, matchResult)
            }
        }
        return -1
    }

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
        val result = matchResult.getConsumed(consCounter) != 0
        matchResult.setConsumed(consCounter, -1)
        return result
    }

    override val name: String
            get()= "<EOL>"
}
