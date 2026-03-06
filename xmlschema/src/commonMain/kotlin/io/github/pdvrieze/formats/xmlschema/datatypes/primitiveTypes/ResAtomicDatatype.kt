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

import io.github.pdvrieze.formats.xmlschema.datatypes.ResSimpleBuiltinRestriction
import io.github.pdvrieze.formats.xmlschema.resolved.*
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdString

interface ResAtomicDatatype<out T: XsdAtomic> : ResolvedBuiltinSimpleType<T>, ResolvedSimpleType.Model,
    ResolvedSimpleType<T>, AnyAtomicType<T> {
    override val isSpecial: Boolean get() = false

    override val model: ResAtomicDatatype<T> get() = this

    abstract override val baseType: ResolvedBuiltinSimpleType<*>
    abstract override val mdlFacets: FacetList
    abstract override val mdlFundamentalFacets: FundamentalFacets
    override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.ATOMIC
    override val mdlPrimitiveTypeDefinition: ResPrimitiveDatatype<T>? get() = null

    override val mdlItemTypeDefinition: ResolvedSimpleType<*>? get() = null
    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType<*>> get() = emptyList()

    override val mdlFinal: Set<VDerivationControl.Type> get() = emptySet()

    override fun value(representation: XsdString): T {
        val normalized = mdlFacets.whiteSpace?.run { value.normalize(representation) } ?: representation
        return valueFromNormalized(normalized)
    }

    override fun validateValue(value: Any, version: SchemaVersion) {
        // Specialised as we know the variant here (it is an atomic value)
        mdlFacets.validateValue(value)
    }

    abstract fun value(maybeValue: XsdAnySimple): XsdAnySimple

    fun valueFromNormalized(normalized: XsdString): T

//    override fun toString(): String = "Builtin:${mdlQName.getLocalPart()}"

    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = ResSimpleBuiltinRestriction(baseType, schema = BuiltinSchemaXmlschema)

}
