/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml.serialization

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringSerializer
import nl.adaptivity.util.CharArraySequence
import nl.adaptivity.util.multiplatform.toCharArray

object CharArrayAsStringSerializer : KSerializer<CharArray> {
    override val serialClassDesc = simpleSerialClassDesc<CharArray>()

    override fun save(output: KOutput, obj: CharArray) = output.writeStringValue(CharArraySequence(obj).toString())

    override fun load(input: KInput): CharArray = input.readStringValue().toCharArray()

    override fun update(input: KInput, old: CharArray) = throw UpdateNotSupportedException("CharArray")
}

@Suppress("UNCHECKED_CAST")
fun KInput.readNullableString(): String? = readNullableSerializableValue(StringSerializer::class as KSerialLoader<String?>)

fun KOutput.writeNullableStringElementValue(desc: KSerialClassDesc, index: Int, value: String?) = writeNullableSerializableElementValue(desc, index, StringSerializer, value)


inline fun KSerializer<*>.readElements(input: KInput, body: (Int) -> Unit) {
    var elem = input.readElement(serialClassDesc)
    while (elem >= 0) {
        body(elem)
        elem = input.readElement(serialClassDesc)
    }
}

inline fun KInput.readElement(desc: KSerialClassDesc, body: KInput.(desc: KSerialClassDesc) -> Unit) {
    val input = readBegin(desc)
    input.body(desc)
    input.readEnd(desc)
}
