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

package io.github.pdvrieze.formats.xpath.impl

enum class Axis(val repr: String) {
    CHILD("child"),
    DESCENDANT("descendant"),
    PARENT("parent"),
    ANCESTOR("ancestor"),
    FOLLOWING_SIBLING("following-sibling"),
    PRECEDING_SIBLING("preceding-sibling"),
    FOLLOWING("following"),
    PRECEDING("preceding"),
    ATTRIBUTE("attribute"),
    NAMESPACE("namespace"),
    SELF("self"),
    DESCENDANT_OR_SELF("descendant-or-self"),
    ANCESTOR_OR_SELF("ancestor-or-self"),
    ;

    companion object {
        private val lookup = Axis.values().associateBy { it.repr }

        fun from(value: String): Axis {
            return requireNotNull(lookup[value]) { "$value is not a valid path axis" }
        }
    }


}
