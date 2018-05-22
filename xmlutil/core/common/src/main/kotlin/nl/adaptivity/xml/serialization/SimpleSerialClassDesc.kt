/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml.serialization

import kotlinx.serialization.*
import nl.adaptivity.util.multiplatform.name

inline fun <reified T> simpleSerialClassDesc(): KSerialClassDesc {
    return SimpleSerialClassDescPrimitive(T::class.name)
}

inline fun <reified T> simpleSerialClassDesc(vararg elements: String): KSerialClassDesc {
    return SimpleSerialClassDesc(T::class.name, *elements)
}


class SimpleSerialClassDescPrimitive(override val name: String) : KSerialClassDesc {
    override val kind: KSerialClassKind get() = KSerialClassKind.PRIMITIVE

    override fun getElementIndex(name: String) = KInput.UNKNOWN_NAME

    override fun getElementName(index: Int): String = throw IndexOutOfBoundsException(index.toString())
}

class SimpleSerialClassDesc(override val name: String, vararg val elements: String): KSerialClassDesc {
    override val kind: KSerialClassKind get() = KSerialClassKind.CLASS

    override fun getElementIndex(name: String): Int {
        val index = elements.indexOf(name)
        return when {
            index >= 0 -> index
            else       -> KInput.UNKNOWN_NAME
        }
    }

    override fun getElementName(index: Int) = elements[index]

    override val associatedFieldsCount: Int get() = elements.size
}

fun KSerialClassDesc.withName(name: String): KSerialClassDesc = RenameDesc(this, name)

private class RenameDesc(val delegate: KSerialClassDesc, override val name:String): KSerialClassDesc by delegate

abstract class DelegateSerializer<T>(val delegate: KSerializer<T>): KSerializer<T> {
    override val serialClassDesc: KSerialClassDesc get() = delegate.serialClassDesc

    override fun load(input: KInput) = delegate.load(input)

    override fun save(output: KOutput, obj: T) = delegate.save(output, obj)

    override fun update(input: KInput, old: T) = delegate.update(input, old)
}