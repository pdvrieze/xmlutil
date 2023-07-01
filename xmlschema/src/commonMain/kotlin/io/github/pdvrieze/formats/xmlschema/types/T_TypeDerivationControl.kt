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

import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class T_TypeDerivationControl(val name: String) : SimpleTypeModel.Derivation {
    sealed class ComplexBase(name: String) : T_TypeDerivationControl(name), ComplexTypeModel.Derivation

    object RESTRICTION : ComplexBase("restriction")

    object EXTENSION : ComplexBase("extension")

    object LIST : T_TypeDerivationControl("list")

    object UNION : T_TypeDerivationControl("union")

}

fun Set<TypeModel.Derivation>.toDerivationSet(): Set<T_TypeDerivationControl.ComplexBase> =
    asSequence()
        .filterIsInstance<T_TypeDerivationControl.ComplexBase>()
        .toSet()
