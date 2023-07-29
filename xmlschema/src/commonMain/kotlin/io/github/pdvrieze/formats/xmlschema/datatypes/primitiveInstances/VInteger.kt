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

import kotlin.jvm.JvmInline

interface VInteger : VDecimal {
    override fun toLong(): Long
    override fun toInt(): Int

    override fun compareTo(other: VDecimal): Int = when(other) {
        is VBigDecimal -> VBigDecimalImpl(xmlString).compareTo(other)
        else -> compareTo(other as VInteger)
    }

    operator fun compareTo(other: VInteger): Int

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

    override fun toString(): String = xmlString

    override fun compareTo(other: VInteger): Int = when (other) {
        is VNonNegativeInteger -> if (i<0) -1 else i.toULong().compareTo(other.toULong())
        else -> i.toLong().compareTo(other.toLong())
    }
}

private class LongInstance(private val l: Long) : VInteger {
    override fun toLong(): Long = l

    override fun toInt(): Int = l.toInt()

    override val xmlString: String get() = l.toString()

    override fun toString(): String = xmlString


    override fun compareTo(other: VInteger): Int = when (other) {
        is VNonNegativeInteger -> if (l < 0L) -1 else l.toULong().compareTo(other.toULong())
        else -> l.compareTo(other.toLong())
    }

}

@JvmInline
value class VDouble(val value: Double): VAnyAtomicType {
    override val xmlString: String get() = value.toString()

    override fun toString(): String = xmlString
}

@JvmInline
value class VFloat(val value: Float): VAnyAtomicType {
    override val xmlString: String get() = value.toString()

    override fun toString(): String = xmlString
}
