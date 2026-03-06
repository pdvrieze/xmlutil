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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSPattern
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinAtomicType
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedPattern
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.FacetCardinality
import io.github.pdvrieze.xml.schematypes.facets.FacetOrdered
import io.github.pdvrieze.xml.schematypes.types.DayTimeDurationType
import io.github.pdvrieze.xml.schematypes.values.*

object ResDayTimeDurationType : ResolvedBuiltinAtomicType<XsdDayTimeDuration>, DayTimeDurationType<XsdDayTimeDuration> {
    override val baseType: ResDurationType get() = ResDurationType

    override val mdlFacets: FacetList = FacetList(
        whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE, true)),
        patterns = listOf(ResolvedPattern(XSPattern("[^YM]*(T.*)?"), SchemaVersion.V1_1,)),
    )

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = FacetOrdered.Companion.PARTIAL,
        bounded = false,
        cardinality = FacetCardinality.Companion.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun valueFromNormalized(normalized: XsdString): XsdDayTimeDuration {
        return XsdDayTimeDuration(normalized.xmlString)
    }

    override fun value(maybeValue: XsdAnySimple): XsdDuration {
        return maybeValue as? XsdYearMonthDuration ?: value((maybeValue as? XsdString) ?: XsdString(maybeValue.xmlString))
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        require(value is XsdDayTimeDuration)
        require(value.months == 0L)
        // TODO is this correct?
        val days = value.millis / (24 * 3600_000)
        require(days == 0L)
    }

    override fun validate(representation: XsdString, version: SchemaVersion) {
        validateValue(value(representation), version)
    }
}
