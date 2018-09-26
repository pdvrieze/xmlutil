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
import nl.adaptivity.xmlutil.serialization.compat.PrimitiveKind
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.compat.SerialKind
import nl.adaptivity.xmlutil.serialization.compat.asSerialKind
import kotlin.reflect.KClass

internal class InputCanary(override val isDeep: Boolean = true) : ElementValueInput(), CanaryCommon {
    override lateinit var kSerialClassDesc: KSerialClassDesc

    override var childDescriptors: Array<SerialDescriptor?> = emptyArray()

    override var currentChildIndex = -1

    override lateinit var serialKind: SerialKind

    override var isClassNullable = false
    override var isCurrentElementNullable = false

    var canBeComplete: Boolean = true

    override var isComplete: Boolean = false

    /**
     * Protection against premature invocation of readEnd by the serializer
     */
    var suspending = false

    override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
        suspending = false
        if (currentChildIndex < 0) { // This is called at every load as we restart load every time
            kSerialClassDesc = desc
            serialKind = desc.kind.asSerialKind()
            childDescriptors = childInfoForClassDesc(desc)
        }
        return this
    }

    fun doSuspend(couldBeFinished: Boolean = false): Nothing {
        val oldSuspending = suspending
        suspending = true
        throw SuspendException((! oldSuspending) && couldBeFinished)
    }

    override fun readEnd(desc: KSerialClassDesc) {
        if (canBeComplete) isComplete = true
        doSuspend(true)
    }

    override fun <T : Any> updateNullableSerializableValue(
            loader: KSerialLoader<T?>,
            desc: KSerialClassDesc,
            old: T?
                                                          ): T? {
        readNotNullMark()
        readSerializableValue(loader)
    }

    override fun <T> updateSerializableValue(loader: KSerialLoader<T>, desc: KSerialClassDesc, old: T): T {
        readSerializableValue(loader)
    }

    override fun <T> readSerializableValue(loader: KSerialLoader<T>): Nothing {
        val polledDesc = Canary.pollDesc(loader)
        if (polledDesc != null) {
            childDescriptors[currentChildIndex] = polledDesc.wrapNullable()
        } else if (isDeep) {
            val childIn = InputCanary(true)

            Canary.load(childIn, loader)

            val newDesc = childIn.serialDescriptor()
            childDescriptors[currentChildIndex] = newDesc.wrapNullable()
            if (childIn.isComplete) {
                Canary.registerDesc(loader, newDesc)
            } else {
                canBeComplete = false
            }
        } else {
            canBeComplete = false
        }
        doSuspend()
    }

    override fun readElement(desc: KSerialClassDesc): Int {
        currentChildIndex++
        if (currentChildIndex < childDescriptors.size) {
            return currentChildIndex
        } else {
            return KInput.READ_DONE
        }
    }

    override fun setCurrentChildType(kind: PrimitiveKind): Nothing {
        super.setCurrentChildType(kind)
        doSuspend(childDescriptors.isEmpty())
    }

    override fun readBooleanValue(): Boolean {
        setCurrentChildType(PrimitiveKind.BOOLEAN)
    }

    override fun readByteValue(): Byte {
        setCurrentChildType(PrimitiveKind.BYTE)
    }

    override fun readCharValue(): Char {
        setCurrentChildType(PrimitiveKind.CHAR)
    }

    override fun readDoubleValue(): Double {
        setCurrentChildType(PrimitiveKind.DOUBLE)
    }

    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T {
        setCurrentEnumChildType(enumClass)
        doSuspend(childDescriptors.isEmpty())
    }

    override fun readFloatValue(): Float {
        setCurrentChildType(PrimitiveKind.FLOAT)
    }

    override fun readIntValue(): Int {
        if (currentChildIndex == 0) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (kSerialClassDesc.kind) {
                KSerialClassKind.LIST,
                KSerialClassKind.SET,
                KSerialClassKind.MAP -> {
                    if (childDescriptors.isNotEmpty()) {
                        childDescriptors[0] = PrimitiveKind.INT.primitiveSerialDescriptor
                    }
                    isCurrentElementNullable = false
                    return 1 // One simulated element
                }
            }
        }
        setCurrentChildType(PrimitiveKind.INT)
    }

    override fun readLongValue(): Long {
        setCurrentChildType(PrimitiveKind.LONG)
    }

    override fun readNotNullMark(): Boolean {
        if (currentChildIndex >= 0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
        return true
    }

    override fun readNullValue(): Nothing? {
        if (currentChildIndex >= 0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
        return null
    }

    override fun readShortValue(): Short {
        setCurrentChildType(PrimitiveKind.SHORT)
    }

    override fun readStringValue(): String {
        /*
         * We need to special case the Polymorphic serializer as it requires the children to be read in order
         * This mean bailing out to continue at the next index is invalid.
         * Check child index first as it will not be 0 if the class desc has not yet been initialised
        */
        if (currentChildIndex == 0 &&
            kSerialClassDesc.kind == KSerialClassKind.POLYMORPHIC) {

            childDescriptors[0] = PrimitiveKind.STRING.primitiveSerialDescriptor
            return "nl.adaptivity.xmlutil.serialization.canary.InputCanary\$Dummy"
        }
        setCurrentChildType(PrimitiveKind.STRING)
    }

    override fun readUnitValue() {
        setCurrentChildType(PrimitiveKind.UNIT)
    }

    override fun readValue(): Any {
        throw SerializationException("Cannot handle non-serializable values")
    }

    internal class SuspendException(val finished: Boolean = false) : Exception()

    // This is used for polymorphic readers.
    @Suppress("unused")
    @Serializable
    class Dummy(val dummyVal: String)
}