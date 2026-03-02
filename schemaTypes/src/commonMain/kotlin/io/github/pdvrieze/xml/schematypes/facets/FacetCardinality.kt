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

interface FacetCardinality : FundamentalFacet {
    val isFinite: Boolean
    val isCountablyInfinite: Boolean get() = !isFinite

    private enum class Cardinality(override val isFinite: Boolean): FacetCardinality {
        FINITE(true), COUNTABLY_INFINITE(false);
    }

    companion object {
        val FINITE: FacetCardinality get() = Cardinality.FINITE
        val COUNTABLY_INFINITE: FacetCardinality get() = Cardinality.COUNTABLY_INFINITE

        operator fun invoke(isFinite: Boolean): FacetCardinality =
            if(isFinite) Cardinality.FINITE else Cardinality.COUNTABLY_INFINITE
    }
}
