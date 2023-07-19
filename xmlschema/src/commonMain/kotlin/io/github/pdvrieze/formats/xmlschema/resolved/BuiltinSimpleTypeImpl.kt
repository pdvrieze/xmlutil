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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets

abstract class BuiltinSimpleTypeImpl(schemaLike: ResolvedSchemaLike) : ResolvedBuiltinSimpleType, ResolvedSimpleType.Model {

    override val model: ResolvedSimpleType.Model get() = this

    override val mdlItemTypeDefinition: Nothing? get() = null

    override val mdlMemberTypeDefinitions: List<Nothing> get() = emptyList()


    override val mdlFacets: FacetList by lazy {
        when (val d = simpleDerivation) {
            is ResolvedListDerivationBase -> FacetList(
                whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), schema)
            )
            is ResolvedUnionDerivation -> TODO()

            is ResolvedSimpleDerivationBase -> d.baseType.mdlFacets
            is ResolvedSimpleRestrictionBase -> FacetList(d.facets, schemaLike, primitiveType = this as? PrimitiveDatatype)
        }
    }

    /*
                 is XSSimpleList -> FacetList(
                    whiteSpace =
                    ResolvedWhiteSpace(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true), schema)
                )

                is XSSimpleUnion -> FacetList.EMPTY

     */

    open override val mdlFundamentalFacets: FundamentalFacets get() = mdlBaseTypeDefinition.mdlFundamentalFacets

}
