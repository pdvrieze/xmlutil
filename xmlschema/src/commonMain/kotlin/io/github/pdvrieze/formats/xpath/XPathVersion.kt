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

package io.github.pdvrieze.formats.xpath

sealed interface SpecVersion {
    fun includes(xpathVersion: XPathVersion): Boolean
    fun includes(xqueryVersion: XQueryVersion): Boolean

    fun includes(anyVersion: SpecVersion): Boolean
}

enum class XPathVersion : Comparable<XPathVersion>, SpecVersion {
    XPath1_0,
    XPath2_0,
    XPath3_0,
    XPath3_1;

    override fun includes(xpathVersion: XPathVersion): Boolean {
        return ordinal >= xpathVersion.ordinal
    }

    override fun includes(xqueryVersion: XQueryVersion): Boolean = false

    override fun includes(anyVersion: SpecVersion): Boolean {
        return anyVersion is XPathVersion && ordinal >= anyVersion.ordinal
    }
}

enum class XQueryVersion(val xpath:XPathVersion) : Comparable<XQueryVersion>, SpecVersion {
    XQuery1_0(XPathVersion.XPath2_0),
    XQuery3_0(XPathVersion.XPath3_0),
    XQuery3_1(XPathVersion.XPath3_1);

    override fun includes(xpathVersion: XPathVersion): Boolean {
        return xpath.includes(xpathVersion)
    }

    override fun includes(xqueryVersion: XQueryVersion): Boolean {
        return ordinal >= xqueryVersion.ordinal
    }

    override fun includes(anyVersion: SpecVersion): Boolean = when (anyVersion) {
        is XPathVersion -> includes(anyVersion)
        is XQueryVersion -> includes(anyVersion)
    }
}

