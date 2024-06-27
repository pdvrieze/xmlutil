/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.core.internal

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
public fun String.countIndentedLength(): Int = fold(0) { acc, ch ->
    acc + when (ch) {
        '\t' -> 8
        else -> 1
    }
}



@ExperimentalXmlUtilApi
public fun isNameStartCode(c: Int, isColonValid: Boolean = true): Boolean = when (c) {
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

@ExperimentalXmlUtilApi
public fun isNameCode(c: Int, isColonValid: Boolean = true): Boolean = when (c) {
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

@ExperimentalXmlUtilApi
public fun isNameStartChar(c: Char, isColonValid: Boolean = true): Boolean = when (c) {
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

@ExperimentalXmlUtilApi
public fun isNameChar10(c: Char, isColonValid: Boolean = true): Boolean = when (c) {
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

@ExperimentalXmlUtilApi
public fun isNameChar11(c: Char, isColonValid: Boolean = true): Boolean = when (c) {
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
//    in '\u3001'..'\uD7FF',
    in '\u3001'..'\uDFFF', // surrogate pairs always have characters > 0x10000, so in 1.1 are always valid.
    in '\uF900'..'\uFDCF',
    in '\uFDF0'..'\uFFFD', -> true

    else -> false
}
