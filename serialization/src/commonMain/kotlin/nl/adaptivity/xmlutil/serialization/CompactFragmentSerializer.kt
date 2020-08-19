/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.*
import nl.adaptivity.serialutil.decodeElements
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.siblingsToFragment
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

@Suppress("NOTHING_TO_INLINE")
inline fun CompactFragment.Companion.serializer() = CompactFragmentSerializer

@Serializer(forClass = CompactFragment::class)
@OptIn(WillBePrivate::class)
object CompactFragmentSerializer : KSerializer<CompactFragment> {
    override val descriptor get() = MYSERIALCLASSDESC

    override fun deserialize(decoder: Decoder): CompactFragment {
        return decoder.decodeStructure(descriptor) {
            readCompactFragmentContent(this, descriptor)
        }
    }

    fun readCompactFragmentContent(input: CompositeDecoder, desc: SerialDescriptor): CompactFragment {
        val xmlInput = input as? XML.XmlInput
        return if (xmlInput != null) {

            xmlInput.input.run {
                next()
                siblingsToFragment()
            }
        } else {
            var namespaces: List<Namespace> = mutableListOf()
            var content = ""

            val nsIndex = desc.getElementIndex("namespaces")
            val contentIndex = desc.getElementIndex("content")

            decodeElements(input) { elem: Int ->
                when (elem) {
                    nsIndex      -> namespaces = input.decodeSerializableElement(desc, elem, ListSerializer(Namespace))
                    contentIndex -> content = input.decodeStringElement(desc, elem)
                }
            }
            CompactFragment(namespaces, content)
        }
    }

    override fun serialize(encoder: Encoder, value: CompactFragment) {
        serialize(encoder, value as ICompactFragment)
    }

    fun serialize(output: Encoder, obj: ICompactFragment) {
        val descriptor = descriptor
        output.encodeStructure(descriptor) {
            writeCompactFragmentContent(this, descriptor, 0, obj)
        }
    }

    @WillBePrivate
    fun writeCompactFragmentContent(
        output: CompositeEncoder,
        serialClassDesc: SerialDescriptor,
        startIndex: Int,
        obj: ICompactFragment
                                   ) {
        val xmlOutput = output as? XML.XmlOutput

        if (xmlOutput != null) {
            val writer = xmlOutput.target
            for (namespace in obj.namespaces) {
                if (writer.getPrefix(namespace.namespaceURI) == null) {
                    writer.namespaceAttr(namespace)
                }
            }

            obj.serialize(writer)
        } else {
            output.encodeSerializableElement(serialClassDesc, startIndex + 0,
                                             ListSerializer(Namespace), obj.namespaces.toList())
            output.encodeStringElement(serialClassDesc, startIndex + 1, obj.contentString)
        }
    }


    @JvmStatic
    val MYSERIALCLASSDESC = object : SerialDescriptor {
        override val kind: SerialKind get() = StructureKind.CLASS

        override val serialName: String get() = "compactFragment"

        override fun getElementIndex(name: String): Int {
            return when (name) {
                "namespaces" -> 0
                "content"    -> 1
                else         -> CompositeDecoder.UNKNOWN_NAME
            }
        }

        override fun getElementName(index: Int): String {
            return when (index) {
                0    -> "namespaces"
                1    -> "content"
                else -> throw IndexOutOfBoundsException("$index")
            }
        }

        override fun getElementAnnotations(index: Int): List<Annotation> = emptyList()

        override fun getElementDescriptor(index: Int): SerialDescriptor {
            return when (index) {
                0    -> ListSerializer(Namespace).descriptor
                1    -> String.serializer().descriptor
                else -> throw IndexOutOfBoundsException("$index")
            }
        }

        override fun isElementOptional(index: Int): Boolean = true // Both properties work if they are left out

        override val elementsCount: Int get() = 2

        override fun toString(): String {
            return "compactFragment[namespaces, content]"
        }
    }
}

@Serializer(forClass = ICompactFragment::class)
object ICompactFragmentSerializer : KSerializer<ICompactFragment> {

    override val descriptor: SerialDescriptor
        get() = CompactFragmentSerializer.descriptor

    override fun deserialize(decoder: Decoder): ICompactFragment {
        return CompactFragmentSerializer.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: ICompactFragment) {
        CompactFragmentSerializer.serialize(encoder, value)
    }
}

internal inline fun <reified T : Any> kClass(): KClass<T> = T::class
