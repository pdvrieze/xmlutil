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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias T_BlockSet = Set<T_BlockSetValues>

@Serializable
enum class T_BlockSetValues {
    @SerialName("extension")
    EXTENSION,

    @SerialName("restriction")
    RESTRICTION,

    @SerialName("substitution")
    SUBSTITUTION
}

fun T_BlockSet.toDerivationSet(): T_DerivationSet {
    return asSequence().mapNotNull { when (it) {
        T_BlockSetValues.EXTENSION -> T_ReducedDerivationControl.EXTENSION
        T_BlockSetValues.RESTRICTION -> T_ReducedDerivationControl.RESTRICTION
        T_BlockSetValues.SUBSTITUTION -> null
    } }.toSet()
}
