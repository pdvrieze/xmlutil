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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.ResPrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSMinLength
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedAnnotation
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedMinLength
import io.github.pdvrieze.formats.xmlschema.resolved.facets.ResolvedWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.xml.schematypes.WhitespaceValue
import io.github.pdvrieze.xml.schematypes.facets.FacetCardinality
import io.github.pdvrieze.xml.schematypes.facets.FacetOrdered
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdQName

/**
 * Space separated for primitives. If the itemType is a Union the members of that union must be atomic.
 *
 * Can be derived using:
 * - length
 * - maxLength
 * - minLength
 * - enumeration
 * - pattern
 * - whiteSpace
 * - assertions
 */
interface ResListDatatype<out T: XsdAnySimple, out E: XsdAnySimple> : ResolvedBuiltinSimpleType<T>,
    ResolvedSimpleType.Model, ResSimpleListType<T, E> {
    override val isSpecial: Boolean get() = false

    abstract override val itemType: ResolvedSimpleType<E>
    val whiteSpace: WhitespaceValue get() = WhitespaceValue.COLLAPSE

    override fun checkType(checkHelper: CheckHelper) {
    }

    override val model: ResListDatatype<*, *>
        get() = this

    override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.LIST

    override val annotations: List<ResolvedAnnotation> get() = emptyList()

    override val mdlFacets: FacetList get() = defaultFacets

    override val mdlFundamentalFacets: FundamentalFacets get() = defaultFundamentalFacets

    override val mdlPrimitiveTypeDefinition: ResPrimitiveDatatype<*>?
        get() = ResAnySimpleType.mdlPrimitiveTypeDefinition

    abstract override val mdlItemTypeDefinition: ResolvedSimpleType<E>

    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType<*>>
        get() = emptyList()

    override val mdlFinal: Set<VDerivationControl.Type> get() = emptySet()
    val itemTypeName: XsdQName? get() = itemType.name
    abstract val simpleType: XSLocalSimpleType?

    companion object {
        val defaultFundamentalFacets: FundamentalFacets = FundamentalFacets(
            ordered = FacetOrdered.Companion.FALSE,
            bounded = false,
            cardinality = FacetCardinality.Companion.COUNTABLY_INFINITE,
            numeric = false
        )

        val defaultFacets: FacetList = FacetList(
            minLength = ResolvedMinLength(XSMinLength(1u)),
            whiteSpace = ResolvedWhiteSpace(XSWhiteSpace(WhitespaceValue.COLLAPSE))
        )
    }

}
