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
import io.github.pdvrieze.formats.xmlschema.model.TypeModel

sealed class T_DerivationControl(val name: String) {
    sealed class ComplexBase(name: String) : T_TypeDerivationControl(name), ComplexTypeModel.Derivation

    object RESTRICTION : ComplexBase("restriction"), T_BlockSetValues {
        override fun toString(): String = "RESTRICTION"
    }

    object EXTENSION : ComplexBase("extension"), T_BlockSetValues {
        override fun toString(): String = "EXTENSION"
    }

    object LIST : T_TypeDerivationControl("list") {
        override fun toString(): String = "LIST"
    }

    object UNION : T_TypeDerivationControl("union") {
        override fun toString(): String = "UNION"
    }

    object SUBSTITUTION : T_DerivationControl("substitution"), T_BlockSetValues {
        override fun toString(): String = "SUBSTITUTION"
    }

}


sealed class T_TypeDerivationControl(name: String) : T_DerivationControl(name), TypeModel.Derivation

fun Set<TypeModel.Derivation>.toDerivationSet(): Set<T_DerivationControl.ComplexBase> =
    asSequence()
        .filterIsInstance<T_DerivationControl.ComplexBase>()
        .toSet()
