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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeContext
import io.github.pdvrieze.formats.xmlschema.types.T_Facet
import io.github.pdvrieze.formats.xmlschema.types.T_LocalSimpleType
import nl.adaptivity.xmlutil.QName

class ResolvedLocalSimpleType(
    override val rawPart: XSLocalSimpleType,
    override val schema: ResolvedSchemaLike,
    override val mdlContext: SimpleTypeContext,
) : ResolvedLocalType, ResolvedSimpleType, T_LocalSimpleType, SimpleTypeModel.Local {

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val simpleDerivation: ResolvedSimpleType.Derivation
        get() = when (val raw = rawPart.simpleDerivation) {
            is XSSimpleUnion -> ResolvedUnionDerivation(raw, schema, this)
            is XSSimpleList -> ResolvedListDerivation(raw, schema, this)
            is XSSimpleRestriction -> ResolvedSimpleRestriction(raw, schema, this)
            else -> error("Derivations must be union, list or restriction")
        }

    override val model: SimpleTypeModel.Local by lazy { ModelImpl(rawPart, schema, mdlContext) }

    private inner class ModelImpl(
        rawPart: XSLocalSimpleType,
        schema: ResolvedSchemaLike,
        override val mdlContext: SimpleTypeContext
    ) : ResolvedSimpleType.ModelBase(rawPart, schema), SimpleTypeModel.Local {

        override val mdlBaseTypeDefinition: ResolvedSimpleType = when (val d = rawPart.simpleDerivation) {
            is XSSimpleRestriction -> d.base?.let { schema.simpleType(it) }
                ?: ResolvedLocalSimpleType(d.simpleType!!, schema, this@ResolvedLocalSimpleType)
            else ->  AnySimpleType
        }


        override val mdlFacets: List<T_Facet>
            get() = TODO("not implemented")

        override val mdlVariety: SimpleTypeModel.Variety = when (rawPart.simpleDerivation) {
            is XSSimpleList -> SimpleTypeModel.Variety.LIST
            is XSSimpleRestriction -> mdlBaseTypeDefinition.mdlVariety
            is XSSimpleUnion -> SimpleTypeModel.Variety.UNION
        }

        override val mdlPrimitiveTypeDefinition: PrimitiveDatatype = when (val b = mdlBaseTypeDefinition) {
            is PrimitiveDatatype -> b
            else -> b.mdlPrimitiveTypeDefinition
        }

        override val mdlItemTypeDefinition: ResolvedSimpleType? = when(val d= rawPart.simpleDerivation) {
            is XSSimpleList -> when (mdlBaseTypeDefinition) {
                AnySimpleType -> d.itemTypeName?.let { schema.simpleType(it) } ?: ResolvedLocalSimpleType(d.simpleType!!, schema, this@ResolvedLocalSimpleType)
                else -> mdlBaseTypeDefinition.mdlItemTypeDefinition
            }
            else -> null
        }

        override val mdlMemberTypeDefinitions: List<SimpleTypeModel> = run {
            val d = rawPart.simpleDerivation
            when {
                d !is XSSimpleUnion -> emptyList()
                mdlBaseTypeDefinition == AnySimpleType -> d.memberTypes?.map { schema.simpleType(it) }
                    ?: d.simpleTypes.map { ResolvedLocalSimpleType(it, schema, this@ResolvedLocalSimpleType) }
                else -> mdlBaseTypeDefinition.mdlMemberTypeDefinitions
            }
        }
    }

    override fun check() {
        super<ResolvedLocalType>.check()
        checkNotNull(model)
    }
}
