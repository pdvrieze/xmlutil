/*
 * Copyright (c) 2021.
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

@file:UseSerializers(QNameSerializer::class)

package nl.adaptivity.xml.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.prefix
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test

class BlendedQNameValuesTest : PlatformXmlTestBase<BlendedQNameValuesTest.Container>(
    Container(
        PseudoList(
            listOf(
                "##any",
                QName("urn:foo", "bar", "baz"),
                QName("urn:example.org/2", "MyValue", "ns1")
            )
        )
    ),
    Container.serializer()
) {
    override val expectedXML: String =
        "<container xmlns=\"urn:example.org\" xmlns:baz=\"urn:foo\" xmlns:ns1=\"urn:example.org/2\">##any baz:bar ns1:MyValue</container>"

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Test
    override fun testDeserializeXml() {
        super.testDeserializeXml()
    }

    @Test
    override fun testSerializeXml() {
        super.testSerializeXml()
    }

    @Serializable
    @XmlSerialName("container", namespace = "urn:example.org", prefix = "")
    data class Container(
        @XmlValue(true)
        val values: PseudoList,
    )

    @Serializable(PseudoList.Serializer::class)
    class PseudoList(val values: List<Any>) {


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as PseudoList

            if (values != other.values) return false

            return true
        }

        override fun hashCode(): Int {
            return values.hashCode()
        }

        object Serializer : KSerializer<PseudoList> {
            override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor("PseudoList", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: PseudoList) {
                val output = encoder as XML.XmlOutput
                val str = value.values.joinToString(" ") { elem ->
                    when (elem) {
                        is QName -> output.ensureNamespace(elem, false).let { "${it.prefix}:${it.localPart}" }
                        else -> elem.toString()
                    }
                }
                encoder.encodeString(str)
            }

            override fun deserialize(decoder: Decoder): PseudoList {
                val input = decoder as XML.XmlInput
                val elems = decoder.decodeString().trim()
                    .splitToSequence(' ')
                    .mapTo(mutableListOf()) {
                        val cIndex = it.indexOf(':')
                        when {
                            cIndex >= 0 -> {
                                val prefix = it.substring(0, cIndex)
                                val localName = it.substring(cIndex + 1)
                                val ns = input.getNamespaceURI(prefix)
                                    ?: throw SerializationException("Could not find namespace for prefix $prefix")
                                QName(ns, localName, prefix)
                            }

                            else -> it
                        }
                    }
                return PseudoList(elems)
            }
        }
    }

}
