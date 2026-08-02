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

import io.github.pdvrieze.xml.schematypes.impl.ListHelper
import io.github.pdvrieze.xml.schematypes.types.HexBinaryType
import io.github.pdvrieze.xml.schematypes.values.XsdHexBinary
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.io.encoding.ExperimentalEncodingApi

@XmlUtilInternal
@OptIn(ExperimentalEncodingApi::class)
class XsdHexBinaryImpl(override val value: ByteArray) : XsdHexBinary, ListHelper<Byte> {

    constructor(hexString: CharSequence) : this(hexString.toString().toByteArray())

    override val xmlString: String get() = value.toHexString(HexFormat.UpperCase)

    override fun get(index: Int): Byte = value[index]

    override val size: Int get() = value.size
    override val schemaType: HexBinaryType<XsdHexBinary> get() = HexBinaryType.Instance

    override fun toString(): String = xmlString

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun equals(other: Any?): Boolean = when (other){
        is XsdHexBinary -> value.contentEquals(other.value)
        else -> false
    }

    companion object {

        private fun String.toByteArray(): ByteArray {
            var start = 0
            while (start < length && this[start] == ' ') start += 1
            if (start >= length) return ByteArray(0)
            var end = length - 1
            while (end > start && this[end] == ' ') end -= 1

            val result = ByteArray((end - start + 1) / 2)
            var rPos = 0
            for (i in start until end step 2) {
                var byteValue = 0
                for (j in 0..1) {
                    val digitValue = when (val c = this[i + j]) {
                        in '0'..'9' -> c - '0'
                        in 'A'..'F' -> c - 'A' + 10
                        in 'a'..'f' -> c - 'a' + 10
                        else -> throw NumberFormatException("Unexpected character $c in hex binary value")
                    }
                    byteValue = (byteValue shl 4) or digitValue
                }

                result[rPos++] = byteValue.toByte()
            }
            assert(rPos == result.size) { "Unexpected number of bytes in hex binary value" }
            return result
        }
    }
}
