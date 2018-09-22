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
import nl.adaptivity.xmlutil.serialization.canary.Canary

class DecoderInput(val decoder: Decoder) : ElementValueInput() {
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

    override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput = this
}

class CompositeDecoderInput(val decoder: CompositeDecoder, val descriptor: SerialDescriptor) : TaggedInput<Int>() {

    override fun KSerialClassDesc.getTag(index: Int): Int = index

    override fun readEnd(desc: KSerialClassDesc) = decoder.endDecodeComposite(descriptor)

    override fun readElement(desc: KSerialClassDesc): Int {
        return decoder.decodeElementIndex(descriptor)
    }

    override fun <T> updateSerializableValue(loader: KSerialLoader<T>,
                                             desc: KSerialClassDesc,
                                             old: T): T = decoder.run {
        decodeElementAgain(descriptor, currentTag)
        val childDescriptor = descriptor.getElementDescriptor(currentTag)
        decodeSerializableElement(descriptor, currentTag, LoaderStrategy(loader, childDescriptor), old, true)
    }


}

class LoaderStrategy<out T>(val loader: KSerialLoader<@UnsafeVariance T>,
                            override val descriptor: SerialDescriptor) : DeserializationStrategy<T> {

    override fun deserialize(input: Decoder, oldValue: @UnsafeVariance T?): T {
        val decoderInput = when (descriptor.extKind) {
            is PrimitiveKind -> DecoderInput(input)
            else -> CompositeDecoderInput(input.beginDecodeComposite(descriptor), descriptor)
        }


        return when (oldValue) {
            null -> loader.load(decoderInput)
            else -> loader.update(decoderInput, oldValue)
        }
    }

    override fun load(input: KInput): T {
        return loader.load(input)
    }

    override fun update(input: KInput, old: @UnsafeVariance T): T {
        return loader.update(input, old)
    }
}
