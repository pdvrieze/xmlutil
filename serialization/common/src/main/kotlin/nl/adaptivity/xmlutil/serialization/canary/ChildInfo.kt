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

import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerialClassKind
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor

internal fun childInfoForClassDesc(desc: KSerialClassDesc): Array<SerialDescriptor?> = when (desc.kind) {

    KSerialClassKind.PRIMITIVE,
    KSerialClassKind.ENUM,
    KSerialClassKind.OBJECT,
    KSerialClassKind.UNIT  -> emptyArray()
    KSerialClassKind.LIST,
    KSerialClassKind.SET,
    KSerialClassKind.MAP   -> arrayOf(null, OutputCanary.Companion.DUMMYSERIALDESC)
    KSerialClassKind.POLYMORPHIC,
    KSerialClassKind.ENTRY -> arrayOfNulls(2)
    else                   -> arrayOfNulls(desc.associatedFieldsCount)
}

internal val DUMMYCHILDINFO = OutputCanary.Companion.DUMMYSERIALDESC
