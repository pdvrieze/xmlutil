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

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedIdentityConstraint
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedTypeContext
import io.github.pdvrieze.formats.xmlschema.types.VScopeVariety
import io.github.pdvrieze.formats.xmlschema.types.VBlockSet
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSetValues

interface ElementModel : IAnnotated, IOptNamed {

    val mdlTypeDefinition: TypeModel
    val mdlTypeTable: TypeTable?
    val mdlNillable: Boolean
    val mdlValueConstraint: ValueConstraintModel?
    val mdlIdentityConstraints: Set<ResolvedIdentityConstraint>
    val mdlSubstitutionGroupAffiliations: List<Use>
    val mdlDisallowedSubstitutions: VBlockSet
    val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues>
    val mdlAbstract: Boolean

    interface Decl : ElementModel, INamedDecl, ResolvedTypeContext {
        val mdlScope: Scope
    }

    interface Use : ElementModel {

    }

    interface Global : Decl, Term, INamed {
        override val mdlScope: Scope.Global
    }

    /** Local element with minOccurs=maxOccurs=0 */
    interface LocalNotPresent

    /**
     * Local element without ref and present
     */
    interface Local<T : Local<T>> : ElementModel, IOptNamedDecl, ParticleModel<T>, ParticleModel.BasicTerm {
        val mdlScope: Scope.Local

        /** Return this */
        override val mdlTerm: T
    }

    interface Ref : Use, ParticleModel<Global> {
        override val mdlTerm: Global
    }

    interface TypeTable {
        fun isEquivalent(other: TypeTable): Boolean {
            //TODO("not implemented")
            return mdlAlternatives.size == other.mdlAlternatives.size
        }

        val mdlAlternatives: List<TypeAlternativeModel>
        val mdlDefault: TypeAlternativeModel
    }

    interface Scope {
        val variety: VScopeVariety

        interface Global : Scope {
            override val variety: VScopeVariety get() = VScopeVariety.GLOBAL
        }

        interface Local : Scope {
            override val variety: VScopeVariety get() = VScopeVariety.LOCAL
            val parent: Any?
        }
    }

    interface ElementParentModel

    interface Derivations
}

