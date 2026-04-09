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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import kotlin.jvm.JvmInline

@JvmInline
@ExperimentalXmlUtilApi
value class WidthModifier private constructor(val data: ULong) {
    constructor(minWidth: Int, maxWidth: Int = Int.MAX_VALUE) : this(minWidth.toULong() shl 32 or maxWidth.toULong()) {
        require(maxWidth >= minWidth && maxWidth>0) { "max width ($maxWidth) may not be less than min width ($minWidth) " }
    }

    constructor(): this (ULong.MAX_VALUE)

    constructor(str: String): this(parse(str))
    val isSpecified: Boolean get() = data != ULong.MAX_VALUE
    val isMaxSpecified: Boolean get() = (maxWidth and Int.MAX_VALUE) != Int.MAX_VALUE

    val minWidth: Int get() = (data shr 32).toUInt().let { if (it == UInt.MAX_VALUE) 0 else it.toInt() }
    val maxWidth: Int get() = data.toInt() and Int.MAX_VALUE

    override fun toString(): String {
        return when (data.toUInt()) {
            0xFFFF_FFFFu -> "<unspecified>"
            0x7FFF_FFFFu -> minWidth.toString()
            0xFFFF_FFFEu -> when ((data shr 32).toUInt()) {
                0xFFFF_FFFFu -> "*-*"
                else -> "$minWidth-*"
            }
            else -> "$minWidth-$maxWidth"
        }
    }

    companion object {
        private fun parse(str: String): ULong {
            val idx = str.indexOf('-')
            // note that for '*' we use intMax -1 as the max width meaning that MAX_VALUE can be used as unspecified
            // but this value should never be reached either so it works as infinity
            return when {
                idx >= 0 -> {
                    val minString = str.substring(0, idx)
                    val min = if (minString=="*") 0uL else minString.toULong()
                    val maxString = str.substring(idx + 1)
                    val max = if (maxString == "*") (Int.MAX_VALUE - 1).toULong() else maxString.toULong()
                    require(max>=min && max>0uL) { "max width ($max) may not be less than min width ($min) "}

                    min.shl(32) or max
                }

                str =="*" -> (Int.MAX_VALUE - 1).toULong()
                else -> str.toULong() shl 32 or Int.MAX_VALUE.toULong()
            }
        }
    }
}
