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

package nl.adaptivity.xmlutil.serialization.compat

import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumSerializer
import nl.adaptivity.xmlutil.serialization.canary.Canary
import nl.adaptivity.xmlutil.serialization.canary.NullableSerialDescriptor
import kotlin.reflect.KClass

open class EncoderOutput(val encoder: Encoder, val descriptor: SerialDescriptor): ElementValueOutput() {
    override fun writeNotNullMark() = encoder.encodeNotNullMark()
    override fun writeNullValue() = encoder.encodeNull()

    override fun writeUnitValue() = encoder.encodeUnit()
    override fun writeBooleanValue(value: Boolean) = encoder.encodeBoolean(value)
    override fun writeByteValue(value: Byte) = encoder.encodeByte(value)
    override fun writeShortValue(value: Short) = encoder.encodeShort(value)
    override fun writeIntValue(value: Int) = encoder.encodeInt(value)
    override fun writeLongValue(value: Long) = encoder.encodeLong(value)
    override fun writeFloatValue(value: Float) = encoder.encodeFloat(value)
    override fun writeDoubleValue(value: Double) = encoder.encodeDouble(value)
    override fun writeCharValue(value: Char) = encoder.encodeChar(value)
    override fun writeStringValue(value: String) = encoder.encodeString(value)

    override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        encoder.encodeSerializable(SaverStrategy(saver, Canary.serialDescriptor(saver, value)), value)
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        val descriptor = descriptor.let { if (it is NullableSerialDescriptor) it.original else it }
        return CompositeEncoderOutput(beginEncodeComposite(descriptor), descriptor)
    }

    override fun writeBegin(desc: KSerialClassDesc, collectionSize: Int, vararg typeParams: KSerializer<*>): KOutput {
        val descriptor = descriptor.let { if (it is NullableSerialDescriptor) it.original else it }
        return CompositeEncoderOutput(beginEncodeComposite(descriptor), descriptor)
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        throw UnsupportedOperationException("Ending a non-composite encoder should not happen")
    }

    override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) {
        EnumSerializer(enumClass).save(this, value)
    }

    private fun beginEncodeComposite(descriptor: SerialDescriptor): CompositeEncoder {
        return encoder.beginEncodeComposite(descriptor)
    }
}

open class CompositeEncoderOutput(val encoder: CompositeEncoder, val descriptor: SerialDescriptor): TaggedOutput<Int>() {
    override fun KSerialClassDesc.getTag(index: Int) = index

    override fun writeFinished(desc: KSerialClassDesc) = encoder.endEncodeComposite(descriptor)

    override fun writeTaggedUnit(tag: Int) = encoder.encodeUnitElement(descriptor, tag)

    override fun writeTaggedBoolean(tag: Int, value: Boolean) {
        encoder.encodeBooleanElement(descriptor, tag, value)
    }

    override fun writeTaggedByte(tag: Int, value: Byte) {
        encoder.encodeByteElement(descriptor, tag, value)
    }

    override fun writeTaggedShort(tag: Int, value: Short) {
        encoder.encodeShortElement(descriptor, tag, value)
    }

    override fun writeTaggedInt(tag: Int, value: Int) {
        encoder.encodeIntElement(descriptor, tag, value)
    }

    override fun writeTaggedLong(tag: Int, value: Long) {
        encoder.encodeLongElement(descriptor, tag, value)
    }

    override fun writeTaggedFloat(tag: Int, value: Float) {
        encoder.encodeFloatElement(descriptor, tag, value)
    }

    override fun writeTaggedDouble(tag: Int, value: Double) {
        encoder.encodeDoubleElement(descriptor, tag, value)
    }

    override fun writeTaggedChar(tag: Int, value: Char) {
        encoder.encodeCharElement(descriptor, tag, value)
    }

    override fun writeTaggedString(tag: Int, value: String) {
        encoder.encodeStringElement(descriptor, tag, value)
    }

    override fun writeTaggedNotNullMark(tag: Int) {
        super.writeTaggedNotNullMark(tag)
    }

    override fun writeTaggedNull(tag: Int) {
        encoder.encodeNullElement(descriptor, tag)
    }

    override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        val childDescriptor = Canary.serialDescriptor(saver, value)
        encoder.encodeSerializableElement(descriptor, currentTag, SaverStrategy(saver, childDescriptor), value)
    }
}

private class SaverStrategy<T>(private val saver: KSerialSaver<T>, override val descriptor: SerialDescriptor): SerializationStrategy<T> {
    override fun serialize(output: Encoder, obj: T) {
        val encoderOutput = when(descriptor.extKind) {
            is PrimitiveKind -> EncoderOutput(output, descriptor)
            else -> CompositeEncoderOutput(output.beginEncodeComposite(descriptor), descriptor)
        }
        saver.save(encoderOutput, obj)
    }
/*

    override fun save(output: KOutput, obj: T) {
        saver.save(output, obj)
    }
*/
}