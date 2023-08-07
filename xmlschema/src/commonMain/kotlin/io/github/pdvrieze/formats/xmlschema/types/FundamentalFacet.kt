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

package io.github.pdvrieze.formats.xmlschema.types

sealed class FundamentalFacet

data class OrderedFacet(val value: Order) : FundamentalFacet() {
    enum class Order { FALSE, PARTIAL, TOTAL }
}
data class BoundedFacet(val value: Boolean) : FundamentalFacet()

data class CardinalityFacet(val value: Cardinality) : FundamentalFacet() {
    enum class Cardinality { FINITE, COUNTABLY_INFINITE }
}

data class NumericFacet(val value: Boolean): FundamentalFacet()

class FundamentalFacets(
    val ordered: OrderedFacet.Order,
    val bounded: Boolean,
    val cardinality: CardinalityFacet.Cardinality,
    val numeric: Boolean
) : AbstractList<FundamentalFacet>() {
    override val size: Int get() = 4

    override fun get(index: Int): FundamentalFacet = when (index) {
        0 -> OrderedFacet(ordered)
        1 -> BoundedFacet(bounded)
        2 -> CardinalityFacet(cardinality)
        3 -> NumericFacet(numeric)
        else -> throw IndexOutOfBoundsException("$index")
    }

    override fun indexOf(element: FundamentalFacet): Int = when (element) {
        is OrderedFacet -> if (element.value == ordered) 0 else -1
        is BoundedFacet -> if (element.value == bounded) 1 else -1
        is CardinalityFacet -> if (element.value == cardinality) 2 else -1
        is NumericFacet -> if (element.value == numeric) 3 else -1
    }

    override fun lastIndexOf(element: FundamentalFacet): Int {
        return indexOf(element)
    }
}
