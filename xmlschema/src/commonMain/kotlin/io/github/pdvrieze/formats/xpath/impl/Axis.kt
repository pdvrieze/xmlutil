/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.XPathVersion

enum class Axis(val literal: String, val minVersion: XPathVersion = XPathVersion.XPath1_0) {
    @XPath1 CHILD("child"),
    @XPath1 DESCENDANT("descendant"),
    @XPath1 PARENT("parent"),
    @XPath1 ANCESTOR("ancestor"),
    @XPath1 FOLLOWING_SIBLING("following-sibling"),
    @XPath1 PRECEDING_SIBLING("preceding-sibling"),
    @XPath1 FOLLOWING("following"),
    @XPath1 PRECEDING("preceding"),
    @XPath1 ATTRIBUTE("attribute"),
    @XPath1 NAMESPACE("namespace"),
    @XPath1 SELF("self"),
    @XPath1 DESCENDANT_OR_SELF("descendant-or-self"),
    @XPath1 ANCESTOR_OR_SELF("ancestor-or-self"),
    ;

    companion object {
        private val lookup = entries.associateBy { it.literal }

        fun from(value: String): Axis {
            return requireNotNull(lookup[value]) { "$value is not a valid path axis" }
        }
    }


}
