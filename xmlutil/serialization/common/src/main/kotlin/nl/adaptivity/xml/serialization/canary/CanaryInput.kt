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
import kotlinx.serialization.internal.StringSerializer
import kotlin.reflect.KClass

internal class CanaryInput(val deep: Boolean = true): ElementValueInput() {
    var kind: KSerialClassKind? = null

    var childInfo: Array<ChildInfo> = emptyArray()

    var currentChildIndex = -1;

    private var type: ChildType = ChildType.UNKNOWN

    override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
        if (currentChildIndex<0) { // This is called at every load as we restart load every time
            kind = desc.kind
            childInfo = Canary.childInfoForClassDesc(desc)
        }
        return this
    }

    override fun readEnd(desc: KSerialClassDesc) {
        if (type == ChildType.UNKNOWN) {
            type = ChildType.ELEMENT
        }
        throw SuspendException(true)
    }

    override fun <T : Any> updateNullableSerializableValue(loader: KSerialLoader<T?>,
                                                           desc: KSerialClassDesc,
                                                           old: T?): T? {
        readSerializableValue(loader)
    }

    override fun <T> updateSerializableValue(loader: KSerialLoader<T>, desc: KSerialClassDesc, old: T): T {
        readSerializableValue(loader)
    }

    override fun <T> readSerializableValue(loader: KSerialLoader<T>): Nothing {
        val extInfo = Canary.pollInfo(loader)
        if (extInfo!=null) {
            val currentInfo = childInfo[currentChildIndex]
            extInfo.kind?.let{ currentInfo.kind }
            currentInfo.type = extInfo.type
            currentInfo.childCount = extInfo.childInfo.size
        } else if(deep) {
            val childIn = CanaryInput(false)
            Canary.load(childIn, loader)
            val currentInfo = childInfo[currentChildIndex]
            childIn.kind?.let { currentInfo.kind = it }
            currentInfo.type = childIn.type
            currentInfo.childCount = childIn.childInfo.size
        }
        throw SuspendException()
    }

    override fun readElement(desc: KSerialClassDesc): Int {
        currentChildIndex++
        if (currentChildIndex<childInfo.size) {
            return currentChildIndex
        } else {
            return KInput.READ_DONE
        }
    }

    private fun setCurrentChildType(type: ChildType):Nothing {
        val index = currentChildIndex
        if (index < 0) {
            this.type = type
            this.kind == KSerialClassKind.PRIMITIVE
        } else if (index < childInfo.size) {
            childInfo[index].type = type
        }
        throw SuspendException(childInfo.isEmpty())
    }

    override fun readBooleanValue(): Boolean {
        setCurrentChildType(ChildType.BOOLEAN)
    }

    override fun readByteValue(): Byte {
        setCurrentChildType(ChildType.BYTE)
    }

    override fun readCharValue(): Char {
        setCurrentChildType(ChildType.CHAR)
    }

    override fun readDoubleValue(): Double {
        setCurrentChildType(ChildType.DOUBLE)
    }

    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T {
        setCurrentChildType(ChildType.ENUM)
    }

    override fun readFloatValue(): Float {
        setCurrentChildType(ChildType.FLOAT)
    }

    override fun readIntValue(): Int {
        setCurrentChildType(ChildType.INT)
    }

    override fun readLongValue(): Long {
        setCurrentChildType(ChildType.LONG)
    }

    override fun readNotNullMark(): Boolean {
        childInfo[currentChildIndex].isNullable = true
        return true
    }

    override fun readNullValue(): Nothing? {
        childInfo[currentChildIndex].isNullable = true
        return null
    }

    override fun readNullableValue(): Any? {
        childInfo[currentChildIndex].isNullable = true
        return null
    }

    override fun readShortValue(): Short {
        setCurrentChildType(ChildType.SHORT)
    }

    override fun readStringValue(): String {
        // We need to special case the Polymorphic serializer as it requires the children to be read in order
        // This mean bailing out to continue at the next index is invalid.
        if (kind == KSerialClassKind.POLYMORPHIC && currentChildIndex==0) {
            childInfo[currentChildIndex].type = ChildType.STRING
            return "nl.adaptivity.xml.serialization.canary.CanaryInput\$Dummy"
        }
        setCurrentChildType(ChildType.STRING)
    }

    override fun readUnitValue() {
        setCurrentChildType(ChildType.UNIT)
    }

    override fun readValue(): Any {
        setCurrentChildType(ChildType.NONSERIALIZABLE)
    }

    fun extInfo(): ExtInfo {
        return ExtInfo(kind, childInfo, type)
    }

    internal class SuspendException(val finished: Boolean = false): Exception()

    @Serializable
    private class Dummy(val dummyVal: String)
}