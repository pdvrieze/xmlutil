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

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.xml.schematypes.facets.*
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdQName

object ResAnySimpleType : ResolvedBuiltinSimpleType<XsdAnySimple>,
    AnySimpleType<XsdAnySimple> {

    override val isSpecial: Boolean get() = true

    override val baseType: ResAnyType get() = ResAnyType
    override val mdlBaseTypeDefinition: ResolvedType get() = baseType

    override val name: XsdQName get() = AnySimpleType.Instance.name

    override val ordered: FacetOrdered get() = AnySimpleType.Instance.ordered
    override val bounded: FacetBounded get() = AnySimpleType.Instance.bounded
    override val cardinality: FacetCardinality get() = AnySimpleType.Instance.cardinality
    override val numeric: FacetNumeric get() = AnySimpleType.Instance.numeric

    override val constrainingFacets: List<ConstrainingFacet>
        get() = AnySimpleType.Instance.constrainingFacets

    override val simpleDerivation: ResolvedSimpleType.Derivation get() = AnySimpleTypeRestriction
    override val model: ResAnySimpleType get() = this
    override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.NIL
    override val mdlPrimitiveTypeDefinition: Nothing? get() = null
    override val mdlItemTypeDefinition: Nothing? get() = null
    override val mdlMemberTypeDefinitions: List<Nothing> get() = emptyList()

    override val mdlFacets: FacetList get() = FacetList.Companion.EMPTY
    override val annotations: List<ResolvedAnnotation> get() = emptyList()
    override val mdlFundamentalFacets: FundamentalFacets = FundamentalFacets(
        ordered = FacetOrdered.FALSE,
        bounded = false,
        cardinality = FacetCardinality.COUNTABLY_INFINITE,
        numeric = false,
    )

    override fun validateValue(value: Any, version: SchemaVersion) {
        // Valid for any value
    }

    override fun validate(representation: VString, version: SchemaVersion) {
        // any representation is valid
    }

    override fun value(representation: VString): Any = representation
}
