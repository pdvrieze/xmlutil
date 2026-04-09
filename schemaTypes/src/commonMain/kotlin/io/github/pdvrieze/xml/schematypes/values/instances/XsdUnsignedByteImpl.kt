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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.types.UnsignedByteType
import io.github.pdvrieze.xml.schematypes.values.XsdDecimal
import io.github.pdvrieze.xml.schematypes.values.XsdInt
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedByte
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedInt
import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
internal class XsdUnsignedByteImpl(override val uByteValue: UByte) : XsdUnsignedByte {
    override val xmlString: String get() = uByteValue.toString()
    override val schemaType: UnsignedByteType<*> get() = UnsignedByteType.Instance

    override fun unaryPlus(): XsdUnsignedByte = this

    override fun toString(): String {
        return "${uByteValue}u"
    }

    override fun hashCode(): Int {
        return uIntValue.hashCode()
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is XsdUnsignedInt -> uIntValue == other.uIntValue
        is XsdInt -> other.intValue>=0 && other.intValue.toUInt() == uIntValue
        is XsdDecimal -> other == this
        else -> false
    }

}
