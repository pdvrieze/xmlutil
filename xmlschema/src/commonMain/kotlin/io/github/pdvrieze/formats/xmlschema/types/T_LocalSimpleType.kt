/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

interface T_LocalSimpleType : T_SimpleType, T_LocalType {

    override val simpleDerivation: T_SimpleType.Derivation


}

interface T_GlobalSimpleType : T_SimpleType, T_GlobalType {
    val final: Set<TypeModel.Derivation>
}

interface T_SimpleType : T_SimpleBaseType {
    val simpleDerivation: Derivation

    interface Derivation : XSI_Annotated {

    }

    sealed interface DerivationBase : Derivation

    interface T_Restriction : T_RestrictionType, DerivationBase {
        override val facets: List<XSFacet>
        override val simpleType: T_LocalSimpleType?
    }

    interface T_List : DerivationBase {
        val itemTypeName: @Serializable(with = QNameSerializer::class) QName?
        val simpleType: T_LocalSimpleType?
    }

    interface T_Union : DerivationBase {
        val simpleTypes: List<T_LocalSimpleType>

        val memberTypes: List<@Serializable(with = QNameSerializer::class) QName>?
    }

}

