/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialKind
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
                                       Array(childDescriptors.size) {
                                           requireNotNull(childDescriptors[it]) {
                                               "childDescriptors[$it]"
                                           }
                                       })
    }

}

val PrimitiveKind.primitiveSerializer
    get() = when (this) {
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