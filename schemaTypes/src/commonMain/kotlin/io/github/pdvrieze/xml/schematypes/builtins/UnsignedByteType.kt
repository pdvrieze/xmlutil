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

package io.github.pdvrieze.xml.schematypes.builtins

import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.*
import io.github.pdvrieze.xml.schematypes.types.BuiltinAtomicType
import io.github.pdvrieze.xml.schematypes.values.XSQName
import io.github.pdvrieze.xml.schematypes.values.XSUnsignedByte
import nl.adaptivity.xmlutil.XMLConstants

interface UnsignedByteType : BuiltinAtomicType<XSUnsignedByte> {
    override val baseType: UnsignedShortType get() = UnsignedShortType.Instance

    override val ordered: FacetOrdered get() = FacetOrdered.TOTAL
    override val bounded: FacetBounded get() = FacetBounded.BOUNDED
    override val cardinality: FacetCardinality get() = FacetCardinality.FINITE
    override val numeric: FacetNumeric get() = FacetNumeric.TRUE

    override val name: XSQName get() = Instance.name

    override val constrainingFacets: List<ConstrainingFacet>
        get() = Instance.constrainingFacets

    object Instance: UnsignedByteType {
        override val name: XSQName get() = XSQName(XMLConstants.XSD_NS_URI, "unsignedByte", "xs")

        override val constrainingFacets: List<ConstrainingFacet> = listOf(
            FacetWhiteSpace(WhitespaceValue.COLLAPSE, true),
            FacetFractionDigits.Companion(0u),
            FacetPattern("[\\-+]?[0-9]+"),
            FacetMaxInclusive.Companion(XSUnsignedByte.Companion(UByte.MAX_VALUE)),
            FacetMinInclusive.Companion(XSUnsignedByte.Companion(0u)),
        )
    }
}
