/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.core.kxio

import kotlinx.io.Sink
import kotlinx.io.writeCodePointValue

internal class SinkAppendable(private val target: Sink) : Appendable {
    override fun append(value: Char): Appendable {
        target.writeCodePointValue(value.code)
        return this
    }

    override fun append(value: CharSequence?): Appendable {
        return append(value, 0, value?.length ?: 4)
    }

    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable {
        if (value == null) {
            return append("null", startIndex, endIndex)
        } else {
            var pendingSurrogate: Char = 0.toChar()
            for (idx in startIndex until endIndex) {
                val c = value[idx]
                when {
                    c.isHighSurrogate() -> pendingSurrogate = c

                    c.isLowSurrogate() -> {
                        val cp = 0x10000 + ((pendingSurrogate.code and 0x3ff).shl(10)) or (c.code and 0x3ff)
                        target.writeCodePointValue(cp)
                        pendingSurrogate = 0.toChar()
                    }

                    else -> target.writeCodePointValue(c.code)
                }
            }
            require(pendingSurrogate == 0.toChar()) { "High surrogate at end of string" }
            return this
        }
    }
}
