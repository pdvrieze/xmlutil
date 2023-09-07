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
 * Valid constant zero character match.
 */
internal class XREmptySet(override var next: XRAbstractSet) : XRLeafSet() {

    override val charCount = 0

    override fun accepts(startIndex: Int, testString: CharSequence): Int = 0

    override fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        for (index in startIndex..testString.length) {
            if (next.matches(index, testString, matchResult) >= 0) {
                return index
            }
        }
        return -1
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int {
        for (index in rightLimit downTo leftLimit) {
            if (next.matches(index, testString, matchResult) >= 0) {
                return index
            }
        }
        return -1
    }


    override val name: String
            get()= "<Empty set>"

    override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean {
        return false
    }

}
