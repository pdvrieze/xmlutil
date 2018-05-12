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
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.ICompactFragment
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.siblingsToFragment
import kotlin.reflect.KClass

@Serializer(forClass = CompactFragment::class)
object CompactFragmentSerializer : KSerializer<CompactFragment> {
    override val serialClassDesc get() = MYSERIALCLASSDESC

    override fun load(input: KInput): CompactFragment {
        return input.readBegin(serialClassDesc) { desc ->
            readCompactFragmentContent(this, desc)
        }
    }

    fun readCompactFragmentContent(input: KInput, desc: KSerialClassDesc): CompactFragment {
        return if (input is XML.XmlInput) {

            input.input.run {
                next()
                siblingsToFragment()
            }
        } else {
            var namespaces: List<Namespace> = mutableListOf()
            var content = ""

            val nsIndex = desc.getElementIndex("namespaces")
            val contentIndex = desc.getElementIndex("content")

            readElements(input) { elem ->
                when (elem) {
                    nsIndex      -> namespaces = input.readSerializableElementValue(desc, elem, Namespace.list)
                    contentIndex -> content = input.readStringElementValue(desc, elem)
                }
            }
            CompactFragment(namespaces, content)
        }
    }

    override fun save(output: KOutput, obj: CompactFragment) {
        save(output, obj as ICompactFragment)
    }

    fun save(output: KOutput, obj: ICompactFragment) {
        val serialClassDesc = serialClassDesc
        output.writeBegin(serialClassDesc) { desc ->
            writeCompactFragmentContent(this, desc, 0, obj)
        }
    }

    fun writeCompactFragmentContent(output: KOutput,
                                    serialClassDesc: KSerialClassDesc,
                                    startIndex: Int,
                                    obj: ICompactFragment) {
        if (output is XML.XmlOutput) {
            val writer = output.target
            for (namespace in obj.namespaces) {
                if (writer.getPrefix(namespace.namespaceURI) == null) {
                    writer.namespaceAttr(namespace)
                }
            }

            obj.serialize(writer)
        } else {
            output.writeSerializableElementValue(serialClassDesc, startIndex + 0, Namespace.list,
                                                 obj.namespaces.toList())
            output.writeStringElementValue(serialClassDesc, startIndex + 1, obj.contentString)
        }
    }


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

        override fun toString(): String {
            return "compactFragment[namespaces, content]"
        }
    }
}

@Serializer(forClass = ICompactFragment::class)
object ICompactFragmentSerializer : KSerializer<ICompactFragment> {

    override val serialClassDesc: KSerialClassDesc
        get() = CompactFragmentSerializer.serialClassDesc

    override fun load(input: KInput): ICompactFragment {
        return CompactFragmentSerializer.load(input)
    }

    override fun save(output: KOutput, obj: ICompactFragment) {
        CompactFragmentSerializer.save(output, obj)
    }
}

internal inline fun <reified T : Any> kClass(): KClass<T> = T::class
