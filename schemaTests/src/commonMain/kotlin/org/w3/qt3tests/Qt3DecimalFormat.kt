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

package org.w3.qt3tests

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.core.internal.appendCodepoint
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.jvm.JvmInline

/**
 * The `decimal-format` element allows a decimal format to be defined as part of the static context
 * for evaluating an XPath expression that calls the `format-number()` function.
 *     
 * When the `decimal-format` element is used in an environment, the test expression will always be
 * a simple XPath expression. If the test is to be run using an XQuery processor, the decimal
 * format can be added to the static context either by using the processor's API, or by
 * constructing a query prolog containing a `declare decimal format` declaration and prepending
 * this to the test expression.
 * 
 * The mechanism is used for testing the format-number() function. As such, the decimal format
 * being defined should always be valid. Tests for invalid decimal formats should be written
 * as XQuery tests with an explicit query prolog (or the equivalent in XSLT).
 *     
 * Test Catalog006 ensures that the decimal-format element is only used in tests that are pure
 * XPath expressions.
 *
 * Note that separators are strings to support full unicode
 */
@Serializable
@XmlSerialName("decimal-format", QT3TNS)
class Qt3DecimalFormat(
    val name: SerializableQName? = null,
    @SerialName("decimal-separator")
    val decimalSeparator: OneChar? = null,
    @SerialName("grouping-separator")
    val groupingSeparator: OneChar? = null,
    @SerialName("zero-digit")
    val zeroDigit: OneChar? = null,
    val digit: OneChar? = null,
    @SerialName("minus-sign")
    val minusSign: OneChar? = null,
    val percent: OneChar? = null,
    @SerialName("per-mille")
    val perMille: OneChar? = null,
    @SerialName("pattern-separator")
    val patternSeparator: OneChar? = null,
    @SerialName("exponent-separator")
    val exponentSeparator: OneChar? = null,
    val infinity: String? = null,
    val NaN: String? = null,
) : Qt3Environment.Element

@Serializable(OneChar.Companion::class)
@JvmInline
value class OneChar(val codePoint: Int): CharSequence {
    override fun get(index: Int): Char {
        if (index < 0) throw IndexOutOfBoundsException("Negative index")
        if (codePoint < 0x10000) {
            if (index > 0) throw IndexOutOfBoundsException("Only single char codepoints supported")
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

    val isSingleChar: Boolean get() = codePoint <= 0xD800

    constructor(char: Char) : this(char.code)

    override fun toString(): String {
        return buildString {
            appendCodepoint(codePoint)
        }
    }

    companion object : KSerializer<OneChar> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("oneChar", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: OneChar) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): OneChar {
            val s = decoder.decodeString()
            if (s.isEmpty()) throw IllegalArgumentException("Empty string, expected one char/codepoint")
            if (s[0].isHighSurrogate()) {
                if (s.length != 2 || ! s[1].isLowSurrogate()) throw IllegalArgumentException("Expected single surrogate pair , but found '$s'")
                val high = s[0].code - 0xd800
                val low = s[1].code - 0xdc00
                val cp = 0x10000 + (high shl 10) + (low and 0x3ff)
                return OneChar(cp)
            } else if (s.length == 1){
                return OneChar(s[0])
            } else {
                throw IllegalArgumentException("Empty string, expected one char/codepoint")
            }
        }
    }
}
