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
import io.github.pdvrieze.xml.schematypes.types.NegativeIntegerType
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable(XsdNegativeInteger.Companion::class)
interface XsdNegativeInteger : XsdNonPositiveInteger {

    override val schemaType: NegativeIntegerType<XsdNegativeInteger>
        get() = NegativeIntegerType.Instance

    override fun toBigInt(): XsdNegativeInteger = this

    fun coerceAtMost(maxMax: XsdNegativeInteger): XsdNegativeInteger = when {
        this < maxMax -> this
        else -> maxMax
    }

    operator fun plus(other: XsdNegativeInteger): XsdNegativeInteger

    operator fun times(other: XsdNegativeInteger): XsdNonNegativeInteger

    override fun compareTo(other: XsdInteger): Int

    operator fun compareTo(other: XsdNegativeInteger): Int

    override fun plus(other: XsdInteger): XsdInteger

    override fun unaryMinus(): XsdPositiveInteger

    companion object : SimpleTypeSerializer<XsdNegativeInteger>("xsd.nonNegativeInteger") {
        override fun deserialize(raw: String, input: XmlReader?): XsdNegativeInteger {
            return invoke(xmlTrimWhitespace(raw))
        }

        val MINUSONE: XsdNegativeInteger = XsdNegativeInteger(-1)

        operator fun invoke(charSequence: CharSequence): XsdNegativeInteger =
            invoke(rawValue = charSequence.toString())

        operator fun invoke(rawValue: String): XsdNegativeInteger {
            return BigNegativeInt(rawValue)
        }

        operator fun invoke(value: Long): XsdNegativeInteger = BigNegativeInt(value)
        operator fun invoke(value: Int): XsdNegativeInteger = BigNegativeInt(value)

    }
}
