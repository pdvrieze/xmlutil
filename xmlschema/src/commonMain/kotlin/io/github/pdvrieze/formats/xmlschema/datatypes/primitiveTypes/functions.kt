/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes

fun isNameStartChar(c: Char, isColonValid: Boolean = true): Boolean = when (c) {
    '\u00f7', '\u037E' -> false

    ':' -> isColonValid

    in 'A'..'Z',
    '_',
    in 'a'..'z',
    in '\u00c0'..'\u00d6',
    in '\u00d8'..'\u02ff',
    in '\u0370'..'\u1FFF',
    '\u200C', '\u200D',
    in '\u2070'..'\u218f',
    in '\u2C00'..'\u2FEF',
    in '\u3001'..'\uD7FF',
    in '\uF900'..'\uFDCF',
    in '\uFDF0'..'\uFFFD',
    -> true

    else -> false
}

fun isNameStartCode(c: Int, isColonValid: Boolean = true): Boolean = when (c) {
    0x00f7, 0x037E -> false

    ':'.code -> isColonValid

    in 'A'.code..'Z'.code,
    '_'.code,
    in 'a'.code..'z'.code,
    in 0x00c0..0x00d6,
    in 0X00d8..0x02ff,
    in 0X0370..0x1FFF,
    0X200C, 0x200D,
    in 0X2070..0x218f,
    in 0X2C00..0x2FEF,
    in 0X3001..0xD7FF,
    in 0XF900..0xFDCF,
    in 0XFDF0..0xFFFD,
    in 0X10000.. 0xeffff,
    -> true

    else -> false
}

fun isNameCode(c: Int, isColonValid: Boolean = true): Boolean = when (c) {
    0x00f7, 0x037E -> false

    ':'.code -> isColonValid

    in 'A'.code..'Z'.code,
    '_'.code, '-'.code, '.'.code,
    in 'a'.code..'z'.code,
    in '0'.code..'9'.code,
    0x00b7,
    in 0x00c0..0x00d6,
    in 0x00d8..0x1FFF,
    0x200C, 0x200D,
    0x203F, 0x2040,
    in 0x2070..0x218f,
    in 0x2C00..0x2FEF,
    in 0x3001..0xD7FF,
    in 0xF900..0xFDCF,
    in 0xFDF0..0xFFFD,
    in 0x10000.. 0xeffff,
    -> true

    else -> false
}

fun isNameChar(c: Char, isColonValid: Boolean = true): Boolean = when (c) {
    '\u00f7', '\u037E' -> false

    ':' -> isColonValid

    in 'A'..'Z',
    '_', '-', '.',
    in 'a'..'z',
    in '0'..'9',
    '\u00b7',
    in '\u00c0'..'\u00d6',
    in '\u00d8'..'\u1FFF',
    '\u200C', '\u200D',
    '\u203F', '\u2040',
    in '\u2070'..'\u218f',
    in '\u2C00'..'\u2FEF',
    in '\u3001'..'\uD7FF',
    in '\uF900'..'\uFDCF',
    in '\uFDF0'..'\uFFFD', -> true

    else -> false
}

fun CharSequence.isNCName10(): Boolean {
    if (isEmpty()) return false
    if (!isNameStartChar(this[0], false)) return false
    for (idx in 1 until length) {
        if (!isNameChar(this[idx], false)) return false
    }
    return true
}

fun CharSequence.isNCName(): Boolean {
    if (isEmpty()) return false
    val codepoints = CodepointIterator(this)
    if (!isNameStartCode(codepoints.next(), false)) return false
    while (codepoints.hasNext()) {
        if (!isNameCode(codepoints.next(), false)) return false
    }
    return true
}

private class CodepointIterator(private val base: CharSequence): Iterator<Int> {
    private var pos = 0

    override fun hasNext(): Boolean = pos < base.length

    override fun next(): Int {
        if (base[pos].isHighSurrogate()) {
            return ((base[pos++].code and 0x3ff) shl 11) or (base[pos++].code and 0x3ff)
        } else {
            return base[pos++].code
        }
    }
}

private fun CharSequence.codePoints(): Sequence<Int> = CodepointIterator(this).asSequence()

fun CharSequence.isXmlName10(): Boolean {
    if (length == 0) return false
    if (!isNameStartChar(this[0])) return false
    for (idx in 1 until length) {
        if (!isNameChar(this[idx])) return false
    }
    return true
}

fun CharSequence.isXmlName(): Boolean {
    if (length == 0) return false
    val codepoints = CodepointIterator(this)
    if (!isNameStartCode(codepoints.next())) return false
    while (codepoints.hasNext()) {
        if (!isNameCode(codepoints.next())) return false
    }
    return true
}

