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

package io.github.pdvrieze.formats.xpath.impl.token

internal sealed interface BuiltinToken: QNameOrBuiltin, WordToken {

    companion object {
        private val BUILTIN_LOOKUP: Array<Array<Array<BuiltinToken>>>

        init {
            val entries = mutableMapOf<String, BuiltinToken>()
            for (e in ReservedFunctions.entries) { entries[e.literal] = e }
            for (e in NodeType.entries) { entries[e.literal] = e }

            BUILTIN_LOOKUP = Array(23) { size ->
                Array(26) { firstLetter ->
                    entries.values.filter { it.literal.length -1 == size && (it.literal[0].code - 'a'.code) == firstLetter }.toTypedArray()
                }
            }

        }

        public fun getBuiltin(name: String): BuiltinToken? {
            return when {
                name.length !in 1..<26 -> null
                name[0] !in 'a'..'z' -> null
                else -> BUILTIN_LOOKUP[name.length-1][name[0].code - 'a'.code].firstOrNull { name == it.literal }
            }
        }

    }

}
