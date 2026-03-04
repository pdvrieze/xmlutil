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
import io.github.pdvrieze.xml.schematypes.values.XsdDate
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.XMLConstants

interface DateType<out T : XsdDate> : PrimitiveDatatype<T> {

    override val ordered: FacetOrdered get() = FacetOrdered.PARTIAL
    override val bounded: FacetBounded get() = FacetBounded.UNBOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.COUNTABLY_INFINITE
    override val numeric: FacetNumeric get() = FacetNumeric.FALSE

    override val name: XsdQName? get() = Instance.name

    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    object Instance : DateType<XsdDate>, BuiltinType {
        override val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "date", "xs")

        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true),
            FacetExplicitTimezone.OPTIONAL
        )
    }
}
