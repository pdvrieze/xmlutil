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

import nl.adaptivity.xmlutil.core.internal.*


fun CharSequence.isNCName10(): Boolean {
    if (isEmpty()) return false
    if (!isNameStartChar(this[0], false)) return false
    for (idx in 1 until length) {
        if (!isNameChar10(this[idx], false)) return false
    }
    return true
}

fun CharSequence.isNCName(): Boolean {
    if (isEmpty()) return false
    val codepoints = CodepointIterator(this)
    if (!isNameStartCode(codepoints.next(), false)) return false
    while (codepoints.hasNext()) {
        if (!isNameCodepoint(codepoints.next(), false)) return false
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
        if (!isNameChar10(this[idx])) return false
    }
    return true
}

fun CharSequence.isXmlName(): Boolean {
    if (length == 0) return false
    val codepoints = CodepointIterator(this)
    if (!isNameStartCode(codepoints.next())) return false
    while (codepoints.hasNext()) {
        if (!isNameCodepoint(codepoints.next())) return false
    }
    return true
}

