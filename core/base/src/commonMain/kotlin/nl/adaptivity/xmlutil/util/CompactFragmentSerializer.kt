/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
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

package nl.adaptivity.xmlutil.util

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.*

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
public object CompactFragmentSerializer : XmlSerializer<CompactFragment> {
    private val namespacesSerializer = ListSerializer(Namespace)

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("nl.adaptivity.xmlutil.util.compactFragment") {
        element("namespaces", namespacesSerializer.descriptor)
        element("content", serialDescriptor<String>())
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: CompactFragment?,
        isValueChild: Boolean
    ): CompactFragment {
        return when {
            isValueChild -> {
                input.siblingsToFragment()
            }

            else -> decoder.decodeStructure(descriptor) {
                input.next()
                input.siblingsToFragment()
            }
        }
    }

    override fun deserialize(decoder: Decoder): CompactFragment {
        return decoder.decodeStructure(descriptor) {
            readCompactFragmentContent(this)
        }
    }

    private fun readCompactFragmentContent(input: CompositeDecoder): CompactFragment {
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

        return CompactFragment(namespaces, content)
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: CompactFragment, isValueChild: Boolean) {
        serializeXMLImpl(encoder, output, value, isValueChild)
    }

    internal fun serializeXMLImpl(encoder: Encoder, output: XmlWriter, value: ICompactFragment, isValueChild: Boolean) {
        when {
            isValueChild -> writeCompactFragmentContent(output, value)

            else ->
                encoder.encodeStructure(descriptor) {
                    writeCompactFragmentContent(output, value)
                }
        }
    }

    override fun serialize(encoder: Encoder, value: CompactFragment) {
        serializeImpl(encoder, value)
    }

    internal fun serializeImpl(
        encoder: Encoder,
        value: ICompactFragment
    ) {
        encoder.encodeStructure(descriptor) {
            this.encodeSerializableElement(
                descriptor, 0,
                namespacesSerializer, value.namespaces.toList()
            )
            this.encodeStringElement(descriptor, 1, value.contentString)
        }
    }

    private fun writeCompactFragmentContent(
        writer: XmlWriter,
        value: ICompactFragment
    ) {
        for (namespace in value.namespaces) {
            if (writer.getPrefix(namespace.namespaceURI) == null) {
                writer.namespaceAttr(namespace)
            }
        }

        value.serialize(writer)
    }


}

