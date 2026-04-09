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

    constructor(str: String): this(parse(str))

    val minWidth: Int get() = (data shr 32).toInt()
    val maxWidth: Int get() = data.toInt()

    @Deprecated("Use maxWidth")
    val maxWidth2: Int get() = if (maxWidth == Int.MAX_VALUE) -1 else maxWidth

    override fun toString(): String {
        return when {
            maxWidth < Int.MAX_VALUE -> "$minWidth-$maxWidth"
            else -> "$minWidth"
        }
    }

    companion object {
        private fun parse(str: String): ULong {
            val idx = str.indexOf('-')
            return when {
                idx >= 0 -> {
                    val minString = str.substring(0, idx)
                    val min = if (minString=="*") 0uL else minString.toULong()
                    val maxString = str.substring(idx + 1)
                    val max = if (maxString == "*") Int.MAX_VALUE.toULong() else maxString.toULong()
                    require(max>=min && max>0uL) { "max width ($max) may not be less than min width ($min) "}

                    min.shl(32) or max
                }

                str =="*" -> Int.MAX_VALUE.toULong()
                else -> str.toULong() shl 32
            }
        }
    }
}
