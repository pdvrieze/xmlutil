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

package io.github.pdvrieze.formats.xpath.eval

import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.values.XsdBase64Binary

enum class Collations(override val uri: String): Collation {
    CODEPOINT("http://www.w3.org/2005/xpath-functions/collation/codepoint") {
        override fun compare(a: String, b: String): Int = a.compareTo(b)

        override fun key(key: String): XsdBase64Binary {
            return XsdBase64Binary(key.encodeToByteArray())
        }

        override fun indexOf(key: String, value: String, startPos: Int): Int {
            return value.indexOf(key, startPos)
        }
    },
    ASCII_CASE_INSENSITIVE("http://www.w3.org/2005/xpath-functions/collation/html-ascii-case-insensitive") {
        override fun compare(a: String, b: String): Int {
            for (i in 0 until minOf(a.length, b.length)) {
                val l = a[i].let { if (it < 'Z') it.lowercaseChar() else it }
                val r = b[i].let { if (it < 'Z') it.lowercaseChar() else it }
                if (l != r) return r.code - l.code
            }
            return a.length - b.length
        }

        override fun key(key: String): XsdBase64Binary {
            val bytes = key.encodeToByteArray()
            for (i in 0 until bytes.size) {
                if (bytes[i].toInt() in 'a'.code..'z'.code) {
                    bytes[i] = (bytes[i] - LOWERCASE_OFFSET).toByte()
                }
            }
            return XsdBase64Binary(bytes)
        }
    },

    ;

    companion object {
        private const val LOWERCASE_OFFSET = 'a'.code - 'A'.code
    }

}


internal expect fun resolveCollation(uri: String): Collation?
