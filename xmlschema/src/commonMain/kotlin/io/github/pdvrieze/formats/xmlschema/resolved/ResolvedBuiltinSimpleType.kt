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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSISimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSI_Particle
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

interface ResolvedBuiltinSimpleType : ResolvedGlobalSimpleType, ResolvedBuiltinType, ResolvedSimpleType.Model {

    override val id: Nothing? get() = null
    override val otherAttrs: Map<QName, Nothing> get() = emptyMap()

    override val mdlScope: VSimpleTypeScope.Global get() = VSimpleTypeScope.Global

    override val annotations: List<ResolvedAnnotation> get() = emptyList()

    override val mdlFacets: FacetList

    override val mdlFundamentalFacets: FundamentalFacets

    override val mdlFinal: Set<VDerivationControl.Type> get() = emptySet()

    override val mdlVariety: ResolvedSimpleType.Variety
        get() = ResolvedSimpleType.Variety.ATOMIC
    override val mdlBaseTypeDefinition: ResolvedType
    override val mdlItemTypeDefinition: ResolvedSimpleType?
    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>

    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?
    override fun checkType(checkHelper: CheckHelper) = Unit

}
