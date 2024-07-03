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

package nl.adaptivity.xmlutil.core.impl

import kotlin.jvm.JvmStatic

internal class EntityMap {
    private val otherEntities = HashMap<String,String>(8)

    public operator fun get(key: String): String? {
        when (key.length) {
            2 -> when (key) {
                "gt" -> return ">"
                "lt" -> return "<"
            }

            3 -> when (key) {
                "amp" -> return "&"
            }

            4 -> when (key) {
                "apos" -> return "'"
                "quot" -> return "\""
            }
        }
        return otherEntities[key]
    }

/*
    override val entries: Set<Map.Entry<String, String>>
        get() = TODO("not implemented")
    override val keys: Set<String> get() = BUILTIN_KEYS + otherEntities.keys
    override val size: Int get() = 5 + otherEntities.size

    override val values: Collection<String> get() = BUILTIN_VALUES + otherEntities.values

    override fun containsKey(key: String): Boolean {
        when (key.length) {
            2 -> when (key) {
                "gt" -> return true
                "lt" -> return true
            }

            3 -> when (key) {
                "amp" -> return true
            }

            4 -> when (key) {
                "apos" -> return true
                "quot" -> return true
            }
        }
        return otherEntities.containsKey(key)
    }


    override fun containsValue(value: String): Boolean {
        return when (value) {
            "gt",
            "lt",
            "amp",
            "apos",
            "quot" -> true

            else -> otherEntities.containsValue(value)
        }
    }

    override fun isEmpty(): Boolean = false
*/

    private companion object {
        @JvmStatic
        private val BUILTIN_KEYS = hashSetOf("gt", "lt", "amp", "apos", "quot")
        private val BUILTIN_VALUES = listOf(">", "<", "&", "'", "\"")
    }
}

