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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFractionDigits
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSPattern
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinAtomicType
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.facets.*
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.FacetCardinality
import io.github.pdvrieze.xml.schematypes.facets.FacetOrdered
import io.github.pdvrieze.xml.schematypes.types.UnsignedShortType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdString
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedInt
import io.github.pdvrieze.xml.schematypes.values.XsdUnsignedShort

object ResUnsignedShortType : ResolvedBuiltinAtomicType<XsdUnsignedShort>, ResIIntegerType<XsdUnsignedShort>,
    UnsignedShortType<XsdUnsignedShort> {

    override val baseType: ResUnsignedIntType get() = ResUnsignedIntType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        fractionDigits = ResolvedFractionDigits(XSFractionDigits(0u)),
        patterns = listOf(ResolvedPattern(XSPattern("[\\-+]?[0-9]+"), SchemaVersion.V1_1,)),
        maxConstraint = ResolvedMaxInclusive.Companion.createUnverified(XsdUnsignedShort(65535u)),
        minConstraint = ResolvedMinInclusive.Companion.createUnverified(XsdUnsignedShort(0u)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = FacetOrdered.Companion.TOTAL,
        bounded = true,
        cardinality = FacetCardinality.Companion.FINITE,
        numeric = true,
    )

    override fun valueFromNormalized(normalized: XsdString): XsdUnsignedShort {
        return XsdUnsignedShort(normalized.xmlString.toUShort())
    }

    override fun value(maybeValue: XsdAnySimple): XsdUnsignedInt {
        return maybeValue as? XsdUnsignedInt ?: value((maybeValue as? XsdString) ?: XsdString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        check(value is XsdUnsignedInt)
    }

    override fun validate(representation: XsdString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }

}
