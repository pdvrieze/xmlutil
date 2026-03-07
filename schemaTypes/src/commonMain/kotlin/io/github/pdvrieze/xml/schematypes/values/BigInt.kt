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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.types.IntegerType

@OptIn(ExperimentalUnsignedTypes::class)
class BigInt private constructor(private val sign: Int, private val ints: UIntArray, private val exp: ULong): XsdInteger {
    init {
        require(ints.isNotEmpty()) { "At least one integer must be present" }
        require(sign == -1 || sign == 1) { "Invalid sign" }
    }

    private constructor(r: ParseResult): this(r.sign, r.ints, r.exp)

    constructor(str: String): this(parse(str))

    override val schemaType: IntegerType<XsdInteger>
        get() = IntegerType.Instance

    override fun toLong(): Long {
/*
        if (isPositive)

        return when (exp) {
            0 if ints.size<1 -> ints[0].toULong()
            0 -> ints[0].toLong() + ints[1].toLong()

            else if exp < 0 -> {
                val l =
                    TODO()
            }
            else -> TODO()
        }
*/
        TODO("not implemented")
    }

    override fun toInt(): Int {
        TODO("not implemented")
    }

    override fun compareTo(other: XsdInteger): Int {
        TODO("not implemented")
    }

    override val xmlString: String
        get() = TODO("not implemented")

    private class ParseResult(val sign: Int, val ints: UIntArray, val exp: ULong)

    companion object {
        private fun parse(s: String): ParseResult {
            TODO("not implemented")
        }
    }
}
