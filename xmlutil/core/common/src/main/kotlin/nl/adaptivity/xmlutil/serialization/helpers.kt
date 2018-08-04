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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringSerializer
import nl.adaptivity.xmlutil.impl.CharArraySequence
import nl.adaptivity.xmlutil.multiplatform.toCharArray

object CharArrayAsStringSerializer : KSerializer<CharArray> {
    override val serialClassDesc = simpleSerialClassDesc<CharArray>()

    override fun save(output: KOutput, obj: CharArray) = output.writeStringValue(
        CharArraySequence(obj).toString())

    override fun load(input: KInput): CharArray = input.readStringValue().toCharArray()

    override fun update(input: KInput, old: CharArray) = throw UpdateNotSupportedException("CharArray")
}

@Suppress("UNCHECKED_CAST")
fun KInput.readNullableString(): String? = readNullableSerializableValue(StringSerializer as KSerialLoader<String?>)

@Suppress("UNCHECKED_CAST")
fun KInput.readNullableString(desc: KSerialClassDesc, index: Int): String? = readNullableSerializableElementValue(desc, index, StringSerializer as KSerialLoader<String?>)

fun KOutput.writeNullableStringElementValue(desc: KSerialClassDesc, index: Int, value: String?) = writeNullableSerializableElementValue(desc, index, StringSerializer, value)


inline fun KSerializer<*>.readElements(input: KInput, body: (Int) -> Unit) {
    var elem = input.readElement(serialClassDesc)
    while (elem >= 0) {
        body(elem)
        elem = input.readElement(serialClassDesc)
    }
}

inline fun <T> KInput.readBegin(desc: KSerialClassDesc, body: KInput.(desc: KSerialClassDesc) -> T):T {
    val input = readBegin(desc)
    try {
        return input.body(desc)
    } finally {
        input.readEnd(desc)
    }
}

inline fun KOutput.writeBegin(desc: KSerialClassDesc, body: KOutput.(desc: KSerialClassDesc) -> Unit) {
    val output = writeBegin(desc)
    try {
        output.body(desc)
    } finally {
        output.writeEnd(desc)
    }
}