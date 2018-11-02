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
import kotlinx.serialization.internal.*

interface CanaryCommon {
    var kSerialClassDesc: SerialDescriptor
    var currentChildIndex: Int
    val serialKind: SerialKind get() = kSerialClassDesc.kind
    var isClassNullable: Boolean
    val isComplete: Boolean
    val isDeep: Boolean
    val childDescriptors: Array<SerialDescriptor?>
    var isCurrentElementNullable: Boolean

    fun setCurrentChildType(kind: PrimitiveKind) {
        val index = currentChildIndex
        if (index < 0) {
            kSerialClassDesc = kind.primitiveSerializer.descriptor
        } else if (index < childDescriptors.size) {
            childDescriptors[index] = kind.primitiveSerializer.descriptor.wrapNullable()
        }

        isCurrentElementNullable = false

    }

    fun setCurrentEnumChildType(enumDescription: EnumDescriptor) {
        if (currentChildIndex < 0) {
            kSerialClassDesc = enumDescription
        } else if (currentChildIndex < childDescriptors.size) {
            childDescriptors[currentChildIndex] = enumDescription
            isCurrentElementNullable = false
            currentChildIndex = -1
        }

    }

    fun SerialDescriptor.wrapNullable() = wrapNullable(isCurrentElementNullable)

    fun SerialDescriptor.wrapNullable(newValue: Boolean) = when {
        !isNullable && newValue -> NullableSerialDescriptor(this)
        else                    -> this
    }

    fun serialDescriptor(): ExtSerialDescriptor {
        val kSerialClassDesc = kSerialClassDesc
        return ExtSerialDescriptorImpl(kSerialClassDesc,
                                   serialKind,
                                   Array(childDescriptors.size) {
                                       requireNotNull(childDescriptors[it]) {
                                           "childDescriptors[$it]"
                                       }
                                   })
    }

}

val PrimitiveKind.primitiveSerializer get() = when (this) {
    PrimitiveKind.INT     -> IntSerializer
    PrimitiveKind.UNIT    -> UnitSerializer
    PrimitiveKind.BOOLEAN -> BooleanSerializer
    PrimitiveKind.BYTE    -> ByteSerializer
    PrimitiveKind.SHORT   -> ShortSerializer
    PrimitiveKind.LONG    -> LongSerializer
    PrimitiveKind.FLOAT   -> FloatSerializer
    PrimitiveKind.DOUBLE  -> DoubleSerializer
    PrimitiveKind.CHAR    -> CharSerializer
    PrimitiveKind.STRING  -> StringSerializer
}