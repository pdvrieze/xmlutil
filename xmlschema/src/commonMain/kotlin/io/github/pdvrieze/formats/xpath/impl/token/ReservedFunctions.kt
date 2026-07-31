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

import io.github.pdvrieze.formats.xpath.SpecVersion
import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.XQueryVersion
import io.github.pdvrieze.formats.xpath.impl.NeedsXQuery1

enum class ReservedFunctions(
    override val literal: String,
    val minSpecVersion: SpecVersion = XPathVersion.XPath1_0
): BuiltinToken {

    ARRAY("array"),
    ATTRIBUTE("attribute"),
    COMMENT("comment"),
    DOCUMENT_NODE("document-node"),
    ELEMENT("element"),
    EMPTY_SEQUENCE("empty-sequence"),
    FUNCTION("function"),
    IF("if"),
    ITEM("item"),
    MAP("map"),
    NAMESPACE_NODE("namespace-node"),
    NODE("node"),
    PROCESSING_INSTRUCTION("processing-instruction"),
    SCHEMA_ATTRIBUTE("schema-attribute"),
    SCHEMA_ELEMENT("schema-element"),
    @NeedsXQuery1 SWITCH("switch", XQueryVersion.XQuery1_0),
    TEXT("text"),
    @NeedsXQuery1 TYPESWITCH("typeswitch", XQueryVersion.XQuery1_0),
    ;

    override val isDelimiting: Boolean get() = false

    companion object {
        private val RESERVED_LOOKUP: Array<Array<Array<ReservedFunctions>>> = Array(23) { size ->
            Array(26) { firstLetter ->
                entries.filter { it.literal.length-1 == size && (it.literal[0].code - 'a'.code) == firstLetter }.toTypedArray()
            }
        }

        fun getReserved(name: String): ReservedFunctions? {
            return when {
                name.length > RESERVED_LOOKUP.size -> null
                name[0] !in 'a'..'z' -> null
                else -> RESERVED_LOOKUP[name.length - 1][name[0].code - 'a'.code].firstOrNull { name == it.literal }
            }
        }

    }
}
