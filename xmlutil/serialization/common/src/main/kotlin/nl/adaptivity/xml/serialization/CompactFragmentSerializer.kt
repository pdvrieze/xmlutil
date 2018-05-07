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
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.XmlEvent
import nl.adaptivity.xml.siblingsToFragment
import kotlin.reflect.KClass

@Serializer(forClass = Namespace::class)
class NamespaceSerializer: KSerializer<Namespace> {
    override val serialClassDesc: KSerialClassDesc get() = Companion

    private lateinit var stringSerializer: KSerializer<String>

    override fun load(input: KInput): Namespace {
        if (! this::stringSerializer.isInitialized) stringSerializer = input.context.klassSerializer(String::class)

        lateinit var prefix: String
        lateinit var namespaceUri: String
        readElements(input) {
            when(it) {
                0 -> prefix = input.readStringElementValue(serialClassDesc, it)
                1 -> namespaceUri = input.readStringElementValue(serialClassDesc, it)
            }
        }
        return XmlEvent.NamespaceImpl(prefix, namespaceUri)
    }

    override fun save(output: KOutput, obj: Namespace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object: KSerialClassDesc {
        override val kind: KSerialClassKind
            get() = KSerialClassKind.CLASS

        override val name: String
            get() = "namespace"

        override fun getElementIndex(name: String): Int = when(name) {
            "prefix" -> 0
            "namespaceUri" -> 1
            else -> KInput.UNKNOWN_NAME
        }

        override fun getElementName(index: Int): String = when (index) {
            0 -> "prefix"
            1 -> "namespaceUri"
            else -> throw IndexOutOfBoundsException("$index")
        }

    }
}

inline fun KSerializer<*>.readElements(input: KInput, body: (Int) -> Unit) {
    var elem = input.readElement(serialClassDesc)
    while (elem>=0) {
        body(elem)
        elem = input.readElement(serialClassDesc)
    }
}

@Serializer(forClass = CompactFragment::class)
class CompactFragmentSerializer() : KSerializer<CompactFragment> {
    override val serialClassDesc get() = MYSERIALCLASSDESC

    override fun load(input: KInput): CompactFragment {
        val serialClassDesc = serialClassDesc
        val newInput = input.readBegin(serialClassDesc)
        if (newInput is XML.XmlInput) {

            return newInput.input.run {
                next()
                siblingsToFragment()
            }
        } else {
            var namespaces: List<Namespace> = mutableListOf()
            var content = ""

            readElements(newInput) { elem ->
                when (elem) {
                    0 -> namespaces = newInput.readSerializableElementValue(serialClassDesc, elem,
                                                                            input.context.klassSerializer(kClass()))
                    1 -> content = newInput.readStringElementValue(serialClassDesc, elem)
                }
            }

            return CompactFragment(namespaces, content)
        }
    }

    override fun save(output: KOutput, obj: CompactFragment) {
        val serialClassDesc = serialClassDesc
        output.writeBegin(serialClassDesc).let { output ->
            if (output is XML.XmlOutput) {
                obj.serialize(output.target)
            } else {
                output.writeSerializableElementValue(serialClassDesc, 0, output.context.klassSerializer(Namespace::class).list, obj.namespaces.toList())
                output.writeStringElementValue(serialClassDesc, 1, obj.contentString)
            }
            output.writeEnd(serialClassDesc)
        }
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

            override fun toString(): String {
                return "compactFragment[namespaces, content]"
            }
        }
    }
}

internal inline fun <reified T : Any> kClass(): KClass<T> = T::class
