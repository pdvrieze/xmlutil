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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationSet
import io.github.pdvrieze.formats.xmlschema.types.T_Facet
import io.github.pdvrieze.formats.xmlschema.types.T_LocalSimpleType
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleType
import nl.adaptivity.xmlutil.QName

class ResolvedLocalSimpleType(
    override val rawPart: XSLocalSimpleType,
    override val schema: ResolvedSchemaLike
) : ResolvedLocalType, ResolvedSimpleType, T_LocalSimpleType, SimpleTypeModel.Local {

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val simpleDerivation: ResolvedSimpleType.Derivation
        get() = when (val raw = rawPart.simpleDerivation) {
            is XSSimpleUnion -> ResolvedUnionDerivation(
                raw,
                schema
            )
            is XSSimpleList -> ResolvedListDerivation(
                raw,
                schema
            )
            is XSSimpleRestriction -> ResolvedSimpleRestriction(
                raw,
                schema
            )
            else -> error("Derivations must be union, list or restriction")
        }

    override val model: SimpleTypeModel.Local by lazy { ModelImpl(rawPart, schema) }

    private class ModelImpl(rawPart: XSLocalSimpleType, schema: ResolvedSchemaLike): SimpleTypeModel.Local {
        override val mdlAnnotations: List<AnnotationModel> = rawPart.annotation.models()
        override val mdlTargetNamespace: VAnyURI?
            get() = TODO("not implemented")
        override val mdlFinal: T_DerivationSet
            get() = TODO("not implemented")
        override val mdlContext: TypeModel
            get() = TODO("not implemented")
        override val mdlBaseTypeDefinition: TypeModel
            get() = TODO("not implemented")
        override val mdlFacets: List<T_Facet>
            get() = TODO("not implemented")
        override val mdlFundamentalFacects: List<T_Facet>
            get() = TODO("not implemented")
        override val mdlVariety: SimpleTypeModel.Variety
            get() = TODO("not implemented")
        override val mdlPrimitiveTypeDefinition: PrimitiveDatatype
            get() = TODO("not implemented")
        override val mdlItemTypeDefinition: SimpleTypeModel
            get() = TODO("not implemented")
        override val mdlMemberTypeDefinitions: List<SimpleTypeModel>
            get() = TODO("not implemented")
    }
}
