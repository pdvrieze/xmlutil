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
import io.github.pdvrieze.xml.schematypes.types.IntegerType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdBigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XsdDecimalStringImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdIntImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XsdLongImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XsdInteger.Companion::class)
interface XsdInteger : XsdDecimal {

    override val schemaType: IntegerType<XsdInteger>

    override fun toLong(): Long
    override fun toInt(): Int

    override fun compareTo(other: XsdDecimal): Int = when (other) {
        is XsdBigDecimal -> XsdDecimalStringImpl(xmlString).compareTo(other)
        else -> compareTo(other as XsdInteger)
    }

    operator fun compareTo(other: XsdInteger): Int

    companion object : SimpleTypeSerializer<XsdInteger>("xsd.integer") {
        val ZERO: XsdInteger = XsdIntImpl(0)

        override fun deserialize(raw: String, input: XmlReader?): XsdInteger {
            // TODO support integer only type
            return XsdDecimal.invoke(raw) as XsdInteger
        }

        operator fun invoke(i: Int): XsdInteger {
            return XsdIntImpl(i)
        }

        operator fun invoke(l: Long): XsdInteger {
            return XsdLongImpl(l)
        }
    }
}

