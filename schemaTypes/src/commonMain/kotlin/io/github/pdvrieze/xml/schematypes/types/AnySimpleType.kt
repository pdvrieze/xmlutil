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
import io.github.pdvrieze.xml.schematypes.values.XSAnySimple
import io.github.pdvrieze.xml.schematypes.values.XSQName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlUtilInternal

sealed interface AnySimpleType : AnyType {
    interface ListT<E: AtomicOrUnion> : AnySimpleType {

        val itemType: E

        @XmlUtilInternal
        class Instance<E: AtomicOrUnion>(
            override val name: XSQName?,
            override val itemType: E,
            constrainingFacets: List<ConstrainingFacet> = emptyList(),
        ) : ListT<E> {

            constructor(elementType: E): this(null, elementType)

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

            override val baseType: AnySimpleType get() = AnySimpleType.Instance
        }

        companion object {
            operator fun <E : AtomicOrUnion> invoke(itemType: E, facets: List<ConstrainingFacet> = emptyList()): ListT<E> {
                return Instance(null, itemType, facets)
            }
        }
    }

    interface Union : AtomicOrUnion {

    }

    interface AtomicOrUnion: AnySimpleType

    val ordered: FacetOrdered
    val bounded: FacetBounded
    val cardinality: FacetCardinality
    val numeric: FacetNumeric

    val constrainingFacets: List<ConstrainingFacet>

    val facets: List<Facet> get() = buildList {
        add(ordered)
        add(bounded)
        add(cardinality)
        add(numeric)
        addAll(constrainingFacets)
    }
    
    object Instance: AnySimpleType, BuiltinAtomicType<XSAnySimple> {
        override val name: XSQName get() = XSQName(XMLConstants.XSD_NS_URI, "anySimpleType", "xs")
        override val baseType: AnyType get() = AnyType.Instance

        override val ordered: FacetOrdered get() = FacetOrdered.FALSE
        override val bounded: FacetBounded get() = FacetBounded.UNBOUNDED
        override val cardinality: FacetCardinality get() = FacetCardinality.COUNTABLY_INFINITE
        override val numeric: FacetNumeric get() = FacetNumeric.FALSE
        override val constrainingFacets: List<ConstrainingFacet> get() = emptyList()
    }
}

