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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*

/**
 * Choice: T_LocalElement, T_GroupRef, E_Choice, E_Sequence, E_Any
 */
interface G_NestedParticle {

    val nestedParticle: Base

    interface Base
    sealed interface SealedBase : Base
    interface Element : SealedBase, T_LocalElement
    interface Group : SealedBase, T_GroupRef
    interface Choice : SealedBase
    interface Sequence : SealedBase
    interface Any : SealedBase
}

interface GX_NestedParticles {
    val elements: List<T_LocalElement>
    val groups: List<T_GroupRef>
    val choices: List<T_Choice>
    val sequences: List<T_Sequence>
    val anys: List<T_AnyElement>
}
