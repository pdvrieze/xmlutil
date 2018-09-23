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
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.compat.asSerialKind
import kotlin.reflect.KClass

internal class InputCanary(val deep: Boolean = true): ElementValueInput() {
    lateinit var kSerialClassDesc: KSerialClassDesc

    var kind: KSerialClassKind? = null

    internal var childInfo: Array<ChildInfo?> = emptyArray()
    var classAnnotations: List<Annotation> = emptyList()

    var currentChildIndex = -1

    private var type: ChildType = ChildType.UNKNOWN

    var isClassNullable = false
    var isCurrentElementNullable = false

    var canBeComplete: Boolean = true

    var complete: Boolean = false


    override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
        if (currentChildIndex<0) { // This is called at every load as we restart load every time
            kSerialClassDesc = desc
            kind = desc.kind
            childInfo = childInfoForClassDesc(desc)
            classAnnotations = desc.getAnnotationsForClass()
        }
        return this
    }

    override fun readEnd(desc: KSerialClassDesc) {
        if (type == ChildType.UNKNOWN) {
            type = ChildType.CLASS
        }
        if (canBeComplete) complete=true
        throw SuspendException(true)
    }

    override fun <T : Any> updateNullableSerializableValue(loader: KSerialLoader<T?>,
                                                           desc: KSerialClassDesc,
                                                           old: T?): T? {
        readNotNullMark()
        readSerializableValue(loader)
    }

    override fun <T> updateSerializableValue(loader: KSerialLoader<T>, desc: KSerialClassDesc, old: T): T {
        readSerializableValue(loader)
    }

    override fun <T> readSerializableValue(loader: KSerialLoader<T>): Nothing {
        val polledDesc = Canary.pollDesc(loader)
        if (polledDesc!=null) {
            childInfo[currentChildIndex] = ChildInfo(polledDesc, isCurrentElementNullable)
        } else if(deep) {
            val childIn = InputCanary(false)

            Canary.load(childIn, loader)

            val newDesc = childIn.serialDescriptor()
            childInfo[currentChildIndex] = ChildInfo(newDesc, isCurrentElementNullable)
            if (childIn.complete) {
                Canary.registerDesc(loader, newDesc)
            } else {
                canBeComplete = false
            }
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
        assert(type.isPrimitive)
        val index = currentChildIndex
        if (index < 0) {
            kSerialClassDesc = type.primitiveSerializer.serialClassDesc
            this.type = type
        } else if (index < childInfo.size) {
            childInfo[index] = ChildInfo(type.primitiveSerialDescriptor, isCurrentElementNullable)
        }
        currentChildIndex = -1
        isCurrentElementNullable = false
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
        if (currentChildIndex == 0) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (kind) {
                KSerialClassKind.LIST,
                KSerialClassKind.SET,
                KSerialClassKind.MAP -> {
                    if (childInfo.isNotEmpty()) {
                        childInfo[0] = ChildInfo(ChildType.INT.primitiveSerialDescriptor, false)
                    }
                    currentChildIndex = -1
                    isCurrentElementNullable = false
                    return 1 // One simulated element
                }
            }
        }
        setCurrentChildType(ChildType.INT)
    }

    override fun readLongValue(): Long {
        setCurrentChildType(ChildType.LONG)
    }

    override fun readNotNullMark(): Boolean {
        if (currentChildIndex>=0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
        return true
    }

    override fun readNullValue(): Nothing? {
        if (currentChildIndex>=0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
        return null
    }

/*
    // Don't override this as we want to use the default that reads a not-null mark and then the value
    override fun readNullableValue(): Any? {
        if (currentChildIndex>=0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
        return null
    }
*/

    override fun readShortValue(): Short {
        setCurrentChildType(ChildType.SHORT)
    }

    override fun readStringValue(): String {
        // We need to special case the Polymorphic serializer as it requires the children to be read in order
        // This mean bailing out to continue at the next index is invalid.
        if (kind == KSerialClassKind.POLYMORPHIC && currentChildIndex==0) {
            childInfo[0] = ChildInfo(ChildType.STRING.primitiveSerialDescriptor)
            return "nl.adaptivity.xmlutil.serialization.canary.CanaryInput\$Dummy"
        }
        setCurrentChildType(ChildType.STRING)
    }

    override fun readUnitValue() {
        setCurrentChildType(ChildType.UNIT)
    }

    override fun readValue(): Any {
        setCurrentChildType(ChildType.NONSERIALIZABLE)
    }

    fun serialDescriptor(): SerialDescriptor {
        val kSerialClassDesc = kSerialClassDesc
        return ExtSerialDescriptor(requireNotNull(kSerialClassDesc, {"parentClassDesc"}),
                                   kSerialClassDesc.kind.asSerialKind(type),
                                   BooleanArray(childInfo.size) { childInfo[it]?.isNullable ?: false },
                                   Array(childInfo.size) {
                                       requireNotNull(requireNotNull(childInfo[it],{"childInfo"}).descriptor, {"descriptor"}) })
    }

    internal class SuspendException(val finished: Boolean = false): Exception()

    @Serializable
    class Dummy(val dummyVal: String)
}