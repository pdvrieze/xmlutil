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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.internal.*
import nl.adaptivity.xmlutil.serialization.canary.ExtSerialDescriptor

sealed class SerialKind
sealed class PrimitiveKind
constructor(val primitiveSerializer: KSerializer<*>) : SerialKind() {
    object INT : PrimitiveKind(IntSerializer)
    object UNIT : PrimitiveKind(UnitSerializer)
    object STRING : PrimitiveKind(StringSerializer)
    object DOUBLE : PrimitiveKind(DoubleSerializer)
    object FLOAT : PrimitiveKind(FloatSerializer)
    object BOOLEAN : PrimitiveKind(BooleanSerializer)
    object BYTE : PrimitiveKind(ByteSerializer)
    object CHAR : PrimitiveKind(CharSerializer)
    object ENUM : PrimitiveKind(EnumSerializer(DummyEnum::class))
    object LONG : PrimitiveKind(LongSerializer)
    object SHORT : PrimitiveKind(ShortSerializer)

    open val primitiveSerialDescriptor: SerialDescriptor by lazy {
        ExtSerialDescriptor(primitiveSerializer.serialClassDesc, this, emptyArray())
    }

}

private enum class DummyEnum {}

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


internal fun KSerialClassKind.asSerialKind(): SerialKind {
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
        KSerialClassKind.PRIMITIVE   -> throw SerializationException("Cannot convert a primitive due to lacking information")
        KSerialClassKind.ENUM        -> UnionKind.ENUM
    }
}