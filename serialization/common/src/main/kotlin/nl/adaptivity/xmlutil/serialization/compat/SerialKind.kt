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

package nl.adaptivity.xmlutil.serialization.compat

import kotlinx.serialization.KSerialClassKind
import nl.adaptivity.xmlutil.serialization.canary.ChildType

sealed class SerialKind
sealed class PrimitiveKind : SerialKind() {
    object INT : PrimitiveKind()
    object UNIT : PrimitiveKind()
    object STRING : PrimitiveKind()
    object DOUBLE : PrimitiveKind()
    object FLOAT : PrimitiveKind()
    object BOOLEAN : PrimitiveKind()
    object BYTE : PrimitiveKind()
    object CHAR : PrimitiveKind()
    object ENUM : PrimitiveKind()
    object LONG : PrimitiveKind()
    object SHORT : PrimitiveKind()
}

sealed class StructureKind : SerialKind() {
    object CLASS : StructureKind()
    object LIST : StructureKind()
    object MAP : StructureKind()
}

sealed class UnionKind : SerialKind() {
    object OBJECT : UnionKind()
    object ENUM : UnionKind()
    object SEALED : UnionKind()
    object POLYMORPHIC : UnionKind()
}


internal fun KSerialClassKind.asSerialKind(type: ChildType?): SerialKind {
    return when (this) {
        KSerialClassKind.CLASS       -> StructureKind.CLASS
        KSerialClassKind.OBJECT      -> UnionKind.OBJECT
        KSerialClassKind.UNIT        -> PrimitiveKind.UNIT
        KSerialClassKind.SEALED      -> UnionKind.SEALED
        KSerialClassKind.LIST        -> StructureKind.LIST
        KSerialClassKind.SET         -> StructureKind.LIST
        KSerialClassKind.MAP         -> StructureKind.MAP
        KSerialClassKind.ENTRY       -> StructureKind.CLASS
        KSerialClassKind.POLYMORPHIC -> UnionKind.POLYMORPHIC
        KSerialClassKind.PRIMITIVE   -> when (type) {

            ChildType.DOUBLE  -> PrimitiveKind.DOUBLE
            ChildType.INT     -> PrimitiveKind.INT
            ChildType.FLOAT   -> PrimitiveKind.FLOAT
            ChildType.STRING  -> PrimitiveKind.STRING
            ChildType.BOOLEAN -> PrimitiveKind.BOOLEAN
            ChildType.BYTE    -> PrimitiveKind.BYTE
            ChildType.UNIT    -> PrimitiveKind.UNIT
            ChildType.CHAR    -> PrimitiveKind.CHAR
            ChildType.ENUM    -> PrimitiveKind.ENUM
            ChildType.LONG    -> PrimitiveKind.LONG
            ChildType.SHORT   -> PrimitiveKind.SHORT
            ChildType.ELEMENT -> StructureKind.CLASS
            else              -> throw IllegalArgumentException("Serializing non-primitive primitive: $type")
        }
        KSerialClassKind.ENUM        -> UnionKind.ENUM
    }
}