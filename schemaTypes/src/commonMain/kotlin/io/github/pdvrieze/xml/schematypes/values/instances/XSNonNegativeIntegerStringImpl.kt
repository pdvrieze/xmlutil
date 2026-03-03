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

import io.github.pdvrieze.xml.schematypes.types.NonNegativeIntegerType
import io.github.pdvrieze.xml.schematypes.values.XSNonNegativeInteger
import io.github.pdvrieze.xml.schematypes.values.XSUnsignedLong
import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
internal class XSNonNegativeIntegerStringImpl(override val xmlString: String) : XSNonNegativeInteger {
    override val schemaType: NonNegativeIntegerType<*> get() = NonNegativeIntegerType.Instance
    override fun toLong(): Long = xmlString.toLong()

    override fun toInt(): Int = xmlString.toInt()

    override fun toULong(): ULong = xmlString.toULong()

    override fun toUInt(): UInt = xmlString.toUInt()

    override fun plus(other: XSNonNegativeInteger): XSNonNegativeInteger {
        return XSUnsignedLong.Companion(toULong() + other.toULong())
    }

    override fun plus(other: ULong): XSNonNegativeInteger {
        return XSUnsignedLong.Companion(toULong() + other)
    }

    override fun times(other: XSNonNegativeInteger): XSNonNegativeInteger {
        return XSUnsignedLong.Companion(toULong() * other.toULong())
    }

    override fun compareTo(other: XSNonNegativeInteger): Int {
        val maybeULong = xmlString.toULongOrNull()
        val otherStr: String
        if (maybeULong!=null) {
            if (other is XSUnsignedLong) return maybeULong.compareTo(other.toULong())
            otherStr = other.xmlString.toString()
            val maybeOtherULong = otherStr.toULongOrNull()
            if (maybeOtherULong != null) return maybeULong.compareTo(maybeOtherULong)
        } else {
            otherStr = other.xmlString.toString()
        }

        val len = maxOf(xmlString.length, otherStr.length)
        val newLeft = xmlString.padStart(len, '0')
        val newRight = otherStr.padStart(len, '0')
        return newLeft.compareTo(newRight)
    }

    override fun toString(): String = "${xmlString}u"
}
