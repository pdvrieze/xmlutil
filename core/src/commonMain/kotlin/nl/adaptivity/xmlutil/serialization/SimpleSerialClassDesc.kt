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

    override fun deserialize(input: Decoder): T = delegate.deserialize(input)

    override fun patch(input: Decoder, old: T): T = delegate.patch(input, old)

    override fun serialize(output: Encoder, obj: T) = delegate.serialize(output, obj)
}