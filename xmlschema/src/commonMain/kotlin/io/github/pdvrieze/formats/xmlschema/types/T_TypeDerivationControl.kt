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
