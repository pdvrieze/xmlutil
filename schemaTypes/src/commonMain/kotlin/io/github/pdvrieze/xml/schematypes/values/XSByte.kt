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
import io.github.pdvrieze.xml.schematypes.values.instances.XSByteImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader

@Serializable(XSByte.Companion::class)
@ExperimentalXmlUtilApi
interface XSByte : XSShort {
    val byteValue: Byte
    override val shortValue: Short get() = byteValue.toShort()
    override val intValue: Int get() = shortValue.toInt()
    override val longValue: Long get() = shortValue.toLong()

    companion object : SimpleTypeSerializer<XSByte>("xsd.byte") {
        operator fun invoke(value: Byte): XSByte = XSByteImpl(value)

        override fun deserialize(raw: String, input: XmlReader?): XSByte {
            return XSByteImpl(raw.toByte())
        }
    }
}

