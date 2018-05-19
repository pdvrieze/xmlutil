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

package nl.adaptivity.xml.serialization.canary

import kotlinx.serialization.*
import kotlin.reflect.KClass

class CanaryOutput(val isDeep: Boolean = true) : ElementValueOutput() {
    var index: Int = -1
    var kind: KSerialClassKind? = null
    var childInfo: Array<ChildInfo> = emptyArray()

    var type: ChildType = ChildType.UNKNOWN

    private var currentClassDesc: KSerialClassDesc? = null

    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        this.index = index
        return true
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        currentClassDesc = desc

        childInfo = when (desc.kind) {
            KSerialClassKind.MAP,
            KSerialClassKind.SET,
            KSerialClassKind.LIST
                 -> arrayOf(ChildInfo("count"), ChildInfo("values"))

            else -> Array(desc.associatedFieldsCount) {
                ChildInfo(desc.getElementName(it), desc.getAnnotationsForIndex(it))
            }
        }
        return this
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        if (type == ChildType.UNKNOWN) {
            type = ChildType.ELEMENT
        }
    }

    override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        if (index < 0) throw IllegalStateException()
        CanaryOutput(false).also {
            saver.save(it, value)

            // Support lists
            if (index<childInfo.size)
                childInfo[index].kind = it.kind
        }
        index = -1
        currentClassDesc = null
    }

    private fun setCurrentChildType(type: ChildType) {
        if (index < 0) {
            this.type = type
        } else if (index < childInfo.size) {
            childInfo[index].type = type
        }
        index = -1
    }

    override fun writeBooleanValue(value: Boolean) {
        setCurrentChildType(ChildType.BOOLEAN)
    }

    override fun writeByteValue(value: Byte) {
        setCurrentChildType(ChildType.BYTE)
    }

    override fun writeCharValue(value: Char) {
        setCurrentChildType(ChildType.CHAR)
    }

    override fun writeDoubleValue(value: Double) {
        setCurrentChildType(ChildType.DOUBLE)
    }

    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) {
        setCurrentChildType(ChildType.ENUM)
    }

    override fun writeFloatValue(value: Float) {
        setCurrentChildType(ChildType.FLOAT)
    }

    override fun writeIntValue(value: Int) {
        setCurrentChildType(ChildType.INT)
    }

    override fun writeLongValue(value: Long) {
        setCurrentChildType(ChildType.LONG)
    }

    override fun writeNonSerializableValue(value: Any) {
        setCurrentChildType(ChildType.NONSERIALIZABLE)
    }

    override fun writeNotNullMark() {
        childInfo[index].isNullable = true
    }

    override fun writeNullValue() {
        childInfo[index].isNullable = true
        index = -1
    }

    override fun writeShortValue(value: Short) {
        setCurrentChildType(ChildType.SHORT)
    }

    override fun writeStringValue(value: String) {
        setCurrentChildType(ChildType.STRING)
    }

    override fun writeUnitValue() {
        setCurrentChildType(ChildType.UNIT)
    }

    fun extInfo(): ExtInfo {
        return ExtInfo(kind, childInfo, type)
    }
}

