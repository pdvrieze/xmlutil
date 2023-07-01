/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

interface VInteger : VDecimal {
    fun toLong(): Long
    fun toInt(): Int

    companion object {
        val ZERO: VInteger = IntInstance(0)

        operator fun invoke(i: Int): VInteger {
            return IntInstance(i)
        }
        operator fun invoke(l: Long): VInteger {
            return LongInstance(l)
        }
    }
}

private class IntInstance(private val i: Int) : VInteger {
    override fun toLong(): Long = i.toLong()

    override fun toInt(): Int = i

    override val xmlString: String get() = i.toString()
}

private class LongInstance(private val l: Long) : VInteger {
    override fun toLong(): Long = l

    override fun toInt(): Int = l.toInt()

    override val xmlString: String get() = l.toString()
}
