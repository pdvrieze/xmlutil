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
import kotlinx.serialization.internal.EnumDescriptor

internal class InputCanary(override val isDeep: Boolean = true) : ElementValueDecoder(), CanaryCommon {
    override lateinit var kSerialClassDesc: SerialDescriptor

    override var childDescriptors: Array<SerialDescriptor?> = emptyArray()

    override var currentChildIndex = -1

    override val serialKind: SerialKind get() = kSerialClassDesc.kind

    override var isClassNullable = false
    override var isCurrentElementNullable = false

    var canBeComplete: Boolean = true

    override var isComplete: Boolean = false

    /**
     * Protection against premature invocation of readEnd by the serializer
     */
    var suspending = false

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        suspending = false
        if (currentChildIndex < 0) { // This is called at every load as we restart load every time
            kSerialClassDesc = desc
            childDescriptors = childInfoForClassDesc(desc)
        }
        return this
    }

    fun doSuspend(couldBeFinished: Boolean = false): Nothing {
        val oldSuspending = suspending
        suspending = true
        throw SuspendException((! oldSuspending) && couldBeFinished)
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (canBeComplete) isComplete = true
        doSuspend(true)
    }

    override fun <T : Any> updateNullableSerializableValue(loader: DeserializationStrategy<T?>, old: T?): T? {
        decodeNotNullMark()
        decodeSerializableValue(loader)
    }

    override fun <T> updateSerializableValue(loader: DeserializationStrategy<T>, old: T): T {
        decodeSerializableValue(loader)
    }


    override fun <T> decodeSerializableValue(loader: DeserializationStrategy<T>): Nothing {
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


    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        currentChildIndex++
        return when {
            currentChildIndex < childDescriptors.size -> currentChildIndex

            else                                      -> CompositeDecoder.READ_DONE
        }
    }

    override fun setCurrentChildType(kind: PrimitiveKind): Nothing {
        super.setCurrentChildType(kind)
        doSuspend(childDescriptors.isEmpty())
    }

    override fun decodeBoolean(): Boolean {
        setCurrentChildType(PrimitiveKind.BOOLEAN)
    }

    override fun decodeByte(): Byte {
        setCurrentChildType(PrimitiveKind.BYTE)
    }

    override fun decodeChar(): Char {
        setCurrentChildType(PrimitiveKind.CHAR)
    }

    override fun decodeDouble(): Double {
        setCurrentChildType(PrimitiveKind.DOUBLE)
    }

    override fun decodeEnum(enumDescription: EnumDescriptor): Int {
        setCurrentEnumChildType(enumDescription)
        doSuspend(childDescriptors.isEmpty())
    }

    override fun decodeFloat(): Float {
        setCurrentChildType(PrimitiveKind.FLOAT)
    }

    override fun decodeInt(): Int {
        if (currentChildIndex == 0) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (kSerialClassDesc.kind) {
                StructureKind.LIST,
                StructureKind.MAP -> {
                    if (childDescriptors.isNotEmpty()) {
                        childDescriptors[0] = PrimitiveKind.INT.primitiveSerializer.descriptor
                    }
                    isCurrentElementNullable = false
                    return 1 // One simulated element
                }
            }
        }
        setCurrentChildType(PrimitiveKind.INT)
    }

    override fun decodeLong(): Long {
        setCurrentChildType(PrimitiveKind.LONG)
    }

    override fun decodeNotNullMark(): Boolean {
        if (currentChildIndex >= 0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
        return true
    }

    override fun decodeNull(): Nothing? {
        if (currentChildIndex >= 0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
        return null
    }

    override fun decodeShort(): Short {
        setCurrentChildType(PrimitiveKind.SHORT)
    }

    override fun decodeString(): String {
        /*
         * We need to special case the Polymorphic serializer as it requires the children to be read in order
         * This mean bailing out to continue at the next index is invalid.
         * Check child index first as it will not be 0 if the class desc has not yet been initialised
        */
        if (currentChildIndex == 0 &&
            kSerialClassDesc.kind == UnionKind.POLYMORPHIC) {

            childDescriptors[0] = PrimitiveKind.STRING.primitiveSerializer.descriptor
            return "nl.adaptivity.xmlutil.serialization.canary.InputCanary\$Dummy"
        }
        setCurrentChildType(PrimitiveKind.STRING)
    }

    override fun decodeUnit() {
        setCurrentChildType(PrimitiveKind.UNIT)
    }

    override fun decodeValue(): Any {
        throw SerializationException("Cannot handle non-serializable values")
    }

    internal class SuspendException(val finished: Boolean = false) : Exception()

    // This is used for polymorphic readers.
    @Suppress("unused")
    @Serializable
    class Dummy(val dummyVal: String)
}