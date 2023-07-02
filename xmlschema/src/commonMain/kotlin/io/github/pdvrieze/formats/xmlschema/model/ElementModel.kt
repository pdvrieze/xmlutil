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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSScopeVariety
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSet

interface ElementModel : IAnnotated, INamed {

    val mdlTypeDefinition: TypeModel
    val mdlTypeTable: TypeTable?
    val mdlNillable: Boolean
    val mdlValueConstraint: ValueConstraintModel?
    val mdlIdentityConstraints: Set<IdentityConstraintModel>
    val mdlSubstitutionGroupAffiliations: Set<Use>
    val mdlDisallowedSubstitutions: T_BlockSet
    val mdlSubstitutionGroupExclusions: Set<out ComplexTypeModel.Derivation>
    val mdlAbstract: Boolean

    interface Decl : ElementModel, INamedDecl, TypeContext {
        val mdlScope: Scope
    }

    interface Use : ElementModel {

    }

    interface Global : Decl, Term {
        override val mdlScope: Scope.Global
    }

    /** Local element with minOccurs=maxOccurs=0 */
    interface LocalNotPresent

    /**
     * Local element without ref and present
     */
    interface Local<T : Local<T>> : Decl, ParticleModel<T>, ParticleModel.BasicTerm {
        override val mdlScope: Scope.Local

        /** Return this */
        override val mdlTerm: T
    }

    interface Ref : Use, ParticleModel<Global> {
        override val mdlTerm: Global
    }

    interface TypeTable {
        val mdlAlternatives: List<TypeAlternativeModel>
        val mdlDefault: TypeAlternativeModel
    }

    interface Scope {
        val variety: XSScopeVariety

        interface Global : Scope {
            override val variety: XSScopeVariety get() = XSScopeVariety.GLOBAL
        }

        interface Local : Scope {
            override val variety: XSScopeVariety get() = XSScopeVariety.LOCAL
            val parent: ElementParentModel
        }
    }

    interface ElementParentModel

    interface Derivations
}
