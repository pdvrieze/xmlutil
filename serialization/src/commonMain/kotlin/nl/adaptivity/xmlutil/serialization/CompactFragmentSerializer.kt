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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.siblingsToFragment
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

@Suppress("NOTHING_TO_INLINE")
public inline fun CompactFragment.Companion.serializer(): KSerializer<CompactFragment> = CompactFragmentSerializer

@OptIn(WillBePrivate::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializer(forClass = CompactFragment::class)
public object CompactFragmentSerializer : KSerializer<CompactFragment> {
    private val namespacesSerializer = ListSerializer(Namespace)

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("compactFragment") {
            element("namespaces", namespacesSerializer.descriptor)
            element("content", serialDescriptor<String>())
        }

    override fun deserialize(decoder: Decoder): CompactFragment {
        return decoder.decodeStructure(descriptor) {
            readCompactFragmentContent(this)
        }
    }

    private fun readCompactFragmentContent(input: CompositeDecoder): CompactFragment {
        return if (input is XML.XmlInput) {

            input.input.run {
                next()
                siblingsToFragment()
            }
        } else {
            var namespaces: List<Namespace> = mutableListOf()
            var content = ""

            var index = input.decodeElementIndex(descriptor)

            while (index >= 0) {
                when (index) {
                    0 -> namespaces = input.decodeSerializableElement(descriptor, index, namespacesSerializer)
                    1 -> content = input.decodeStringElement(descriptor, index)
                }
                index = input.decodeElementIndex(descriptor)
            }
            CompactFragment(namespaces, content)
        }
    }

    override fun serialize(encoder: Encoder, value: CompactFragment) {
        serialize(encoder, value as ICompactFragment)
    }

    public fun serialize(output: Encoder, value: ICompactFragment) {
        output.encodeStructure(descriptor) {
            writeCompactFragmentContent(this, descriptor, value)
        }
    }

    private fun writeCompactFragmentContent(
        encoder: CompositeEncoder,
        descriptor: SerialDescriptor,
        value: ICompactFragment
    ) {

        val xmlOutput = encoder as? XML.XmlOutput

        if (xmlOutput != null) {
            val writer = xmlOutput.target
            for (namespace in value.namespaces) {
                if (writer.getPrefix(namespace.namespaceURI) == null) {
                    writer.namespaceAttr(namespace)
                }
            }

            value.serialize(writer)
        } else {
            encoder.encodeSerializableElement(
                descriptor, 0,
                namespacesSerializer, value.namespaces.toList()
            )
            encoder.encodeStringElement(descriptor, 1, value.contentString)
        }
    }


}

public object ICompactFragmentSerializer : KSerializer<ICompactFragment> {

    override val descriptor: SerialDescriptor
        get() = CompactFragmentSerializer.descriptor

    override fun deserialize(decoder: Decoder): ICompactFragment {
        return CompactFragmentSerializer.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: ICompactFragment) {
        CompactFragmentSerializer.serialize(encoder, value)
    }
}

/**
 * Helper function that helps decoding structure elements
 */
private inline fun DeserializationStrategy<*>.decodeElements(input: CompositeDecoder, body: (Int) -> Unit) {
    var index = input.decodeElementIndex(descriptor)
    @Suppress("DEPRECATION")
    if (index == CompositeDecoder.DECODE_DONE) return

    while (index >= 0) {
        body(index)
        index = input.decodeElementIndex(descriptor)
    }
}
