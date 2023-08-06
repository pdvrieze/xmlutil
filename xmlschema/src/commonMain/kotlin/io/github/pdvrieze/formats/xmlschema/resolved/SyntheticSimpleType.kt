/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import nl.adaptivity.xmlutil.QName

class SyntheticSimpleType(
    context: VSimpleTypeScope.Member,
    override val mdlBaseTypeDefinition: ResolvedSimpleType,
    override val mdlFacets: FacetList,
    override val mdlFundamentalFacets: FundamentalFacets,
    override val mdlVariety: ResolvedSimpleType.Variety,
    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?,
    override val mdlItemTypeDefinition: ResolvedSimpleType?,
    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>,
    override val schema: ResolvedSchemaLike,
) : ResolvedSimpleType, ResolvedSimpleType.Model {
    override val model: ResolvedSimpleType.Model get() = this

    override val otherAttrs: Map<QName, Nothing> get() = emptyMap()
    override val annotations: List<ResolvedAnnotation> get() = emptyList()
    override val id: VID? get() = null
    override val mdlFinal: Set<Nothing> get() = emptySet()
    override val simpleDerivation: Nothing get() = error("Not supported")

    override val rawPart: Nothing get() = error("Not supported")

    override val mdlScope: VSimpleTypeScope.Local = VSimpleTypeScope.Local(context)

    override fun validate(representation: VString) {
        mdlFacets.validate(mdlPrimitiveTypeDefinition, representation)
    }

}
