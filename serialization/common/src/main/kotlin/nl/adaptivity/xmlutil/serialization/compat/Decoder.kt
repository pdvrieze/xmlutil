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

interface Decoder {
    // returns true if the following value is not null, false if null
    fun decodeNotNullMark(): Boolean
    // consumes null, returns null, is be called when decodeNotNullMark() is false
    fun decodeNull(): Nothing?

    fun decodeUnit()
    fun decodeBoolean(): Boolean
    fun decodeByte(): Byte
    fun decodeShort(): Short
    fun decodeInt(): Int
    fun decodeLong(): Long
    fun decodeFloat(): Float
    fun decodeDouble(): Double
    fun decodeChar(): Char
    fun decodeString(): String

    // Decoder is allowed to override this default implementation
    // todo: Add oldValue to deserialize
    fun <T : Any?> decodeSerializable(strategy: DeserializationStrategy<T>): T =
            strategy.deserialize(this)

    fun beginDecodeComposite(desc: SerialDescriptor): CompositeDecoder
}

interface CompositeDecoder {
    fun endDecodeComposite(desc: SerialDescriptor)

    // decodeElementIndex results
    companion object {
        // end of input
        const val READ_DONE = -1
        // the decoder is sure that elements go in order, do not call decodeElementIndex anymore
        const val READ_ALL = -2
    }

    // returns either index or one of READ_XXX constants
    fun decodeElementIndex(desc: SerialDescriptor): Int

    /**
     * Optional method to specify collection size to pre-allocate memory,
     * called in the beginning of collection reading.
     * If the decoder specifies in-order reading ([READ_ALL] is returned from [decodeElementIndex]), then
     * the correct implementation of this method is mandatory.
     *
     * @return Collection size or -1 if not available.
     */
    fun decodeCollectionSize(desc: SerialDescriptor): Int = -1

    /**
     * This method is called when [decodeElementIndex] returns the index which was
     * already encountered during deserialization of this class.
     *
     * @throws [UpdateNotSupportedException] if this implementation
     *                                       doesn't allow duplication of fields.
     */
    fun decodeElementAgain(desc: SerialDescriptor, index: Int): Unit

    fun decodeUnitElement(desc: SerialDescriptor, index: Int)
    fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean
    fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte
    fun decodeShortElement(desc: SerialDescriptor, index: Int): Short
    fun decodeIntElement(desc: SerialDescriptor, index: Int): Int
    fun decodeLongElement(desc: SerialDescriptor, index: Int): Long
    fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float
    fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double
    fun decodeCharElement(desc: SerialDescriptor, index: Int): Char
    fun decodeStringElement(desc: SerialDescriptor, index: Int): String

    // [wasRead] is analogous to [decodeElementAgain] passed here, so fast decoders
    // won't have to save it in a state variable
    fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, strategy: DeserializationStrategy<T>, oldValue: T?, wasRead: Boolean): T
}