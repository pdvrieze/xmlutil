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
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.compat.asSerialKind

interface CanaryCommon {
    var kSerialClassDesc: KSerialClassDesc
    var currentChildIndex: Int
    var type: ChildType
    var isClassNullable: Boolean
    val isComplete: Boolean
    val isDeep: Boolean
    val childDescriptors: Array<SerialDescriptor?>
    var isCurrentElementNullable: Boolean

    fun setCurrentChildType(type: ChildType): Unit {
        assert(type.isPrimitive)
        val index = currentChildIndex
        if (index < 0) {
            kSerialClassDesc = type.primitiveSerializer.serialClassDesc
            this.type = type
        } else if (index < childDescriptors.size) {
            childDescriptors[index] = type.primitiveSerialDescriptor.wrapNullable()
        }

        isCurrentElementNullable = false
    }

    fun SerialDescriptor.wrapNullable() = wrapNullable(isCurrentElementNullable)

    fun SerialDescriptor.wrapNullable(newValue: Boolean) = when {
        !isNullable && newValue -> NullableSerialDescriptor(this)
        else                    -> this
    }

    fun serialDescriptor(): SerialDescriptor {
        val kSerialClassDesc = kSerialClassDesc
        return ExtSerialDescriptor(kSerialClassDesc,
                                   kSerialClassDesc.kind.asSerialKind(type),
                                   Array(childDescriptors.size) {
                                       requireNotNull(childDescriptors[it], {
                                           "childDescriptors[$it]"
                                       })
                                   })
    }

}