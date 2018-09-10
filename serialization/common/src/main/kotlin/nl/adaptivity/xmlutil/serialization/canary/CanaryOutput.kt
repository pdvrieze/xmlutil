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
import nl.adaptivity.xmlutil.serialization.canary.Canary.childInfoForClassDesc
import kotlin.reflect.KClass

class CanaryOutput(val isDeep: Boolean = true) : ElementValueOutput() {
    var index: Int = -1
    var kind: KSerialClassKind? = null
    var childInfo: Array<ChildInfo> = emptyArray()
    var classAnnotations: List<Annotation> = emptyList()

    var type: ChildType = ChildType.UNKNOWN

    var isNullable: Boolean = false

    private var currentClassDesc: KSerialClassDesc? = null

    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        if (index<0)
            throw IndexOutOfBoundsException()
        this.index = index
        return true
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        currentClassDesc = desc
        kind = desc.kind
        classAnnotations = desc.getAnnotationsForClass()

        childInfo = childInfoForClassDesc(desc)
        return this
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        if (type == ChildType.UNKNOWN) {
            type = ChildType.ELEMENT
        }
    }

    override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        if (index < 0) throw IllegalStateException()
        if (index <childInfo.size) {
            val poll = Canary.pollInfo(saver)
            val childAtIndex = childInfo[index]
            if (poll!=null) {
                childAtIndex.kind = poll.kind
                childAtIndex.classAnnotations = poll.classAnnotations
                childAtIndex.isNullable = childAtIndex.isNullable || poll.isNullable
                childAtIndex.type = poll.type
                childAtIndex.childCount = poll.childInfo.size
            } else if (isDeep) {
                CanaryOutput(false).also {
                    saver.save(it, value)

                    childAtIndex.kind = it.kind
                    childAtIndex.type = it.type
                    childAtIndex.classAnnotations = it.classAnnotations
                    childAtIndex.childCount = it.childInfo.size
                    childAtIndex.isNullable = childAtIndex.isNullable || it.isNullable
                }
            }
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
        if (index>=0) {
            childInfo[index].isNullable = true
        } else {
            isNullable = true
        }
    }

    override fun writeNullValue() {
        if (index>=0) {
            childInfo[index].isNullable = true
        } else {
            isNullable = true
        }
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
        return ExtInfo(kind, classAnnotations, childInfo, type, isNullable)
    }
}

