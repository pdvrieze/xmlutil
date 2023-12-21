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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.siblingsToFragment
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

public inline fun CompactFragment.Companion.serializer(): KSerializer<CompactFragment> = CompactFragmentSerializer

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
public object CompactFragmentSerializer : AbstractXmlSerializer<CompactFragment>() {
    private val namespacesSerializer = ListSerializer(Namespace)

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("compactFragment") {
        element("namespaces", namespacesSerializer.descriptor)
        element("content", serialDescriptor<String>())
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: CompactFragment?,
        isValueChild: Boolean
    ): CompactFragment {
        return decoder.decodeStructure(descriptor) {
            input.next()
            input.siblingsToFragment()
        }
    }

    override fun deserializeNonXML(decoder: Decoder): CompactFragment {
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
        when {
            isValueChild -> writeCompactFragmentContent(output, value)

            else ->
                encoder.encodeStructure(descriptor) {
                    writeCompactFragmentContent(output, value)
                }
        }
    }

    override fun serializeNonXML(encoder: Encoder, value: CompactFragment) {
        encoder.encodeStructure(descriptor) {
            serializeNonXML(this, value)
        }
    }

    public fun serialize(encoder: Encoder, value: ICompactFragment): Unit = when (encoder) {
        is XML.XmlOutput -> encoder.encodeStructure(descriptor) {
            writeCompactFragmentContent(encoder.target, value)
        }

        else -> encoder.encodeStructure(descriptor) {
            serializeNonXML(this, value)
        }
    }

    private fun serializeNonXML(encoder: CompositeEncoder, value: ICompactFragment) {
        encoder.encodeSerializableElement(
            descriptor, 0,
            namespacesSerializer, value.namespaces.toList()
        )
        encoder.encodeStringElement(descriptor, 1, value.contentString)
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
