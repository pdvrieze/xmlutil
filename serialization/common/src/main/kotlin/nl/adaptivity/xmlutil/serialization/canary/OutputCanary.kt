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
import nl.adaptivity.xmlutil.serialization.compat.*
import kotlin.reflect.KClass


class OutputCanary constructor(
        kSerialClassDesc: KSerialClassDesc? = null,
        override val isDeep: Boolean = true)
    : ElementValueOutput(), CanaryCommon {

    override var currentChildIndex: Int = -1

    override lateinit var kSerialClassDesc: KSerialClassDesc

    override lateinit var serialKind: SerialKind

    init {
        kSerialClassDesc?.let { this.kSerialClassDesc = it }
    }

    // If we are not deep the descriptor is going to be incomplete (only used for nullability)
    override var isComplete: Boolean = isDeep
        private set


    override var childDescriptors: Array<SerialDescriptor?> = emptyArray()

    // We have two nullable flags to make distinguishing easier. This is because composite and non-composite are mixed.
    override var isClassNullable: Boolean = false

    override var isCurrentElementNullable: Boolean = false

    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        if (index < 0)
            throw IndexOutOfBoundsException()
        this.currentChildIndex = index
        return true
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        kSerialClassDesc = desc
        serialKind = desc.kind.asSerialKind()

        childDescriptors = childInfoForClassDesc(desc)
        return this
    }

    override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        if (currentChildIndex < 0) throw IllegalStateException()
        if (currentChildIndex < childDescriptors.size) {
            val poll = Canary.pollDesc(saver)

            if (poll != null) {
                val isNullable = OutputCanary(isDeep = false).let { saver.save(it, value); it.isClassNullable }
                childDescriptors[currentChildIndex] = poll.wrapNullable(isNullable)
            } else if (isDeep) {
                OutputCanary().also {
                    saver.save(it, value)
                    val desc = it.serialDescriptor()
                    if (it.isComplete) {
                        Canary.registerDesc(saver, desc)
                    } else {
                        isComplete = false // If children are not complete we are not
                    }
                    childDescriptors[currentChildIndex] = desc.wrapNullable()
                }
            }
        }
        currentChildIndex = -1
    }

    override fun setCurrentChildType(kind: PrimitiveKind) {
        super.setCurrentChildType(kind)
        currentChildIndex = -1
    }

    override fun writeBooleanValue(value: Boolean) {
        setCurrentChildType(PrimitiveKind.BOOLEAN)
    }

    override fun writeByteValue(value: Byte) {
        setCurrentChildType(PrimitiveKind.BYTE)
    }

    override fun writeCharValue(value: Char) {
        setCurrentChildType(PrimitiveKind.CHAR)
    }

    override fun writeDoubleValue(value: Double) {
        setCurrentChildType(PrimitiveKind.DOUBLE)
    }

    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) {
        setCurrentEnumChildType(enumClass)
    }

    override fun writeFloatValue(value: Float) {
        setCurrentChildType(PrimitiveKind.FLOAT)
    }

    override fun writeIntValue(value: Int) {
        setCurrentChildType(PrimitiveKind.INT)
    }

    override fun writeLongValue(value: Long) {
        setCurrentChildType(PrimitiveKind.LONG)
    }

    override fun writeNonSerializableValue(value: Any) {
        throw SerializationException("Cannot create descriptors for types includin unserializable values")
        /*
        isCurrentElementNullable = false
        currentChildIndex = -1
        */
    }

    override fun writeNotNullMark() {
        if (currentChildIndex >= 0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
    }

    override fun writeNullValue() {
        if (currentChildIndex >= 0) {
            isCurrentElementNullable = true
            if (currentChildIndex < childDescriptors.size) {
                childDescriptors[currentChildIndex] = DUMMYCHILDINFO
                isComplete = false
            }
        } else {
            isClassNullable = true
        }
        currentChildIndex = -1
    }

    override fun writeShortValue(value: Short) {
        setCurrentChildType(PrimitiveKind.SHORT)
    }

    override fun writeStringValue(value: String) {
        setCurrentChildType(PrimitiveKind.STRING)
    }

    override fun writeUnitValue() {
        setCurrentChildType(PrimitiveKind.UNIT)
    }


    companion object {

        object DUMMYSERIALDESC : SerialDescriptor {
            override val name: String get() = throw UnsupportedOperationException("Dummy descriptors have no names")
            @Suppress("OverridingDeprecatedMember")
            override val kind: KSerialClassKind
                get() = throw UnsupportedOperationException("Dummy descriptors have no kind")

            override val extKind: SerialKind
                get() = throw UnsupportedOperationException("Dummy descriptors have no kind")
            override val elementsCount: Int get() = 0

            override fun getElementName(index: Int) = throw UnsupportedOperationException("No children in dummy")

            override fun getElementIndex(name: String) = throw UnsupportedOperationException("No children in dummy")

            override fun getEntityAnnotations(): List<Annotation> = emptyList()

            override fun getElementAnnotations(index: Int) = throw UnsupportedOperationException("No children in dummy")

            override fun getElementDescriptor(index: Int): SerialDescriptor {
                throw UnsupportedOperationException("No children in dummy")
            }

            override val isNullable: Boolean get() = false

            override fun isElementOptional(index: Int) =
                    throw UnsupportedOperationException("No children in dummy")
        }


    }
}


