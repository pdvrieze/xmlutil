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

package io.github.pdvrieze.formats.xpath.impl.token

import io.github.pdvrieze.formats.xpath.impl.XPathInternal

internal sealed interface Token {
    val isDelimiting: Boolean

    companion object {
        @XPathInternal
        public fun isDelimStart(c: Int): Boolean = (c and DELIMSTARTMASK) == 0 && DELIMSTARTCHAR[c]

        @XPathInternal
        public fun isDelimStart(c: Char): Boolean =
            c.code.let { code -> ((code and DELIMSTARTMASK) == 0) && DELIMSTARTCHAR[code] }

        private val DELIMSTARTMASK:Int = 0x7FFF_FF70
        private val DELIMSTARTCHAR = BooleanArray(0x7f)
        private val DELIMORWSCHAR: BooleanArray

        init {
            for (c in arrayOf('!', '"', '#', '$', '(', ')', '*', '+', ',', ',', '-', '.',
                '/', ':', '<', '=', '=', '>', '?', '@', '[', '\'', ']', '{', '|', '|', '}')) {
                DELIMSTARTCHAR[c.code] = true
            }
            DELIMORWSCHAR = DELIMSTARTCHAR.copyOf()
            DELIMORWSCHAR[0xA] = true
            DELIMORWSCHAR['\t'.code] = true
            DELIMORWSCHAR[' '.code] = true
        }
    }
}
