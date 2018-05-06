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

package nl.adaptivity.util.xml

import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import nl.adaptivity.util.multiplatform.Class
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlSerializable
import kotlin.reflect.KClass

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
expect class CompactFragment : ICompactFragment {
    constructor(content: String)
    constructor(orig: ICompactFragment)
    constructor(content: XmlSerializable)
    constructor(namespaces: Iterable<Namespace>, content: CharArray?)
    constructor(namespaces: Iterable<Namespace>, content: String)

    class Factory() : XmlDeserializerFactory<CompactFragment>

    companion object {
        fun deserialize(reader: XmlReader): CompactFragment
    }
}

@Serializer(forClass = CompactFragment::class)
class CompactFragmentSerializer() : KSerializer<CompactFragment> {
    override val serialClassDesc get() = MYSERIALCLASSDESC

    override fun load(input: KInput): CompactFragment {
        val serialClassDesc = serialClassDesc
        val newInput = input.readBegin(serialClassDesc)
        var namespaces: List<Namespace> = mutableListOf()
        var content: String = ""
        var elem = newInput.readElement(serialClassDesc)
        while (elem != KInput.READ_DONE) {
            when (elem) {
                0 -> namespaces = newInput.readSerializableElementValue(serialClassDesc, elem,
                                                                        input.context.klassSerializer(
                                                                            kClass()))
                1 -> content = newInput.readStringElementValue(serialClassDesc, elem)
                KInput.UNKNOWN_NAME
                  -> throw IllegalArgumentException("Unknown child element")
            }
            elem = newInput.readElement(serialClassDesc)
        }
        return CompactFragment(namespaces, content)
    }

    override fun save(output: KOutput, obj: CompactFragment) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



    companion object {
        @JvmStatic
        val MYSERIALCLASSDESC = object : KSerialClassDesc {
            override val kind: KSerialClassKind get() = KSerialClassKind.CLASS

            override val name: String get() = "compactFragment"

            override fun getElementIndex(name: String): Int {
                return when (name) {
                    "namespaces" -> 0
                    "content"    -> 1
                    else         -> KInput.UNKNOWN_NAME
                }
            }

            override fun getElementName(index: Int): String {
                return when (index) {
                    0    -> "namespaces"
                    1    -> "content"
                    else -> throw IndexOutOfBoundsException("$index")
                }
            }

            override val associatedFieldsCount: Int get() = 2
        }
    }
}

internal inline fun <reified T : Any> kClass(): KClass<T> = T::class