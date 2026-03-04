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

package io.github.pdvrieze.xml.schematypes.types

import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.*
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimpleList
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.XmlUtilInternal

interface AnySimpleListType<out T: XsdAnySimple, out E: XsdAnySimple> : AnySimpleType<T> {
    override val baseType: AnySimpleType<*>

    val itemType: AnySimpleType<E>

    @XmlUtilInternal
    class Instance<out T: XsdAnySimple, out E: XsdAnySimple>(
        override val name: XsdQName?,
        override val itemType: AnySimpleType<E>,
        constrainingFacets: List<ConstrainingFacet> = emptyList(),
    ) : AnySimpleListType<T, E> {

        constructor(elementType: AnySimpleType<E>): this(null, elementType)

        override val ordered: FacetOrdered get() = FacetOrdered.FALSE
        override val bounded: FacetBounded get() = FacetBounded.UNBOUNDED
        override val cardinality: FacetCardinality get() = FacetCardinality.COUNTABLY_INFINITE
        override val numeric: FacetNumeric get() = FacetNumeric.FALSE

        override val constrainingFacets: List<ConstrainingFacet> = buildList {
            for (facet in constrainingFacets) {
                when (facet) {
                    is FacetLength,
                    is FacetMinLength,
                    is FacetMaxLength,
                    is FacetEnumeration,
                    is FacetPattern,
                    is FacetAssertion -> add(facet)

                    is FacetWhiteSpace if (facet.value != WhitespaceValue.COLLAPSE ||
                            facet.fixed != false) -> Unit // ignore

                    else -> throw IllegalArgumentException("Unsupported facet: $facet")
                }
            }

            add(FacetWhiteSpace(WhitespaceValue.COLLAPSE, true))
        }

        override val baseType: AnySimpleType<XsdAnySimple> get() = AnySimpleType.Instance
    }

    companion object {
        operator fun <E : XsdAnySimple> invoke(
            itemType: AnySimpleType<E>,
            facets: List<ConstrainingFacet> = emptyList()
        ): AnySimpleListType<XsdAnySimpleList<E>, E> {

            return Instance(null, itemType, facets)
        }
    }
}
