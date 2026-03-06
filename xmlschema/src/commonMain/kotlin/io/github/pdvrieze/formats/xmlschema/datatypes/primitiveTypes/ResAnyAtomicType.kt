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

import io.github.pdvrieze.formats.xmlschema.datatypes.ResAnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.ResSimpleBuiltinRestriction
import io.github.pdvrieze.formats.xmlschema.resolved.BuiltinSchemaXmlschema
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinAtomicType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleRestrictionBase
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.xml.schematypes.facets.*
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import io.github.pdvrieze.xml.schematypes.values.XsdString

object ResAnyAtomicType : ResolvedBuiltinAtomicType<XsdAtomic>,
    AnyAtomicType<XsdAtomic> {
    override val isSpecial: Boolean get() = true
    override val baseType: ResAnySimpleType get() = ResAnySimpleType

    override val simpleDerivation: ResolvedSimpleRestrictionBase =
        ResSimpleBuiltinRestriction(ResAnySimpleType, schema = BuiltinSchemaXmlschema)

    override val mdlFacets: FacetList get() = FacetList.Companion.EMPTY

    override val name: XsdQName get() = AnyAtomicType.Instance.name
    override val ordered: FacetOrdered get() = AnyAtomicType.Instance.ordered
    override val bounded: FacetBounded get() = AnyAtomicType.Instance.bounded
    override val cardinality: FacetCardinality get() = AnyAtomicType.Instance.cardinality
    override val numeric: FacetNumeric get() = AnyAtomicType.Instance.numeric
    override val constrainingFacets: List<ConstrainingFacet> get() = AnyAtomicType.Instance.constrainingFacets

    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = FacetOrdered.FALSE,
        bounded = false,
        cardinality = FacetCardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun value(maybeValue: XsdAnySimple): XsdAnySimple {
        return maybeValue as? XsdAtomic ?: XsdString(maybeValue.xmlString)
    }

    override fun valueFromNormalized(normalized: XsdString): XsdAtomic {
        TODO("not implemented")
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        error("Atomic is not directly usable")
    }

    override fun validate(representation: XsdString, version: SchemaVersion) {
        error("Atomic is not directly usable")
    }
}
