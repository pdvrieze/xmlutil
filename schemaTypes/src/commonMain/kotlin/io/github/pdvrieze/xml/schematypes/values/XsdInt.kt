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
import io.github.pdvrieze.xml.schematypes.types.IntType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdIntImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.math.absoluteValue

@Serializable(XsdInt.Companion::class)
@XmlUtilInternal
interface XsdInt : XsdLong {

    override val schemaType: IntType<XsdInt>

    val intValue: Int
    override val longValue: Long get() = intValue.toLong()
    override fun toInt(): Int = intValue

    override fun toBigInt(): XsdInteger = BigInt(intValue)

    override val size: ULong get() = 1uL

    override val sign: Int get() = intValue.compareTo(0)

    override fun abs(): XsdUnsignedInt

    override fun get(index: Int): UInt {
        if (index != 0) throw IndexOutOfBoundsException("Index $index out of bounds")
        return intValue.absoluteValue.toUInt()
    }

    override fun get(index: ULong): UInt {
        if (index != 0uL) throw IndexOutOfBoundsException("Index $index out of bounds")
        return intValue.absoluteValue.toUInt()
    }

    override fun countTrailingZeroBits(): ULong {
        return intValue.countTrailingZeroBits().toULong()
    }


    override fun significantBitsFromZero(): ULong {
        return 32u - intValue.countLeadingZeroBits().toULong()
    }


    companion object : SimpleTypeSerializer<XsdInt>("xsd.int") {
        operator fun invoke(value: Int): XsdInt = XsdIntImpl(value)

        override fun deserialize(raw: String, input: XmlReader?): XsdInt {
            return XsdIntImpl(raw.toInt())
        }
    }

}
