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

interface AttributeModel : IAnnotated, INamed {

    interface Decl : AttributeModel, INamedDecl {
        val mdlTypeDefinition: SimpleTypeModel
        val mdlScope: ScopeModel
        val mdlValueConstraint: ValueConstraintModel?
        val mdlInheritable: Boolean
    }

    interface Use : IAnnotated {
        val mdlRequired: Boolean
        val mdlAttributeDeclaration: Decl
        val mdlValueConstraint: ValueConstraintModel?
        val mdlInheritable: Boolean
    }

    interface Global : Decl {
    }

    interface Ref : Use

    interface ProhibitedRef : IAnnotated

    interface Local : Decl, Use {
        override val mdlAttributeDeclaration: Decl get() = this
    }

    interface ScopeModel {
        val mdlVariety: XSScopeVariety

        interface Global : ScopeModel {
            override val mdlVariety: XSScopeVariety get() = XSScopeVariety.GLOBAL
        }

        interface Local : ScopeModel {
            override val mdlVariety: XSScopeVariety get() = XSScopeVariety.LOCAL
            val parent: AttributeParentModel
        }
    }

    interface AttributeParentModel

}

