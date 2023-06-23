/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSFacet
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

interface T_LocalSimpleType: T_SimpleType, T_LocalType {

    override val simpleDerivation: T_SimpleType.Derivation


}

interface T_GlobalSimpleType: T_SimpleType, T_GlobalType {
    val final: Set<T_SimpleDerivationSetElem>
}

interface T_SimpleType: T_SimpleBaseType {
    val simpleDerivation: Derivation

    interface Derivation : XSI_Annotated {

    }

    sealed interface DerivationBase: Derivation

    interface T_Restriction: T_RestrictionType, DerivationBase {
        override val facets: List<XSFacet>
        override val simpleType: T_LocalSimpleType?
    }

    interface T_List: DerivationBase {
        val itemTypeName: @Serializable(with = QNameSerializer::class) QName?
        val simpleType: T_LocalSimpleType?
    }

    interface T_Union: DerivationBase {
        val simpleTypes: List<T_LocalSimpleType>

        val memberTypes: List<@Serializable(with = QNameSerializer::class) QName>?
    }

}

