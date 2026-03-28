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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.types.LongType
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import io.github.pdvrieze.xml.schematypes.values.XsdLong
import io.github.pdvrieze.xml.schematypes.values.XsdNonNegativeInteger
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedLong
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

@JvmInline
internal value class XsdLongImpl(override val longValue: Long) : XsdLong {

    constructor(value: CharSequence): this(value.toString().toLong())

    override fun toInt(): Int = longValue.toInt()

    override val xmlString: String get() = longValue.toString()
    override val schemaType: LongType<*> get() = LongType.Instance

    override fun toString(): String = xmlString

    override fun unaryMinus(): XsdLong = XsdLongImpl(-longValue)

    override fun plus(other: XsdInteger): XsdInteger = when (other) {
        is XsdLong -> XsdLongImpl(longValue + other.longValue)
        else -> other.plus(this)
    }

    override fun minus(other: XsdInteger): XsdInteger = when (other) {
        is XsdLong -> XsdLongImpl(longValue - other.longValue)
        else -> other.unaryMinus().plus(this)
    }

    override fun abs(): XsdUnsignedLong {
        return XsdUnsignedLong(longValue.absoluteValue.toULong())
    }

    override fun compareTo(other: XsdInteger): Int = when (other) {
        is XsdNonNegativeInteger -> if (longValue < 0L) -1 else longValue.toULong().compareTo(other.toULong())
        else -> longValue.compareTo(other.toLong())
    }

}
