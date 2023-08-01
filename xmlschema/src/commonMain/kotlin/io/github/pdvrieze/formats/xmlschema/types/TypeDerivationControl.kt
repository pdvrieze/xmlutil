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

sealed class VDerivationControl(val name: String) {
    sealed class Complex(name: String) : Type(name), ComplexTypeModel.Derivation
    sealed class Type(name: String) : VDerivationControl(name), TypeModel.Derivation

    object RESTRICTION : Complex("restriction"), T_BlockSetValues {
        override fun toString(): String = "RESTRICTION"
    }

    object EXTENSION : Complex("extension"), T_BlockSetValues {
        override fun toString(): String = "EXTENSION"
    }

    object LIST : Type("list") {
        override fun toString(): String = "LIST"
    }

    object UNION : Type("union") {
        override fun toString(): String = "UNION"
    }

    object SUBSTITUTION : VDerivationControl("substitution"), T_BlockSetValues {
        override fun toString(): String = "SUBSTITUTION"
    }

}

fun Set<TypeModel.Derivation>.toDerivationSet(): Set<VDerivationControl.Complex> =
    asSequence()
        .filterIsInstance<VDerivationControl.Complex>()
        .toSet()
