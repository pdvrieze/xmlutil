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

package io.github.pdvrieze.formats.xmlschema.model

import io.github.pdvrieze.formats.xmlschema.types.T_DerivationSet

interface ComplexTypeModel : TypeModel, AttributeModel.ParentModel, ElementModel.ParentModel,
    SimpleTypeContext {

    val mdlAbstract: Boolean
    val mdlProhibitedSubstitutions: T_DerivationSet
    override val mdlFinal: Set<Derivation>
    val mdlContentType: ContentType
    val mdlAttributeUses: Set<AttributeModel.Use>
    val mdlAttributeWildcard: WildcardModel
    val mdlDerivationMethod: DerivationMethod

    interface Global : ComplexTypeModel, INamedDecl, TypeModel.Global

    interface Local : ComplexTypeModel, TypeModel.Local {
        val mdlContext: ComplexTypeContext
    }

    interface SimpleContent : ComplexTypeModel {
        override val mdlContentType: ContentType.Simple
    }

    interface ComplexContent : ComplexTypeModel {
    }

    interface ImplicitContent: ComplexTypeModel {
//        override val mdlBaseTypeDefinition get(): AnyType = AnyType
//        override val mdlDerivationMethod: DerivationMethod get() = DerivationMethod.RESTRICION
    }

    interface GlobalSimpleContent: Global, SimpleContent
    interface GlobalComplexContent: Global, ComplexContent
    interface GlobalImplicitContent: Global, ImplicitContent

    interface LocalSimpleContent: Local, SimpleContent
    interface LocalComplexContent: Local, ComplexContent
    interface LocalImplicitContent: Local, ImplicitContent

    enum class DerivationMethod { RESTRICION, EXTENSION }

    interface ContentType {
        val mdlVariety: Variety

        interface Empty : ContentType {
            override val mdlVariety: Variety get() = Variety.EMPTY
        }

        interface Simple : ContentType {
            override val mdlVariety: Variety get() = Variety.EMPTY
            val mdlSimpleTypeDefinition: SimpleTypeModel
        }

        interface ElementBase : ContentType {
            val mdlParticle: ParticleModel<Term>
            val openContent: OpenContentModel?
        }

        interface ElementOnly : ElementBase {
            override val mdlVariety: Variety get() = Variety.EMPTY
        }

        interface Mixed : ElementBase {
            override val mdlVariety: Variety get() = Variety.EMPTY
        }
    }

    interface Derivation: TypeModel.Derivation

    enum class Variety { EMPTY, SIMPLE, ELEMENT_ONLY, MIXED }

}

interface ComplexTypeContext
