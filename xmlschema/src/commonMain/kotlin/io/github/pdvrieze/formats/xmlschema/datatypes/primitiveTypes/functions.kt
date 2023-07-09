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


fun isNameChar(c: Char, isColonValid: Boolean = true): Boolean = when (c) {
    '\u00f7', '\u037E' -> false

    ':' -> isColonValid

    in 'A'..'Z',
    '_',
    '-',
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

fun CharSequence.isNCName(): Boolean {
    if (isEmpty()) return false
    if (!isNameStartChar(this[0], false)) return false
    for (idx in 1 until length) {
        if (!isNameChar(this[idx], false)) return false
    }
    return true
}

fun CharSequence.isXmlName(): Boolean {
    if (length == 0) return false
    if (!isNameStartChar(this[0])) return false
    for (idx in 1 until length) {
        if (!isNameChar(this[idx])) return false
    }
    return true
}

