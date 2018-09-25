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
import kotlinx.serialization.internal.NullableSerializer
import nl.adaptivity.xmlutil.serialization.canary.Canary
import kotlin.reflect.KClass

open class DecoderInput(val decoder: Decoder, val serialDescriptor: SerialDescriptor) : ElementValueInput() {
    override fun readNotNullMark(): Boolean = decoder.decodeNotNullMark()
    override fun readNullValue(): Nothing? = decoder.decodeNull()

    override fun readUnitValue() = decoder.decodeUnit()
    override fun readBooleanValue() = decoder.decodeBoolean()
    override fun readByteValue() = decoder.decodeByte()
    override fun readShortValue() = decoder.decodeShort()
    override fun readIntValue() = decoder.decodeInt()
    override fun readLongValue() = decoder.decodeLong()
    override fun readFloatValue() = decoder.decodeFloat()
    override fun readDoubleValue() = decoder.decodeDouble()
    override fun readCharValue() = decoder.decodeChar()
    override fun readStringValue() = decoder.decodeString()

    override fun <T> readSerializableValue(loader: KSerialLoader<T>): T { // This is called first, it gives the loader and type
        return decoder.decodeSerializable(LoaderStrategy(loader, Canary.serialDescriptor(loader)))
    }

    override fun <T> updateSerializableValue(loader: KSerialLoader<T>, desc: KSerialClassDesc, old: T): T {
        return super.updateSerializableValue(loader, desc, old)
    }

    override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
        // Upgrade to a composite object. We don't need to distinguish as this is called by the serializer
        return CompositeDecoderInput(decoder.beginDecodeComposite(serialDescriptor), serialDescriptor)
    }
}

class CompositeDecoderInput(val decoder: CompositeDecoder, val descriptor: SerialDescriptor) : TaggedInput<Int>() {

    var prevWasNotNullMark: Boolean = false

    override fun KSerialClassDesc.getTag(index: Int): Int = index

    override fun readEnd(desc: KSerialClassDesc) = decoder.endDecodeComposite(descriptor)

    override fun readElement(desc: KSerialClassDesc): Int {
        return decoder.decodeElementIndex(descriptor)
    }

    override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
        return super.readBegin(desc, *typeParams)
    }

    override fun <T> readSerializableValue(loader: KSerialLoader<T>): T {
        val childDescriptor = when (descriptor.extKind) {
            StructureKind.LIST,
            StructureKind.MAP -> descriptor.getElementDescriptor(1) // 0 is index, an Int 1 is the content
            UnionKind.POLYMORPHIC,
            UnionKind.SEALED  -> Canary.serialDescriptor(loader)
            else              -> descriptor.getElementDescriptor(currentTag)
        }
        return decoder.decodeSerializableElement(
                descriptor,
                currentTag,
                LoaderStrategy(loader, childDescriptor),
                null,
                false
                                                )
    }

    override fun <T : Any> updateNullableSerializableValue(
            loader: KSerialLoader<T?>,
            desc: KSerialClassDesc,
            old: T?
                                                          ): T? = decoder.run {
        @Suppress("UNCHECKED_CAST")
        val effectiveLoader = NullableSerializer<T>((loader as KSerialLoader<T>).asSerializer())
        return decodeSerializableElement(
                descriptor, currentTag, LoaderStrategy(effectiveLoader, descriptor.getElementDescriptor(currentTag)),
                old, false
                                        )
    }

    override fun <T> updateSerializableValue(
            loader: KSerialLoader<T>,
            desc: KSerialClassDesc,
            old: T
                                            ): T = decoder.run {
        decodeElementAgain(descriptor, currentTag)
        val childDescriptor = descriptor.getElementDescriptor(currentTag)
        decodeSerializableElement(descriptor, currentTag, LoaderStrategy(loader, childDescriptor), old, true)
    }

    override fun readTaggedBoolean(tag: Int): Boolean {
        return decoder.decodeBooleanElement(descriptor, tag)
    }

    override fun readTaggedByte(tag: Int): Byte {
        return decoder.decodeByteElement(descriptor, tag)
    }

    override fun readTaggedChar(tag: Int): Char {
        return decoder.decodeCharElement(descriptor, tag)
    }

    override fun readTaggedDouble(tag: Int): Double {
        return decoder.decodeDoubleElement(descriptor, tag)
    }

    override fun <E : Enum<E>> readTaggedEnum(tag: Int, enumClass: KClass<E>): E {
        val strategy = LoaderStrategy(EnumSerializer(enumClass), descriptor.getElementDescriptor(currentTag))
        return decoder.decodeSerializableElement(descriptor, tag, strategy, null, false)
    }

    override fun readTaggedFloat(tag: Int): Float {
        return decoder.decodeFloatElement(descriptor, tag)
    }

    override fun readTaggedInt(tag: Int): Int {
        return decoder.decodeIntElement(descriptor, tag)
    }

    override fun readTaggedLong(tag: Int): Long {
        return decoder.decodeLongElement(descriptor, tag)
    }

    override fun readTaggedNotNullMark(tag: Int): Boolean {
        prevWasNotNullMark = true
        val nullMarkStrategy = NullMarkStrategy(descriptor.getElementDescriptor(tag))
        return decoder.decodeSerializableElement(descriptor, tag, nullMarkStrategy, null, false)
    }

    override fun readTaggedNull(tag: Int): Nothing? {
        val nullStrategy = NullStrategy(descriptor.getElementDescriptor(tag))
        return decoder.decodeSerializableElement(descriptor, tag, nullStrategy, null, false)
    }

    override fun readTaggedShort(tag: Int): Short {
        return decoder.decodeShortElement(descriptor, tag)
    }

    override fun readTaggedString(tag: Int): String {
        return decoder.decodeStringElement(descriptor, tag)
    }

    override fun readTaggedUnit(tag: Int) {
        decoder.decodeUnitElement(descriptor, tag)
    }

}

class LoaderStrategy<out T>(
        val loader: KSerialLoader<@UnsafeVariance T>,
        override val descriptor: SerialDescriptor
                           ) : DeserializationStrategy<T> {

    override fun deserialize(input: Decoder, oldValue: @UnsafeVariance T?): T {
        val decoderInput = DecoderInput(input, descriptor)

        return when (oldValue) {
            null -> loader.load(decoderInput)
            else -> loader.update(decoderInput, oldValue)
        }
    }
}

private class NullableInput(decoder: Decoder,
                            descriptor: SerialDescriptor): DecoderInput(decoder, descriptor) {
    override fun readNotNullMark() = decoder.decodeNotNullMark()
}

private class NullMarkStrategy(override val descriptor: SerialDescriptor) : DeserializationStrategy<Boolean> {

    override fun deserialize(input: Decoder, oldValue: Boolean?): Boolean {
        return input.decodeNotNullMark()
    }

}

private class NullStrategy(override val descriptor: SerialDescriptor) : DeserializationStrategy<Nothing?> {

    override fun deserialize(input: Decoder, oldValue: Nothing?): Nothing? {
        return input.decodeNull()
    }

}

fun <T> KSerialLoader<T>.asSerializer(): KSerializer<T> = this as? KSerializer<T> ?: object : KSerializer<T> {

    override val serialClassDesc: KSerialClassDesc
        get() = throw UnsupportedOperationException("This is not actually a serializer")

    override fun load(input: KInput): T {
        return this@asSerializer.load(input)
    }

    override fun save(output: KOutput, obj: T) {
        throw UnsupportedOperationException("This is not actually a serializer")
    }
}