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
import io.github.pdvrieze.xml.schematypes.values.XsdEntities
import io.github.pdvrieze.xml.schematypes.values.XsdEntity
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.XMLConstants

interface EntitiesType<out T : XsdEntities, out E: XsdEntity> : AnySimpleListType<T, E> {
    override val baseType: AnySimpleListType<*, *> get() = Instance.baseType
    override val itemType: EntityType<E>

    override val ordered: FacetOrdered get() = FacetOrdered.FALSE
    override val bounded: FacetBounded get() = FacetBounded.UNBOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.COUNTABLY_INFINITE
    override val numeric: FacetNumeric get() = FacetNumeric.FALSE

    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    override val name: XsdQName? get() = Instance.name

    object Instance : EntitiesType<XsdEntities, XsdEntity>, BuiltinType {
        override val baseType: AnySimpleListType<*, *> =
            AnySimpleListType(itemType)

        override val itemType: EntityType<XsdEntity> get() = EntityType.Instance

        override val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "ENTITIES", "xs")

        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true),
            FacetMinLength(1u),
        )
    }

}
