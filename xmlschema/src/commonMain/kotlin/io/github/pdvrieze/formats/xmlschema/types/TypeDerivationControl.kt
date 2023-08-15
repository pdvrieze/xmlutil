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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface VDerivationControl {
    val name: String
    @Serializable
    sealed class Complex : Type {
        constructor(name: String) : super(name)
    }

    @Serializable
    sealed class Type(override val name: String) : VDerivationControl

    @Serializable
    @SerialName("restriction")
    object RESTRICTION : Complex("restriction"), T_BlockSetValues {
        override fun toString(): String = "RESTRICTION"
    }

    @Serializable
    @SerialName("extension")
    object EXTENSION : Complex("extension"), T_BlockSetValues {
        override fun toString(): String = "EXTENSION"
    }

    @Serializable
    object LIST : Type("list") {
        override fun toString(): String = "LIST"
    }

    @Serializable
    object UNION : Type("union") {
        override fun toString(): String = "UNION"
    }

    @Serializable
    object SUBSTITUTION : VDerivationControl, T_BlockSetValues {
        override val name: String get() = "substitution"

        override fun toString(): String = "SUBSTITUTION"
    }

    sealed interface T_BlockSetValues : VDerivationControl
}

fun Set<VDerivationControl>.toDerivationSet(): Set<VDerivationControl.Complex> =
    asSequence()
        .filterIsInstance<VDerivationControl.Complex>()
        .toSet()
