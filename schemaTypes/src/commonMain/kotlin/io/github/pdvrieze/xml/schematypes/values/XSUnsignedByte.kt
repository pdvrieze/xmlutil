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
import io.github.pdvrieze.xml.schematypes.types.UnsignedByteType
import io.github.pdvrieze.xml.schematypes.values.instances.XSUnsignedByteImpl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlTrimWhitespace

@Serializable(XSUnsignedByte.Companion::class)
interface XSUnsignedByte : XSUnsignedShort {

    override val type: UnsignedByteType<*> get() = UnsignedByteType.Instance

    val uByteValue: UByte
    override val uShortValue: UShort get() = uByteValue.toUShort()
    override val uIntValue: UInt get() = uByteValue.toUInt()
    override val uLongValue: ULong get() = uByteValue.toULong()

    override fun toInt(): Int = uByteValue.toInt()

    override fun toLong(): Long = uByteValue.toLong()

    override fun toUInt(): UInt = uByteValue.toUInt()

    override fun toULong(): ULong = uByteValue.toULong()

    companion object : SimpleTypeSerializer<XSUnsignedByte>("xsd.unsignedLong") {
        override fun deserialize(raw: String, input: XmlReader?): XSUnsignedByte {
            return XSUnsignedByteImpl(xmlTrimWhitespace(raw).toUByte())
        }

        operator fun invoke(value: UByte): XSUnsignedByte = XSUnsignedByteImpl(value)
    }

}
