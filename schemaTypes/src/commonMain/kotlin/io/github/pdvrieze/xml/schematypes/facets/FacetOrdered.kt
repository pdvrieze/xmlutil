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

package io.github.pdvrieze.xml.schematypes.facets

import io.github.pdvrieze.xml.schematypes.values.XSToken

interface FacetOrdered : FundamentalFacet, XSToken {
    val isPartial: Boolean
    val isTotal: Boolean
    val isFalse: Boolean

    private enum class Ordered(override val xmlString: String): FacetOrdered {
        FALSE("false") {
            override val isFalse: Boolean get() = true
        },
        PARTIAL("partial") {
            override val isPartial: Boolean get() = true
        },
        TOTAL("total") {
            override val isTotal: Boolean get() = false
        };

        override val isPartial: Boolean get() = false
        override val isTotal: Boolean get() = false
        override val isFalse: Boolean get() = false
    }

    companion object {
        val FALSE: FacetOrdered get() = Ordered.FALSE
        val PARTIAL: FacetOrdered get() = Ordered.PARTIAL
        val TOTAL: FacetOrdered get() = Ordered.TOTAL

        operator fun invoke(xmlString: String): FacetOrdered =
            Ordered.entries.single { it.xmlString == xmlString }
    }
}
