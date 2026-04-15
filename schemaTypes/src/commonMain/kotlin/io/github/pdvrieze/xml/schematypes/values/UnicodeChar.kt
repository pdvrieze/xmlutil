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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import kotlin.jvm.JvmInline

@Serializable(UnicodeChar.Companion::class)
@JvmInline
value class UnicodeChar(val codePoint: Int) : CharSequence {
    override fun get(index: Int): Char {
        if (index < 0) throw IndexOutOfBoundsException("Negative index")
        if (codePoint < 0x10000) {
            if (index > 0) throw IndexOutOfBoundsException("The codepoint only has one character")
            return Char(codePoint)
        }
        val down = codePoint - 0x10000
        return when (index) {
            0 -> Char((down shr 10) + 0xd800)
            1 -> Char((down and 0x3ff) + 0xdc00)
            else -> throw IndexOutOfBoundsException("Only single code points supported")
        }
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return toString().subSequence(startIndex, endIndex)
    }

    override val length: Int
        get() = when {
            codePoint < 0x10000 -> 1
            else -> 2
        }

    val isSingleChar: Boolean get() = codePoint < 0x10000

    constructor(char: Char) : this(char.code)

    override fun toString(): String {
        return buildString {
            appendCodepoint(codePoint)
        }
    }

    companion object : KSerializer<UnicodeChar> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("io.github.pdvrieze.xml.schematypes.values.UnicodeChar", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: UnicodeChar) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): UnicodeChar {
            val s = decoder.decodeString()
            if (s.isEmpty()) throw IllegalArgumentException("Empty string, expected one char/codepoint")
            if (s[0].isHighSurrogate()) {
                if (s.length != 2 || !s[1].isLowSurrogate()) throw IllegalArgumentException("Expected single surrogate pair , but found '$s'")
                val high = s[0].code - 0xd800
                val low = s[1].code - 0xdc00
                val cp = 0x10000 + (high shl 10) + (low and 0x3ff)
                return UnicodeChar(cp)
            } else if (s.length == 1) {
                return UnicodeChar(s[0])
            } else {
                throw IllegalArgumentException("Empty string, expected one char/codepoint")
            }
        }
    }
}
