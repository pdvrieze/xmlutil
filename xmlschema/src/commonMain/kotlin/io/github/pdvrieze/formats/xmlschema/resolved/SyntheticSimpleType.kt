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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.ResPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdID
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.QName

class SyntheticSimpleType(
    context: VSimpleTypeScope.Member,
    override val baseType: ResolvedSimpleType<*>,
    override val mdlFacets: FacetList,
    override val mdlFundamentalFacets: FundamentalFacets,
    override val mdlVariety: ResolvedSimpleType.Variety,
    override val mdlPrimitiveTypeDefinition: ResPrimitiveDatatype<*>?,
    override val mdlItemTypeDefinition: ResolvedSimpleType<*>?,
    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType<*>>,
) : ResolvedSimpleType<XsdAnySimple>, ResolvedSimpleType.Model, ExternalSimpleType {
    override val model: ResolvedSimpleType.Model get() = this

    override val name: XsdQName? get() = null
    override val mdlBaseTypeDefinition: ResolvedType get() = baseType

    override val otherAttrs: Map<QName, Nothing> get() = emptyMap()
    override val annotations: List<ResolvedAnnotation> get() = emptyList()
    override val id: XsdID? get() = null
    override val mdlFinal: Set<Nothing> get() = emptySet()
    override val simpleDerivation: Nothing get() = error("Not supported")

    override val mdlScope: VSimpleTypeScope.Local = VSimpleTypeScope.Local(context)

    override fun validate(representation: XsdString, version: SchemaVersion) {
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }

}
