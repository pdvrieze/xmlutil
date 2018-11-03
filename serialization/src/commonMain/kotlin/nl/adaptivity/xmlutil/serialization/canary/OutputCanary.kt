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
import kotlin.reflect.KClass


class OutputCanary constructor(
        kSerialClassDesc: KSerialClassDesc? = null,
        override val isDeep: Boolean = true)
    : ElementValueEncoder(), CanaryCommon {

    override var currentChildIndex: Int = -1

    override lateinit var kSerialClassDesc: KSerialClassDesc

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

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        if (index < 0)
            throw IndexOutOfBoundsException()
        this.currentChildIndex = index
        return true
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        kSerialClassDesc = desc

        childDescriptors = childInfoForClassDesc(desc)
        return this
    }

    override fun <T> encodeSerializableValue(saver: SerializationStrategy<T>, value: T) {
        if (currentChildIndex < 0) throw AssertionError("This should not happen")
        if (currentChildIndex < childDescriptors.size) {
            val poll = Canary.pollDesc(saver)

            if (poll != null) {
                val isNullable = OutputCanary(isDeep = false).let { saver.serialize(it, value); it.isClassNullable }
                childDescriptors[currentChildIndex] = poll.wrapNullable(isNullable)
            } else if (isDeep) {
                OutputCanary().also {
                    saver.serialize(it, value)
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

    override fun encodeBoolean(value: Boolean) {
        setCurrentChildType(PrimitiveKind.BOOLEAN)
    }

    override fun encodeByte(value: Byte) {
        setCurrentChildType(PrimitiveKind.BYTE)
    }

    override fun encodeChar(value: Char) {
        setCurrentChildType(PrimitiveKind.CHAR)
    }

    override fun encodeDouble(value: Double) {
        setCurrentChildType(PrimitiveKind.DOUBLE)
    }

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
        setCurrentEnumChildType(enumDescription)
    }

    override fun encodeFloat(value: Float) {
        setCurrentChildType(PrimitiveKind.FLOAT)
    }

    override fun encodeInt(value: Int) {
        setCurrentChildType(PrimitiveKind.INT)
    }

    override fun encodeLong(value: Long) {
        setCurrentChildType(PrimitiveKind.LONG)
    }

    override fun encodeNotNullMark() {
        if (currentChildIndex >= 0) {
            isCurrentElementNullable = true
        } else {
            isClassNullable = true
        }
    }

    override fun encodeNull() {
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

    override fun encodeShort(value: Short) {
        setCurrentChildType(PrimitiveKind.SHORT)
    }

    override fun encodeString(value: String) {
        setCurrentChildType(PrimitiveKind.STRING)
    }

    override fun encodeUnit() {
        setCurrentChildType(PrimitiveKind.UNIT)
    }


    companion object {

        object DUMMYSERIALDESC : SerialDescriptor {
            override val name: String get() = throw AssertionError("Dummy descriptors have no names")
            @Suppress("OverridingDeprecatedMember")
            override val kind: KSerialClassKind
                get() = throw AssertionError("Dummy descriptors have no kind")

            override val elementsCount: Int get() = 0

            override fun getElementName(index: Int) = throw AssertionError("No children in dummy")

            override fun getElementIndex(name: String) = throw AssertionError("No children in dummy")

            override fun getEntityAnnotations(): List<Annotation> = emptyList()

            override fun getElementAnnotations(index: Int) = throw AssertionError("No children in dummy")

            override fun getElementDescriptor(index: Int): SerialDescriptor {
                throw AssertionError("No children in dummy")
            }

            override val isNullable: Boolean get() = false

            override fun isElementOptional(index: Int) =
                    throw AssertionError("No children in dummy")
        }


    }
}


