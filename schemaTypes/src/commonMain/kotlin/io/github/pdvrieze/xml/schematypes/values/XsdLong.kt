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

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.LongType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.math.absoluteValue

@Serializable(XsdLong.Companion::class)
@XmlUtilInternal
interface XsdLong : XsdInteger {

    override val schemaType: LongType<XsdLong>

    val longValue: Long

    override fun toLong(): Long = longValue

    override val size: ULong get() = 2uL
    override val sign: Int get() = longValue.compareTo(0L)

    override fun abs(): XsdUnsignedLong

    override fun get(index: Int): UInt {
        when (index) {
            0 -> return longValue.absoluteValue.toUInt()
            1 -> return longValue.absoluteValue.toULong().shr(32).toUInt()
            else -> throw IndexOutOfBoundsException("Index $index out of bounds")
        }
    }

    override fun get(index: ULong): UInt {
        when (index) {
            0uL -> return longValue.absoluteValue.toUInt()
            1uL -> return longValue.absoluteValue.toULong().shr(32).toUInt()
            else -> throw IndexOutOfBoundsException("Index $index out of bounds")
        }
    }

    override fun countTrailingZeroBits(): ULong {
        return longValue.countTrailingZeroBits().toULong()
    }

    companion object : SimpleTypeSerializer<XsdLong>("XSLong") {
        operator fun invoke(value: Long): XsdLong = XsdLongImpl(value)

        override fun deserialize(raw: String, input: XmlReader?): XsdLong {
            return XsdLongImpl(raw.toLong())
        }
    }

}
