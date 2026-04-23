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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import kotlin.jvm.JvmInline

@Serializable(UnicodeChar.Companion::class)
@JvmInline
@ExperimentalXmlUtilApi
value class UnicodeChar(val codePoint: Int) : CharSequence, Comparable<UnicodeChar> {
    override val length: Int
        get() = when {
            codePoint < 0 -> 0
            codePoint < 0x10000 -> 1
            else -> 2
        }

    val isValid: Boolean get() = codePoint in 0..0x10ffff

    val isSingleChar: Boolean get() = codePoint in 0..< 0x10000

    val zeroDigitOrNull: UnicodeChar? get() {
        val rawCp = when (codePoint) {
            in 0x30..0x39 -> 0x30
            in 0x0 .. 0x1baf -> return null
            else if (codePoint < 0x1BB0) -> when (codePoint) {
                in 0x0660 .. 0x0669 -> 0x0660 // Arabic-Indic Digits (٠١٢٣٤٥٦٧٨٩)
                in 0x06F0 .. 0x06F9 -> 0x06F0 // Extended Arabic-Indic Digits (۰۱۲۳۴۵۶۷۸۹)
                in 0x07C0 .. 0x07C9 -> 0x07C0 // N'Ko Digits
                in 0x0966 .. 0x096F -> 0x0966 // Devanagari Digits
                in 0x09E6 .. 0x09EF -> 0x09E6 // Bengali Digits
                in 0x0A66 .. 0x0A6F -> 0x0A66 // Gurmukhi Digits
                in 0x0AE6 .. 0x0AEF -> 0x0AE6 // Gujarati Digits
                in 0x0B66 .. 0x0B6F -> 0x0B66 // Oriya Digits
                in 0x0BE6 .. 0x0BEF -> 0x0BE6 // Tamil Digits
                in 0x0C66 .. 0x0C6F -> 0x0C66 // Telugu Digits
                in 0x0CE6 .. 0x0CEF -> 0x0CE6 // Kannada Digits
                in 0x0D66 .. 0x0D6F -> 0x0D66 // Malayalam Digits
                in 0x0DE6 .. 0x0DEF -> 0x0DE6 // Sinhala Lith Digits
                in 0x0E50 .. 0x0E59 -> 0x0E50 // Thai Digits
                in 0x0ED0 .. 0x0ED9 -> 0x0ED0 // Lao Digits
                in 0x0F20 .. 0x0F29 -> 0x0F20 // Tibetan Digits
                in 0x1040 .. 0x1049 -> 0x1040 // Myanmar Digits
                in 0x1090 .. 0x1099 -> 0x1090 // Myanmar Shan Digits
                in 0x17E0 .. 0x17E9 -> 0x17E0 // Khmer Digits
                in 0x1810 .. 0x1819 -> 0x1810 // Mongolian Digits
                in 0x1946 .. 0x194F -> 0x1946 // Limbu Digits
                in 0x19D0 .. 0x19D9 -> 0x19D0 // New Tai Lue Digits
                in 0x1A80 .. 0x1A89 -> 0x1A80 // Tai Tham Hora Digits
                in 0x1A90 .. 0x1A99 -> 0x1A90 // Tai Tham Tham Digits
                in 0x1B50 .. 0x1B59 -> 0x1B50 // Balinese Digits
                else -> return null
            }
            else -> when (codePoint) {
                in 0x1BB0 .. 0x1BB9 -> 0x1BB0 // Sundanese Digits
                in 0x1C40 .. 0x1C49 -> 0x1C40 // Lepcha Digits
                in 0x1C50 .. 0x1C59 -> 0x1C50 // Ol Chiki Digits
                in 0xA620 .. 0xA629 -> 0xA620 // Vai Digits
                in 0xA8D0 .. 0xA8D9 -> 0xA8D0 // Saurashtra Digits
                in 0xA900 .. 0xA909 -> 0xA900 // Kayah Li Digits
                in 0xA9D0 .. 0xA9D9 -> 0xA9D0 // Javanese Digits
                in 0xAA50 .. 0xAA59 -> 0xAA50 // Cham Digits
                in 0xABF0 .. 0xABF9 -> 0xABF0 // Meetei Mayek Digits
                in 0x11066 .. 0x1106F -> 0x11066 // Brahmi Digits
                in 0x11136 .. 0x1113F -> 0x11136 // Chakma Digits
                in 0x111D0 .. 0x111D9 -> 0x111D0 // Sharada Digits
                in 0x112F0 .. 0x112F9 -> 0x112F0 // Khudawadi Digits
                in 0x114D0 .. 0x114D9 -> 0x114D0 // Tirhuta Digits
                in 0x11650 .. 0x11659 -> 0x11650 // Modi Digits
                in 0x116C0 .. 0x116C9 -> 0x116C0 // Takri Digits
                in 0x11730 .. 0x11739 -> 0x11730 // Ahom Digits
                in 0x118E0 .. 0x118E9 -> 0x118E0 // Warang Citi Digits
                in 0x11C50 .. 0x11C59 -> 0x11C50 // Bhaiksuki Digits
                in 0x11D50 .. 0x11D59 -> 0x11D50 // Masaram Gondi Digits
                in 0x11DA0 .. 0x11DA9 -> 0x11DA0 // Gunjala Gondi Digits
                in 0x16A60 .. 0x16A69 -> 0x16A60 // Mro Digits
                in 0x16B50 .. 0x16B59 -> 0x16B50 // Pahawh Hmong Digits
                in 0x1D7CE .. 0x1D7D8 -> 0x1D7CE // Mathematical Bold Digits
                in 0x1D7D8 .. 0x1D7E1 -> 0x1D7D8 // Mathematical Double-Struck Digits
                in 0x1D7E2 .. 0x1D7EB -> 0x1D7E2 // Mathematical Sans-Serif Digits
                in 0x1D7EC .. 0x1D7F5 -> 0x1D7EC // Mathematical Bold Sans-Serif Digits
                in 0x1D7F6 .. 0x1D7FF -> 0x1D7F6 // Mathematical monospace digits

                else -> return null
            }
        }
        return UnicodeChar(rawCp)
    }

    val isDigit: Boolean get() = zeroDigitOrNull != null

    val isLetter: Boolean get() = codePoint < 0x10000 && Char(codePoint).isLetter()

    constructor(char: Char) : this(char.code)

    override fun compareTo(other: UnicodeChar): Int {
        return codePoint.compareTo(other.codePoint)
    }

    override fun get(index: Int): Char {
        if (codePoint < 0) throw IndexOutOfBoundsException("Invalid codepoint")
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

    operator fun rangeTo(other: UnicodeChar): UnicodeCharRange {
        return UnicodeCharRange(this, other)
    }

    override fun toString(): String {
        if (codePoint < 0) return ""
        return buildString {
            appendCodepoint(codePoint)
        }
    }

    companion object : KSerializer<UnicodeChar> {
        val INVALID: UnicodeChar = UnicodeChar(-1)

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

@JvmInline
value class UnicodeCharRange(val intRange: IntRange): ClosedRange<UnicodeChar>, OpenEndRange<UnicodeChar> {
    constructor(start: UnicodeChar, endInclusive: UnicodeChar) :
            this(start.codePoint..endInclusive.codePoint)

    override val start: UnicodeChar get() = UnicodeChar(intRange.first)
    override val endInclusive: UnicodeChar get() = UnicodeChar(intRange.last)

    @Suppress("DEPRECATION")
    @Deprecated("Can throw an exception when it's impossible to represent the value with Int type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    override val endExclusive: UnicodeChar get() = UnicodeChar(intRange.endExclusive)

    override fun contains(value: UnicodeChar): Boolean = intRange.contains(value.codePoint)

    override fun isEmpty(): Boolean = intRange.isEmpty()
}

@ExperimentalXmlUtilApi
@Suppress("NOTHING_TO_INLINE")
inline fun CharSequence.unicodeChar(pos: Int): UnicodeChar = UnicodeChar(get(pos))

fun CharSequence.indexOf(unicodeChar: UnicodeChar, pos: Int = 0): Int {
    for (i in pos until length) {
        if (get(i) == unicodeChar[0]) {
            if (unicodeChar.length == 1) return i
            // should be valid as we don't expect partial surrogates
            if (get(i + 1) == unicodeChar[1]) return i + 1
        }
    }
    return -1
}

@ExperimentalXmlUtilApi
@IgnorableReturnValue
fun Appendable.appendUnicode(char: UnicodeChar): Appendable = when {
    char.isSingleChar -> append(char[0])
    else -> append(char[0]).append(char[1])
}

