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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.multiplatform.name

inline fun <reified T> simpleSerialClassDesc(vararg elements: String): SerialDescriptor {
    return SimpleSerialClassDesc(T::class.name, *elements)
}


class SimpleSerialClassDescPrimitive(override val kind: PrimitiveKind, override val name: String) : SerialDescriptor {

    override fun getElementIndex(name: String) = CompositeDecoder.UNKNOWN_NAME

    override fun getElementName(index: Int): String = throw IndexOutOfBoundsException(index.toString())

    override fun isElementOptional(index: Int): Boolean = false
}

class SimpleSerialClassDesc(override val name: String, vararg val elements: String): SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.CLASS

    override fun getElementIndex(name: String): Int {
        val index = elements.indexOf(name)
        return when {
            index >= 0 -> index
            else       -> CompositeDecoder.UNKNOWN_NAME
        }
    }

    override fun getElementName(index: Int) = elements[index]

    override fun isElementOptional(index: Int): Boolean = false

    override val elementsCount: Int get() = elements.size
}

fun SerialDescriptor.withName(name: String): SerialDescriptor = RenameDesc(this, name)

private class RenameDesc(val delegate: SerialDescriptor, override val name:String): SerialDescriptor by delegate

abstract class DelegateSerializer<T>(val delegate: KSerializer<T>): KSerializer<T> {
    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun deserialize(decoder: Decoder): T = delegate.deserialize(decoder)

    override fun patch(decoder: Decoder, old: T): T = delegate.patch(decoder, old)

    override fun serialize(encoder: Encoder, obj: T) = delegate.serialize(encoder, obj)
}