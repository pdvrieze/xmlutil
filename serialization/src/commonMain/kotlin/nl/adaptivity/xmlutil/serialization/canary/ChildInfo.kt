/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind
import kotlinx.serialization.UnionKind

internal fun childInfoForClassDesc(desc: kotlinx.serialization.SerialDescriptor): Array<SerialDescriptor?> = when (desc.kind) {
    is PrimitiveKind,
        UnionKind.ENUM_KIND,
        UnionKind.OBJECT -> emptyArray()

    StructureKind.MAP,
        StructureKind.LIST -> arrayOf(null, OutputCanary.Companion.DUMMYSERIALDESC)

    UnionKind.POLYMORPHIC -> arrayOfNulls(2)

    else                   -> arrayOfNulls(desc.elementsCount)
}

internal val DUMMYCHILDINFO = OutputCanary.Companion.DUMMYSERIALDESC
