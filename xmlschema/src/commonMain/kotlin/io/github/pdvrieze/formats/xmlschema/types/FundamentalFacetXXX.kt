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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.xml.schematypes.facets.*

sealed class FundamentalFacetXXX

data class OrderedFacet(val value: Order) : FundamentalFacetXXX() {

    object Order {
        val FALSE get() = FacetOrdered.FALSE
        val PARTIAL get() = FacetOrdered.PARTIAL
        val TOTAL get() = FacetOrdered.TOTAL
    }

    enum class OrderXX : FacetOrdered {
        FALSE {
            override val isFalse: Boolean get() = true
        },
        PARTIAL {
            override val isPartial: Boolean get() = true
        },
        TOTAL {
            override val isTotal: Boolean get() = true
        };

        override val isPartial: Boolean get() = false
        override val isTotal: Boolean get() = false
        override val isFalse: Boolean get() = false

        companion object {
            operator fun invoke(o: FacetOrdered): OrderXX = when {
                o.isFalse -> FALSE
                o.isTotal -> TOTAL
                else -> PARTIAL
            }
        }
    }
}
data class BoundedFacet(val value: Boolean) : FundamentalFacetXXX()

data class CardinalityFacet(val value: Cardinality) : FundamentalFacetXXX() {
    object Cardinality {
        val FINITE get() = FacetCardinality.FINITE
        val COUNTABLY_INFINITE get() = FacetCardinality.COUNTABLY_INFINITE
    }

    enum class CardinalityX {
        FINITE, COUNTABLY_INFINITE;

        companion object {
            operator fun invoke(c: FacetCardinality): CardinalityX = when {
                c.isFinite -> FINITE
                else -> COUNTABLY_INFINITE
            }
        }
    }
}

data class NumericFacet(val value: Boolean): FundamentalFacetXXX()

class FundamentalFacets(
    val ordered: FacetOrdered,
    val bounded: FacetBounded,
    val cardinality: FacetCardinality,
    val numeric: FacetNumeric
) : AbstractList<FundamentalFacet>() {

    constructor(
        ordered: FacetOrdered,
        bounded: Boolean,
        cardinality: FacetCardinality,
        numeric: Boolean
    ) : this(ordered, FacetBounded(bounded), cardinality, FacetNumeric(numeric))

    override val size: Int get() = 4

    val isBounded: Boolean get() = bounded.isBounded
    val isNumeric: Boolean get() = numeric.isNumeric

    override fun get(index: Int): FundamentalFacet = when (index) {
        0 -> ordered
        1 -> bounded
        2 -> cardinality
        3 -> numeric
        else -> throw IndexOutOfBoundsException("$index")
    }

    override fun indexOf(element: FundamentalFacet): Int = when (element) {
        is FacetOrdered -> if (! element.isFalse) 0 else -1
        is FacetBounded -> if (element.isBounded == isBounded) 1 else -1
        is FacetCardinality -> if (element.isFinite) 2 else -1
        is FacetNumeric -> if (element.isNumeric) 3 else -1
    }

    override fun lastIndexOf(element: FundamentalFacet): Int {
        return indexOf(element)
    }
}
