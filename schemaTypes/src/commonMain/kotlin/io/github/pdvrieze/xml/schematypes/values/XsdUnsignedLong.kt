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
import io.github.pdvrieze.xml.schematypes.types.UnsignedLongType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdUnsignedLongImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@ExperimentalXmlUtilApi
@Serializable(XsdUnsignedLong.Companion::class)
interface XsdUnsignedLong : XsdNonNegativeInteger {

    override val schemaType: UnsignedLongType<XsdUnsignedLong>

    val uLongValue: ULong

    override fun toLong(): Long = uLongValue.toLong()

    override fun toInt(): Int = uLongValue.toInt()

    override fun toULong(): ULong = uLongValue

    override fun toUInt(): UInt = uLongValue.toUInt()


    override fun plus(other: XsdNonNegativeInteger): XsdNonNegativeInteger {
        if (other !is XsdUnsignedLong) return other.plus(this)
        return XsdUnsignedLongImpl(uLongValue + other.toULong())
    }

    override fun plus(other: ULong): XsdNonNegativeInteger {
        return XsdUnsignedLongImpl(uLongValue + other)
    }

    override fun times(other: XsdNonNegativeInteger): XsdNonNegativeInteger {
        return XsdUnsignedLong(toULong() * other.toULong())
    }

    companion object : SimpleTypeSerializer<XsdUnsignedLong>("xsd.unsignedLong") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdUnsignedLong {
            return XsdUnsignedLongImpl(xmlTrimWhitespace(raw).toULong())
        }

        val ZERO: XsdUnsignedLong = XsdUnsignedLongImpl(0u)

        operator fun invoke(value: ULong): XsdUnsignedLong = XsdUnsignedLongImpl(value)
        operator fun invoke(value: UInt): XsdUnsignedInt = XsdUnsignedInt(value)
    }

}
