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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlDeserializationStrategy
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.localName
import nl.adaptivity.xmlutil.elementContentToFragment
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.*

class StringOrCompositePolymorphism253 {

    val xmlElement = XML(ExtensionDto.elementModule()) {
        recommended_0_90_2()
    }

    val xmlDefaultElement = XML(ExtensionDto.elementDefaultModule()) {
        recommended_0_90_2()
    }

    val xmlDummyElement = XML(ExtensionDto.defaultModule()) {
        recommended_0_90_2()
    }

    val xmlXmlDummyElement = XML(ExtensionDto.xmlDefaultModule()) {
        recommended_0_90_2()
    }

    val xmlRecoverConsume = XML(ExtensionDto.nodefaultModule()) {
        recommended_0_90_2 {
            unknownChildHandler = UnknownChildHandler { input, _, _, _, _ ->
                input.elementContentToFragment() // parse the element and drop it
                emptyList()
            }
        }
    }

    val xmlRecoverBlind = XML(ExtensionDto.nodefaultModule()) {
        recommended_0_90_2 {
            unknownChildHandler = UnknownChildHandler { input, _, _, _, _ ->
                emptyList()
            }
        }
    }

    @Test
    fun testParseRecoverConsume() {
        testParse(xmlRecoverConsume) { assertTrue(it.isEmpty()) }
    }

    @Test
    fun testParseElement() {
        testParse(xmlElement) {
            if (it.any { it is String && it.isBlank() }) {
                fail("Expected parsing to ignore whitespace, but this didn't happen")
            }
            val elem = assertIs<Element>(it.singleOrNull())
            assertEquals("other", elem.localName)
        }
    }

    @Test
    fun testParseDefaultElement() {
        testParse(xmlDefaultElement) {
            val elem = assertIs<Element>(it.singleOrNull())
            assertEquals("other", elem.localName)
        }
    }

    @Test
    fun testParseDummyElement() {
        testParse(xmlDummyElement) {
            val elem = assertIs<Unit>(it.singleOrNull())
        }
    }

    @Test
    fun testParseXmlDummyElement() {
        testParse(xmlXmlDummyElement) {
            val elem = assertIs<CompactFragment>(it.singleOrNull())
            assertEquals("", elem.contentString)
        }
    }

    @Test
    fun testParseRecoverWithoutConsuming() {
        testParse(xmlRecoverBlind) { assertTrue(it.isEmpty()) }
    }

    private fun testParse(xml: XML, assertE2: (List<Any>) -> Unit) {
        if (testTarget == Target.Node) return

        val result = xml.decodeFromString<List<ExtensionDto>>(SAMPLE, QName("Extensions"))
        assertEquals(3, result.size)
        assertEquals("{\"savedData\":\"\"}", assertIs<String>(result[0].value[0]).trim())
        val e2 = result[1].value
        assertE2(e2)

        for (e in result[2].value) {
            if (e is String) assertEquals("", e.trim())
        }

        val e3 = assertIs<AdVerificationsDto>(result[2].value.single())
        assertEquals(1, e3.verifications?.size)
        assertIs<Element>(e3.verifications?.single())
    }


    @Serializable
    @SerialName("Extension")
    data class ExtensionDto(
        val source: String? = null,
        val type: String? = null,
        @XmlValue
        @XmlIgnoreWhitespace
        var value: List<@Polymorphic Any> = listOf()
    ) {
        companion object {
            fun nodefaultModule() = SerializersModule {
                polymorphic(Any::class) {
                    subclass(String::class)
                    subclass(AdVerificationsDto::class)
                }
            }
            fun elementModule() = SerializersModule {
                polymorphic(Any::class) {
                    subclass(Element::class)
                    subclass(String::class)
                    subclass(AdVerificationsDto::class)
                }
            }
            fun elementDefaultModule() = SerializersModule {
                polymorphic(Any::class) {
                    defaultDeserializer { Element.serializer() }
                    subclass(String::class)
                    subclass(AdVerificationsDto::class)
                }
            }
            fun defaultModule() = SerializersModule {
                polymorphic(Any::class) {
                    defaultDeserializer { DummyDeserializer }
                    subclass(String::class)
                    subclass(AdVerificationsDto::class)
                }
            }
            fun xmlDefaultModule() = SerializersModule {
                polymorphic(Any::class) {
                    defaultDeserializer { DummyXmlDeserializer }
                    subclass(String::class)
                    subclass(AdVerificationsDto::class)
                }
            }
        }
    }

    object DummyDeserializer: DeserializationStrategy<Any> {
        override val descriptor: SerialDescriptor get() = buildClassSerialDescriptor("Dummy") {
            element("dummyVal", Unit.serializer().descriptor, isOptional = true)
        }

        override fun deserialize(decoder: Decoder): Any {
            return decoder.decodeStructure(descriptor) {
                var i = decodeElementIndex(descriptor)
                while (i>=0) {
                    decodeSerializableElement(descriptor, i, DummyDeserializer)
                    i = decodeElementIndex(descriptor)
                }
                Unit
            }
        }
    }

    object DummyXmlDeserializer: XmlDeserializationStrategy<Any> {
        override val descriptor: SerialDescriptor get() = buildClassSerialDescriptor("Dummy")

        override fun deserialize(decoder: Decoder): Any {
            throw UnsupportedOperationException()
        }

        override fun deserializeXML(
            decoder: Decoder,
            input: XmlReader,
            previousValue: Any?,
            isValueChild: Boolean
        ): Any {
            return input.elementContentToFragment()
        }
    }

    @Serializable
    @SerialName("AdVerifications")
    data class AdVerificationsDto(
        @XmlElement(true)
        @XmlValue
        var verifications: MutableList<Element>? = mutableListOf()
    )

    val SAMPLE = """
        |<Extensions>
        |    <Extension source="mySource">
        |        <![CDATA[{"savedData":""}]]>
        |    </Extension>
        |    <Extension>
        |        <other />
        |    </Extension>
        |    <Extension type="AdVerifications">
        |        <AdVerifications>
        |            <Verification vendor="Something">
        |                <JavaScriptResource apiFramework="omid" browserOptional="true">
        |                    <![CDATA[https://google.com/video.js]]>
        |                </JavaScriptResource>
        |                <VerificationParameters>
        |                    <![CDATA[{"key":"21649"}]]>
        |                </VerificationParameters>
        |            </Verification>
        |        </AdVerifications>
        |    </Extension>
        |</Extensions>
    """.trimMargin()
}
