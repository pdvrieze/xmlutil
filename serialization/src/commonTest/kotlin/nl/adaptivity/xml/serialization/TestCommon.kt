/*
 * Copyright (c) 2019.
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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.*

private fun String.normalize() = replace(" />", "/>").replace("\r\n","\n")

@OptIn(UnstableDefault::class)
val testConfiguration = JsonConfiguration(
    isLenient = true,
    serializeSpecialFloatingPointValues = true
                                         )

class TestCommon {

    abstract class TestBase<T>(
        val value: T,
        val serializer: KSerializer<T>,
        val serialModule: SerialModule = EmptyModule,
        protected val baseXmlFormat: XML = XML(serialModule),
        private val baseJsonFormat: Json = Json(testConfiguration, serialModule)
                              ) {
        abstract val expectedXML: String
        abstract val expectedJson: String

        fun serializeXml(): String = baseXmlFormat.stringify(serializer, value).normalize()

        fun serializeJson(): String = baseJsonFormat.stringify(serializer, value)

        @Test
        open fun testSerializeXml() {
            assertEquals(expectedXML, serializeXml())
        }

        @Test
        open fun testDeserializeXml() {
            assertEquals(value, baseXmlFormat.parse(serializer, expectedXML))
        }

        @Test
        open fun testSerializeJson() {
            assertEquals(expectedJson, serializeJson())
        }

        @Test
        open fun testDeserializeJson() {
            assertEquals(value, baseJsonFormat.parse(serializer, expectedJson))
        }

    }

    abstract class TestPolymorphicBase<T>(
        value: T,
        serializer: KSerializer<T>,
        serialModule: SerialModule,
        baseJsonFormat: Json = Json(testConfiguration, serialModule)
                                         ) :
        TestBase<T>(value, serializer, serialModule, XML(serialModule) { autoPolymorphic = true }, baseJsonFormat) {

        abstract val expectedNonAutoPolymorphicXML: String

        @Test
        fun nonAutoPolymorphic_serialization_should_work() {
            val serialized =
                XML(context = serialModule) { autoPolymorphic = false }.stringify(serializer, value).normalize()
            assertEquals(expectedNonAutoPolymorphicXML, serialized)
        }

        @Test
        fun nonAutoPolymorphic_deserialization_should_work() {
            val actualValue = XML(context = serialModule) { autoPolymorphic = false }
                .parse(serializer, expectedNonAutoPolymorphicXML)

            assertEquals(value, actualValue)
        }

    }

    class SimpleDataTest : TestBase<Address>(
        Address("10", "Downing Street", "London"),
        Address.serializer()
                                            ) {
        override val expectedXML: String =
            "<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>"

        override val expectedJson: String =
            "{\"houseNumber\":\"10\",\"street\":\"Downing Street\",\"city\":\"London\",\"status\":\"VALID\"}"

        val unknownValues
            get() =
                "<address xml:lang=\"en\" houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>"

        @Test
        fun deserialize_with_unused_attributes() {
            val e = assertFailsWith<UnknownXmlFieldException> {
                XML.parse(serializer, unknownValues)
            }

            val expectedMsgStart = "Could not find a field for name {http://www.w3.org/XML/1998/namespace}lang\n" +
                    "  candidates: houseNumber, street, city, status at position "
            val msgSubstring = e.message?.let { it.substring(0, minOf(it.length, expectedMsgStart.length)) }

            assertEquals(expectedMsgStart, msgSubstring)
        }


        @Test
        fun deserialize_with_unused_attributes_and_custom_handler() {
            var ignoredName: QName? = null
            var ignoredIsAttribute: Boolean? = null
            val xml = XML {
                unknownChildHandler = { _, isAttribute, name, _ ->
                    ignoredName = name
                    ignoredIsAttribute = isAttribute
                }
            }
            assertEquals(value, xml.parse(serializer, unknownValues))
            assertEquals(QName(XMLConstants.XML_NS_URI, "lang", "xml"), ignoredName)
            assertEquals(true, ignoredIsAttribute)
        }

    }

    class ValueContainerTest : TestBase<ValueContainer>(
        ValueContainer("foobar"),
        ValueContainer.serializer()
                                                       ) {
        override val expectedXML: String = "<valueContainer>foobar</valueContainer>"
        override val expectedJson: String = "{\"content\":\"foobar\"}"

        @Test
        fun testAlternativeXml() {
            val alternativeXml = "<valueContainer><![CDATA[foo]]>bar</valueContainer>"
            assertEquals(value, baseXmlFormat.parse(serializer, alternativeXml))
        }

    }

    class ValueContainerTestWithSpaces : TestBase<ValueContainer>(
        ValueContainer("    \nfoobar\n  "),
        ValueContainer.serializer()
                                                       ) {
        override val expectedXML: String = "<valueContainer>    \nfoobar\n  </valueContainer>"
        override val expectedJson: String = "{\"content\":\"    \\nfoobar\\n  \"}"

        @Test
        fun testAlternativeXml() {
            val alternativeXml = "<valueContainer>    \n<![CDATA[foo]]>bar\n  </valueContainer>"
            assertEquals(value, baseXmlFormat.parse(serializer, alternativeXml))
        }

    }

    @UnstableDefault
    class MixedValueContainerTest : TestPolymorphicBase<MixedValueContainer>(
        MixedValueContainer(listOf("foo", Address("10", "Downing Street", "London"), "bar")),
        MixedValueContainer.serializer(),
        MixedValueContainer.module(),
        baseJsonFormat = Json(JsonConfiguration(useArrayPolymorphism = true), context = MixedValueContainer.module())
                                                                            ) {
        override val expectedXML: String =
            "<MixedValueContainer>foo<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>bar</MixedValueContainer>"
        override val expectedNonAutoPolymorphicXML: String =
            "<MixedValueContainer><data type=\"kotlin.String\"><value>foo</value></data><data type=\".Address\"><value houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/></data><data type=\"kotlin.String\"><value>bar</value></data></MixedValueContainer>"
        override val expectedJson: String =
            "{\"data\":[[\"kotlin.String\",\"foo\"],[\"nl.adaptivity.xml.serialization.Address\",{\"houseNumber\":\"10\",\"street\":\"Downing Street\",\"city\":\"London\",\"status\":\"VALID\"}],[\"kotlin.String\",\"bar\"]]}"

    }

    class InvalidValueContainerTest {
        val format = XML()
        val data = InvalidValueContainer("foobar", Address("10", "Downing Street", "London"))
        val serializer = InvalidValueContainer.serializer()
        val invalidXML1: String =
            "<InvalidValueContainer><address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>foobar</InvalidValueContainer>"
        val invalidXML2: String =
            "<InvalidValueContainer>foobar<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/></InvalidValueContainer>"
        val invalidXML3: String =
            "<InvalidValueContainer>foo<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>bar</InvalidValueContainer>"

        @Test
        fun testSerializeInvalid() {
            val e = assertFails {
                format.stringify(serializer, data)
            }
            assertTrue(e is XmlSerialException)
            assertTrue(e.message?.contains("@XmlValue") == true)
        }

        @Test
        fun testDeserializeInvalid1() {
            val e = assertFails {
                format.parse(serializer, invalidXML1)
            }
            assertTrue(e is XmlSerialException)
            assertTrue(e.message?.contains("@XmlValue") == true)
        }

        @Test
        fun testDeserializeInvalid2() {
            val e = assertFails {
                format.parse(serializer, invalidXML2)
            }
            assertTrue(e is XmlSerialException)
            assertTrue(e.message?.contains("@XmlValue") == true)
        }

        @Test
        fun testDeserializeInvalid3() {
            val e = assertFails {
                format.parse(serializer, invalidXML3)
            }
            assertTrue(e is XmlSerialException)
            assertTrue(e.message?.contains("@XmlValue") == true)
        }

    }

    class OptionalBooleanTest : TestBase<Location>(
        Location(Address("1600", "Pensylvania Avenue", "Washington DC")),
        Location.serializer()
                                                  ) {
        override val expectedXML: String =
            "<Location><address houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\" status=\"VALID\"/></Location>"
        override val expectedJson: String =
            "{\"addres\":{\"houseNumber\":\"1600\",\"street\":\"Pensylvania Avenue\",\"city\":\"Washington DC\",\"status\":\"VALID\"},\"temperature\":NaN}"

        val noisyXml
            get() =
                "<Location><unexpected><address>Foo</address></unexpected><address houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\" status=\"VALID\"/></Location>"

        fun fails_with_unexpected_child_tags() {
            val e = assertFailsWith<UnknownXmlFieldException> {
                XML.parse(serializer, noisyXml)
            }
            assertEquals(
                "Could not find a field for name {http://www.w3.org/XML/1998/namespace}lang\n" +
                        "  candidates: houseNumber, street, city, status at position [row,col {unknown-source}]: [1,1]",
                e.message
                        )
        }


        @Test
        fun deserialize_with_unused_attributes_and_custom_handler() {
            var ignoredName: QName? = null
            var ignoredIsAttribute: Boolean? = null
            val xml = XML {
                unknownChildHandler = { _, isAttribute, name, _ ->
                    ignoredName = name
                    ignoredIsAttribute = isAttribute
                }
            }
            assertEquals(value, xml.parse(serializer, noisyXml))
            assertEquals(QName(XMLConstants.NULL_NS_URI, "unexpected", ""), ignoredName)
            assertEquals(false, ignoredIsAttribute)
        }

    }

    class SimpleClassWithNullablValueNONNULL : TestBase<NullableContainer>(
        NullableContainer("myBar"),
        NullableContainer.serializer()
                                                                          ) {
        override val expectedXML: String = "<p:NullableContainer xmlns:p=\"urn:myurn\" bar=\"myBar\"/>"
        override val expectedJson: String = "{\"bar\":\"myBar\"}"
    }

    class SimpleClassWithNullablValueNULL : TestBase<NullableContainer>(
        NullableContainer(),
        NullableContainer.serializer()
                                                                       ) {
        override val expectedXML: String = "<p:NullableContainer xmlns:p=\"urn:myurn\"/>"
        override val expectedJson: String = "{\"bar\":null}"
    }

    class ASimpleBusiness : TestBase<Business>(
        Business("ABC Corp", Address("1", "ABC road", "ABCVille")),
        Business.serializer()
                                              ) {
        override val expectedXML: String =
            "<Business name=\"ABC Corp\"><headOffice houseNumber=\"1\" street=\"ABC road\" city=\"ABCVille\" status=\"VALID\"/></Business>"
        override val expectedJson: String =
            "{\"name\":\"ABC Corp\",\"headOffice\":{\"houseNumber\":\"1\",\"street\":\"ABC road\",\"city\":\"ABCVille\",\"status\":\"VALID\"}}"
    }

    class AChamberOfCommerce : TestBase<Chamber>(
        Chamber(
            "hightech", listOf(
                Business("foo", null),
                Business("bar", null)
                              )
               ),
        Chamber.serializer()
                                                ) {
        override val expectedXML: String = "<chamber name=\"hightech\">" +
                "<member name=\"foo\"/>" +
                "<member name=\"bar\"/>" +
                "</chamber>"
        override val expectedJson: String =
            "{\"name\":\"hightech\",\"members\":[{\"name\":\"foo\",\"headOffice\":null},{\"name\":\"bar\",\"headOffice\":null}]}"
    }

    class AnEmptyChamber : TestBase<Chamber>(
        Chamber("lowtech", emptyList()),
        Chamber.serializer()
                                            ) {
        override val expectedXML: String = "<chamber name=\"lowtech\"/>"
        override val expectedJson: String = "{\"name\":\"lowtech\",\"members\":[]}"
    }

    class ACompactFragment : TestBase<CompactFragment>(
        CompactFragment(listOf(XmlEvent.NamespaceImpl("p", "urn:ns")), "<p:a>someA</p:a><b>someB</b>"),
        CompactFragment.serializer()
                                                      ) {
        override val expectedXML: String =
            "<compactFragment xmlns:p=\"urn:ns\"><p:a>someA</p:a><b>someB</b></compactFragment>"
        override val expectedJson: String =
            "{\"namespaces\":[{\"prefix\":\"p\",\"namespaceURI\":\"urn:ns\"}],\"content\":\"<p:a>someA</p:a><b>someB</b>\"}"
    }

    class ClassWithImplicitChildNamespace : TestBase<Namespaced>(
        Namespaced("foo", "bar"),
        Namespaced.serializer()
                                                                ) {
        override val expectedXML: String =
            "<xo:namespaced xmlns:xo=\"http://example.org\"><xo:elem1>foo</xo:elem1><xo:elem2>bar</xo:elem2></xo:namespaced>"
        val invalidXml =
            "<xo:namespaced xmlns:xo=\"http://example.org\"><elem1>foo</elem1><xo:elem2>bar</xo:elem2></xo:namespaced>"
        override val expectedJson: String = "{\"elem1\":\"foo\",\"elem2\":\"bar\"}"

        @Test
        fun invalidXmlDoesNotDeserialize() {
            assertFailsWith<UnknownXmlFieldException> {
                XML.parse(serializer, invalidXml)
            }
        }
    }

    class AComplexElement : TestBase<Special>(
        Special(),
        Special.serializer()
                                             ) {
        override val expectedXML: String =
            """<localname xmlns="urn:namespace" paramA="valA"><paramb xmlns="urn:ns2">1</paramb><flags xmlns:f="urn:flag">""" +
                    "<f:flag>2</f:flag>" +
                    "<f:flag>3</f:flag>" +
                    "<f:flag>4</f:flag>" +
                    "<f:flag>5</f:flag>" +
                    "<f:flag>6</f:flag>" +
                    "</flags></localname>"
        override val expectedJson: String = "{\"paramA\":\"valA\",\"paramB\":1,\"flagValues\":[2,3,4,5,6]}"
    }

    class InvertedPropertyOrder : TestBase<Inverted>(
        Inverted("value2", 7),
        Inverted.serializer()
                                                    ) {
        override val expectedXML: String = """<Inverted arg="7"><elem>value2</elem></Inverted>"""
        override val expectedJson: String = "{\"elem\":\"value2\",\"arg\":7}"

        @Test
        fun noticeMissingChild() {
            val xml = "<Inverted arg='5'/>"
            assertFailsWith<MissingFieldException> {
                XML.parse(serializer, xml)
            }
        }

        @Test
        fun noticeIncompleteSpecification() {
            val xml = "<Inverted arg='5' argx='4'><elem>v5</elem></Inverted>"
            assertFailsWith<UnknownXmlFieldException>("Could not find a field for name argx") {
                XML.parse(serializer, xml)
            }

        }
    }

    class AClassWithPolymorhpicChild : TestPolymorphicBase<Container>(
        Container("lbl", ChildA("data")),
        Container.serializer(),
        baseModule
                                                                     ) {
        override val expectedXML: String
            get() = "<Container label=\"lbl\"><childA valueA=\"data\"/></Container>"
        override val expectedJson: String
            get() = "{\"label\":\"lbl\",\"member\":{\"type\":\"nl.adaptivity.xml.serialization.ChildA\",\"valueA\":\"data\"}}"
        override val expectedNonAutoPolymorphicXML: String get() = "<Container label=\"lbl\"><member type=\".ChildA\"><value valueA=\"data\"/></member></Container>"
    }

    class AClassWithMultipleChildren : TestPolymorphicBase<Container2>(
        Container2("name2", listOf(ChildA("data"), ChildB(1, 2, 3, "xxx"))),
        Container2.serializer(),
        baseModule
                                                                      ) {
        override val expectedXML: String
            get() = "<Container2 name=\"name2\"><childA valueA=\"data\"/><better a=\"1\" b=\"2\" c=\"3\" valueB=\"xxx\"/></Container2>"
        override val expectedNonAutoPolymorphicXML: String
            get() = expectedXML
        override val expectedJson: String
            get() = "{\"name\":\"name2\",\"children\":[{\"type\":\"nl.adaptivity.xml.serialization.ChildA\",\"valueA\":\"data\"},{\"type\":\"childBNameFromAnnotation\",\"a\":1,\"b\":2,\"c\":3,\"valueB\":\"xxx\"}]}"


    }

    class AClassWithXMLPolymorphicNullableChild : TestPolymorphicBase<Container4>(
        Container4("name2", ChildA("data")),
        Container4.serializer(),
        baseModule
                                                                                 ) {
        override val expectedXML: String
            get() = "<Container4 name=\"name2\"><childA valueA=\"data\"/></Container4>"
        override val expectedNonAutoPolymorphicXML: String
            get() = expectedXML
        override val expectedJson: String
            get() = "{\"name\":\"name2\",\"child\":{\"type\":\"nl.adaptivity.xml.serialization.ChildA\",\"valueA\":\"data\"}}"


    }

    class ASimplerClassWithUnspecifiedChildren : TestPolymorphicBase<Container3>(
        Container3("name2", listOf(ChildA("data"), ChildB(4, 5, 6, "xxx"), ChildA("yyy"))),
        Container3.serializer(),
        baseModule
                                                                                ) {
        override val expectedXML: String
            get() = "<container-3 xxx=\"name2\"><childA valueA=\"data\"/><childB a=\"4\" b=\"5\" c=\"6\" valueB=\"xxx\"/><childA valueA=\"yyy\"/></container-3>"
        override val expectedJson: String
            get() = "{\"xxx\":\"name2\",\"member\":[{\"type\":\"nl.adaptivity.xml.serialization.ChildA\",\"valueA\":\"data\"},{\"type\":\"childBNameFromAnnotation\",\"a\":4,\"b\":5,\"c\":6,\"valueB\":\"xxx\"},{\"type\":\"nl.adaptivity.xml.serialization.ChildA\",\"valueA\":\"yyy\"}]}"
        override val expectedNonAutoPolymorphicXML: String
            get() = "<container-3 xxx=\"name2\"><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"data\"/></member><member type=\"childBNameFromAnnotation\"><value a=\"4\" b=\"5\" c=\"6\" valueB=\"xxx\"/></member><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"yyy\"/></member></container-3>"
    }


    class CustomSerializedClass : TestBase<CustomContainer>(
        CustomContainer(Custom("foobar")),
        CustomContainer.serializer()
                                                           ) {

        override val expectedXML: String = "<CustomContainer elem=\"foobar\"/>"
        override val expectedJson: String = "{\"nonXmlElemName\":\"foobar\"}"

    }

    class AContainerWithSealedChild : TestBase<SealedSingle>(
        SealedSingle("mySealed", SealedA("a-data")),
        SealedSingle.serializer()
                                                            ) {
        override val expectedXML: String
            get() = "<SealedSingle name=\"mySealed\"><SealedA data=\"a-data\" extra=\"2\"/></SealedSingle>"
        override val expectedJson: String
            get() = "{\"name\":\"mySealed\",\"member\":{\"data\":\"a-data\",\"extra\":\"2\"}}"
    }

    class AContainerWithSealedChildren : TestPolymorphicBase<Sealed>(
        Sealed("mySealed", listOf(SealedA("a-data"), SealedB("b-data"))),
        Sealed.serializer(),
        EmptyModule//sealedModule
                                                                    ) {
        override val expectedXML: String
            get() = "<Sealed name=\"mySealed\"><SealedA data=\"a-data\" extra=\"2\"/><SealedB_renamed main=\"b-data\" ext=\"0.5\"/></Sealed>"
        override val expectedJson: String
            get() = "{\"name\":\"mySealed\",\"members\":[{\"type\":\"nl.adaptivity.xml.serialization.SealedA\",\"data\":\"a-data\",\"extra\":\"2\"},{\"type\":\"nl.adaptivity.xml.serialization.SealedB\",\"main\":\"b-data\",\"ext\":0.5}]}"
        override val expectedNonAutoPolymorphicXML: String
            get() = "<Sealed name=\"mySealed\"><member type=\".SealedA\"><value data=\"a-data\" extra=\"2\"/></member><member type=\".SealedB\"><value main=\"b-data\" ext=\"0.5\"/></member></Sealed>"
    }

    class ComplexSealedTest : TestBase<ComplexSealedHolder>(
        ComplexSealedHolder("a", 1, 1.5f, OptionB1(5, 6, 7)),
        ComplexSealedHolder.serializer(),
        EmptyModule,
        XML(XmlConfig(autoPolymorphic = true))
                                                           ) {
        override val expectedXML: String
            get() = "<ComplexSealedHolder a=\"a\" b=\"1\" c=\"1.5\"><OptionB1 g=\"5\" h=\"6\" i=\"7\"/></ComplexSealedHolder>"
        override val expectedJson: String
            get() = "{\"a\":\"a\",\"b\":1,\"c\":1.5,\"options\":{\"type\":\"nl.adaptivity.xml.serialization.OptionB1\",\"g\":5,\"h\":6,\"i\":7}}"
    }

    class NullableListTestWithElements : TestBase<NullList>(
        NullList("A String", listOf(
                NullListElement("Another String1"),
                NullListElement("Another String2")
                              )),
        NullList.serializer()
                                               ) {
        override val expectedXML: String
            get() = "<Baz><Str>A String</Str><Bar><AnotherStr>Another String1</AnotherStr></Bar><Bar><AnotherStr>Another String2</AnotherStr></Bar></Baz>"
        override val expectedJson: String
            get() = "{\"Str\":\"A String\",\"Bar\":[{\"AnotherStr\":\"Another String1\"},{\"AnotherStr\":\"Another String2\"}]}"
    }

    class NullableListTestNull : TestBase<NullList>(
        NullList("A String"),
        NullList.serializer()
                                               ) {
        override val expectedXML: String
            get() = "<Baz><Str>A String</Str></Baz>"
        override val expectedJson: String
            get() = "{\"Str\":\"A String\",\"Bar\":null}"

    }

    @Test
    fun testEmitXmlDeclFull() {
        val expectedXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <prefix:model xmlns:prefix="namespace" version="0.0.1" anAttribute="attrValue">
                <prefix:anElement>elementValue</prefix:anElement>
                <prefix:aBlankElement/>
            </prefix:model>""".trimIndent()

        val model = SampleModel1("0.0.1", "attrValue", "elementValue")

        val format = XML {
            xmlDeclMode = XmlDeclMode.Charset
            indentString = "    "
        }

        val serializedModel = format.stringify(SampleModel1.serializer(), model).normalize().replace('\'', '"')

        assertEquals(expectedXml, serializedModel)
    }

    @Test
    fun testEmitXmlDeclMinimal() {
        val expectedXml = """
            <?xml version="1.0"?>
            <prefix:model xmlns:prefix="namespace" version="0.0.1" anAttribute="attrValue">
            <!--i--><prefix:anElement>elementValue</prefix:anElement>
            <!--i--><prefix:aBlankElement/>
            </prefix:model>""".trimIndent()

        val model = SampleModel1("0.0.1", "attrValue", "elementValue")

        val format = XML {
            xmlDeclMode = XmlDeclMode.Minimal
            indentString = "<!--i-->"
        }

        val serializedModel = format.stringify(SampleModel1.serializer(), model).normalize().replace('\'', '"')

        assertEquals(expectedXml, serializedModel)
    }

    @Test
    fun `deserialize mixed content from xml`() {
        val contentText = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val expectedObj = Tag(listOf("some text ", B("some bold text", I("some bold italic text"))))

        val xml = XML(Tag.module) {
            autoPolymorphic = true
        }
        val deserialized = xml.parse(Tag.serializer(), contentText)

        assertEquals(expectedObj, deserialized)
    }

    @Test
    fun `serialize mixed content to xml`() {
        val contentText = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val expectedObj = Tag(listOf("some text ", B("some bold text", I("some bold italic text"))))

        val xml = XML(Tag.module) {
            indentString = ""
            autoPolymorphic = true
        }

        val serialized = xml.stringify(Tag.serializer(), expectedObj)
        assertEquals(contentText, serialized)
    }

}
