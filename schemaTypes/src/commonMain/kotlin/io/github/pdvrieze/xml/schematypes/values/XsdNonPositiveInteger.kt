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
import io.github.pdvrieze.xml.schematypes.types.NonPositiveIntegerType
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable(XsdNonPositiveInteger.Companion::class)
interface XsdNonPositiveInteger : XsdInteger {

    override val schemaType: NonPositiveIntegerType<XsdNonPositiveInteger>
        get() = NonPositiveIntegerType.Instance

    fun toULong(): ULong

    fun toUInt(): UInt

    override fun toBigInt(): XsdNonPositiveInteger = this

    fun coerceAtMost(maxMax: XsdNonPositiveInteger): XsdNonPositiveInteger = when {
        this < maxMax -> this
        else -> maxMax
    }

    operator fun plus(other: XsdNonPositiveInteger): XsdNonPositiveInteger

    operator fun times(other: XsdNonPositiveInteger): XsdNonNegativeInteger

    override fun compareTo(other: XsdInteger): Int {
        if (other.sign < sign) return 1
        return abs().compareTo(other.abs())
    }

    operator fun compareTo(other: XsdNonPositiveInteger): Int

    operator fun plus(other: ULong): XsdNonPositiveInteger
    override fun plus(other: XsdInteger): XsdInteger

    override fun unaryMinus(): XsdNonNegativeInteger

    companion object : SimpleTypeSerializer<XsdNonPositiveInteger>("xsd.nonNegativeInteger") {
        override fun deserialize(raw: String, input: XmlReader?): XsdNonPositiveInteger {
            return invoke(xmlTrimWhitespace(raw))
        }

        val MINUSONE = BigNonPositiveInt(-1)
        val ZERO = BigNonPositiveInt(0)

        operator fun invoke(charSequence: CharSequence): XsdNonPositiveInteger =
            invoke(rawValue = charSequence.toString())

        operator fun invoke(rawValue: String): XsdNonPositiveInteger {
            return BigNonPositiveInt(rawValue)
        }

        operator fun invoke(value: Long): XsdNonPositiveInteger = BigNonPositiveInt(value)
        operator fun invoke(value: Int): XsdNonPositiveInteger = BigNonPositiveInt(value)

    }
}
