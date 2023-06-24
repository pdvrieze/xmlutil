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

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_Facet
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleType
import nl.adaptivity.xmlutil.QName

sealed interface ResolvedSimpleType : ResolvedType, T_SimpleType, SimpleTypeModel {
    override val simpleDerivation: Derivation

    val model: SimpleTypeModel

    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations

    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace

    override val mdlBaseTypeDefinition: TypeModel get() = model.mdlBaseTypeDefinition

    override val mdlFacets: List<T_Facet> get() = model.mdlFacets

    override val mdlFundamentalFacets: List<T_Facet> get() = model.mdlFundamentalFacets

    override val mdlVariety: SimpleTypeModel.Variety get() = model.mdlVariety

    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype get() = model.mdlPrimitiveTypeDefinition

    override val mdlItemTypeDefinition: ResolvedSimpleType get() = model.mdlItemTypeDefinition as ResolvedSimpleType

    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        get() = model.mdlMemberTypeDefinitions as List<ResolvedSimpleType>


    override fun check(
        seenTypes: SingleLinkedList<QName>,
        inheritedTypes: SingleLinkedList<QName>
    ) { // TODO maybe move to toplevel

        when (val n = (this as? OptNamedPart)?.name) {
            null -> simpleDerivation.check(SingleLinkedList(), inheritedTypes)
            else -> {
                val qName = n.toQname(schema.targetNamespace)
                simpleDerivation.check(SingleLinkedList(qName), inheritedTypes + qName)
            }
        }
    }

    sealed class Derivation(final override val schema: ResolvedSchemaLike) : T_SimpleType.Derivation, ResolvedPart {
        final override val annotation: XSAnnotation? get() = rawPart.annotation
        final override val id: VID? get() = rawPart.id

        abstract override val rawPart: T_SimpleType.Derivation
        abstract val baseType: ResolvedSimpleType
        abstract fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>)
    }

    sealed class ModelBase(
        rawPart: XSLocalSimpleType,
        protected val schema: ResolvedSchemaLike,
    ) : SimpleTypeModel {
        override val mdlAnnotations: AnnotationModel? = rawPart.annotation.models()
        override val mdlTargetNamespace: VAnyURI? get() = schema.targetNamespace
        override val mdlFundamentalFacets: List<T_Facet>
            get() = TODO("not implemented")

        abstract override val mdlItemTypeDefinition: ResolvedSimpleType?
    }

}
