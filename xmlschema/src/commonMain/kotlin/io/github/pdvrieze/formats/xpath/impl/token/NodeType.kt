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

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath1
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath3_0

enum class NodeType(
    override val literal: String,
    override val minVersion: XPathVersion = XPathVersion.XPath3_1,
): BuiltinToken {

    @NeedsXPath2
    DOCUMENT("document-node", XPathVersion.XPath2_0),
    @NeedsXPath2
    ELEMENT("element", XPathVersion.XPath2_0),
    @NeedsXPath2
    SCHEMA_ELEMENT("schema-element", XPathVersion.XPath2_0),
    @NeedsXPath2
    SCHEMA_ATTRIBUTE("schema-attribute", XPathVersion.XPath2_0),
    @NeedsXPath2
    ATTRIBUTE("attribute", XPathVersion.XPath2_0),
    @NeedsXPath1
    COMMENT("comment", XPathVersion.XPath1_0),
    @NeedsXPath1
    TEXT("text", XPathVersion.XPath1_0),
    @NeedsXPath1
    PROCESSING_INSTRUCTION("processing-instruction", XPathVersion.XPath1_0),
    @NeedsXPath3_0
    NAMESPACE_NODE("namespace-node", XPathVersion.XPath3_0),
    @NeedsXPath1
    ANY_KIND("node", XPathVersion.XPath1_0),;

    override val isDelimiting: Boolean get() = false

    fun collectUnsupportedExprs(
        xPathVersion: XPathVersion,
        isXQuery: Boolean,
        collector: MutableList<Any>
    ) {
        if (minVersion > xPathVersion) collector.add(this)
    }

    override fun toString(): String {
        return "NodeType test($literal)"
    }

    enum class ExpectedContent {
        ELEMENT_DECL,
        NODE,
    }

    companion object {
        private val lookup = entries.associateBy { it.literal }

        /** Has internal version check */
        fun maybeValueOf(name: String, version: XPathVersion): NodeType? {
            return lookup[name]?.takeIf { it.minVersion <= version }
        }
    }
}
