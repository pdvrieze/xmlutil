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

import kotlinx.serialization.*

internal fun childInfoForClassDesc(desc: kotlinx.serialization.SerialDescriptor): Array<SerialDescriptor?> = when (desc.kind) {
    is PrimitiveKind,
        UnionKind.ENUM_KIND,
        UnionKind.OBJECT -> emptyArray()

    StructureKind.MAP,
        StructureKind.LIST -> arrayOf(null, DUMMYSERIALDESC)

    UnionKind.POLYMORPHIC -> arrayOfNulls(2)

    else                   -> arrayOfNulls(desc.elementsCount)
}

internal val DUMMYCHILDINFO = DUMMYSERIALDESC

internal object DUMMYSERIALDESC : SerialDescriptor {
    override val name: String get() = throw AssertionError("Dummy descriptors have no names")
    @Suppress("OverridingDeprecatedMember")
    override val kind: SerialKind
        get() = throw AssertionError("Dummy descriptors have no kind")

    override val elementsCount: Int get() = 0

    override fun getElementName(index: Int) = throw AssertionError("No children in dummy")

    override fun getElementIndex(name: String) = throw AssertionError("No children in dummy")

    override fun getEntityAnnotations(): List<Annotation> = emptyList()

    override fun getElementAnnotations(index: Int) = throw AssertionError("No children in dummy")

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        throw AssertionError("No children in dummy")
    }

    override val isNullable: Boolean get() = false

    override fun isElementOptional(index: Int) =
        throw AssertionError("No children in dummy")
}
