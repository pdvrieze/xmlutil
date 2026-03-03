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
import io.github.pdvrieze.xml.schematypes.values.instances.XSBigDecimal
import io.github.pdvrieze.xml.schematypes.values.instances.XSDecimalStringImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XSIntImpl
import io.github.pdvrieze.xml.schematypes.values.instances.XSLongImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@ExperimentalXmlUtilApi
@Serializable(XSInteger.Companion::class)
interface XSInteger : XSDecimal {

    override val type: IntegerType<*> get() = IntegerType.Instance

    override fun toLong(): Long
    override fun toInt(): Int

    override fun compareTo(other: XSDecimal): Int = when(other) {
        is XSBigDecimal -> XSDecimalStringImpl(xmlString.toString()).compareTo(other)
        else -> compareTo(other as XSInteger)
    }

    operator fun compareTo(other: XSInteger): Int

    companion object : SimpleTypeSerializer<XSInteger>("xsd.integer"){
        val ZERO: XSInteger = XSIntImpl(0)

        override fun deserialize(raw: String, input: XmlReader?): XSInteger {
            // TODO support integer only type
            return XSDecimal.invoke(raw) as XSInteger
        }

        operator fun invoke(i: Int): XSInteger {
            return XSIntImpl(i)
        }
        operator fun invoke(l: Long): XSInteger {
            return XSLongImpl(l)
        }
    }
}

