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

package io.github.pdvrieze.xml.schematypes

import io.github.pdvrieze.xml.schematypes.values.XsdBase64Binary

interface Collation : Comparator<String> {
    val uri: String

    fun equals(left: String, right: String): Boolean = compare(left, right) == 0

    fun key(key: String): XsdBase64Binary

    fun indexOf(key: String, value: String, startPos: Int = 0): Int {
        return indexOfImpl(key, value, startPos)
    }

    fun contains(key: String, value: String): Boolean = indexOf(key, value) >= 0

    enum class MaxVariable(val text: String) {
        SPACE("space"),
        PUNCT("punct"),
        SYMBOL("symbol"),
        CURRENCY("currency"),
        ;
    }

    enum class Alternate(val text: String) {
        NON_IGNORABLE("non-ignorable"),
        SHIFTED("shifted"),
        BLANKED("blanked"),
        ;
    }

    enum class CaseFirst(val text: String) {
        UPPER("upper"),
        LOWER("lower"),
        ;
    }
}

internal tailrec fun Collation.indexOfImpl(key: String, value: String, startPos: Int = 0): Int {
    if (startPos + key.length > value.length) return -1

    if (compare(key, value.substring(startPos, startPos + key.length)) == 0) return startPos

    return indexOfImpl(key, value, startPos + 1)
}
