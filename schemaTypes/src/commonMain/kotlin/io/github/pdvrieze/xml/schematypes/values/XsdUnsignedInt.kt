/*
 * Copyright (c) 2021-2026.
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
import io.github.pdvrieze.xml.schematypes.types.UnsignedIntType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdUnsignedIntImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@Serializable(XsdUnsignedInt.Companion::class)
interface XsdUnsignedInt : XsdUnsignedLong {

    override val schemaType: UnsignedIntType<XsdUnsignedInt>

    val uIntValue: UInt
    override val uLongValue: ULong
        get() = uIntValue.toULong()

    override fun toInt(): Int = uIntValue.toInt()

    override fun toLong(): Long = uIntValue.toLong()

    override fun toUInt(): UInt = uIntValue

    override fun toULong(): ULong = uIntValue.toULong()


    override fun plus(other: XsdNonNegativeInteger): XsdNonNegativeInteger {
        if (other !is XsdUnsignedInt) return other.plus(this)
        return XsdUnsignedIntImpl(uIntValue + other.toUInt())
    }

    override fun plus(other: ULong): XsdUnsignedInt {
        return XsdUnsignedIntImpl(uIntValue + other.toUInt())
    }

    override fun times(other: XsdNonNegativeInteger): XsdNonNegativeInteger {
        return XsdUnsignedLong(toULong() * other.toULong())
    }


    companion object : SimpleTypeSerializer<XsdUnsignedInt>("xsd.unsignedLong") {
        override fun deserialize(raw: String, input: XmlReader?): XsdUnsignedInt {
            return XsdUnsignedIntImpl(xmlTrimWhitespace(raw).toUInt())
        }

        operator fun invoke(value: UInt): XsdUnsignedInt = XsdUnsignedIntImpl(value)
    }

}

