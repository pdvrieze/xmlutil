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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import nl.adaptivity.xml.serialization.*
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private fun String.normalize() = replace(" />", "/>")

val testConfiguration = JsonConfiguration(strictMode = false)

class TestJs {

    abstract class TestBase<T>(val value: T, val serializer: KSerializer<T>) {
        abstract val expectedXML: String
        abstract val expectedJson: String

        fun serializeXml(): String = XML.stringify(serializer, value)
        fun serializeJson(): String = Json(testConfiguration).stringify(serializer, value)

        @Test
        open fun testSerializeXml() {
            assertEquals(expectedXML, serializeXml())
        }

        @Test
        open fun testDeserializeXml() {
            assertEquals(value, XML.parse(serializer, expectedXML))
        }

        @Test
        open fun testSerializeJson() {
            assertEquals(expectedJson, serializeJson())
        }

        @Test
        open fun testDeserializeJson() {
            assertEquals(value, Json(testConfiguration).parse(serializer, expectedJson))
        }

    }

    class SimpleDataTest : TestBase<Address>(
        Address("10", "Downing Street", "London"),
        Address.serializer()
                                            ) {
        override val expectedXML: String = "<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>"

        override val expectedJson: String = "{\"houseNumber\":\"10\",\"street\":\"Downing Street\",\"city\":\"London\",\"status\":\"VALID\"}"

    }

    class OptionalBooleanTest : TestBase<Location>(
        Location(Address("1600", "Pensylvania Avenue", "Washington DC")),
        Location.serializer()
                                                  ) {
        override val expectedXML: String =
            "<Location><address houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\" status=\"VALID\"/></Location>"
        override val expectedJson: String =
            "{\"addres\":{\"houseNumber\":\"1600\",\"street\":\"Pensylvania Avenue\",\"city\":\"Washington DC\",\"status\":\"VALID\"},\"temperature\":NaN}"
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
        override val expectedXML: String = """<localname xmlns="urn:namespace" paramA="valA"><paramb xmlns="urn:ns2">1</paramb><flags xmlns:f="urn:flag">"""+
                "<f:flag>2</f:flag>" +
                "<f:flag>3</f:flag>" +
                "<f:flag>4</f:flag>" +
                "<f:flag>5</f:flag>" +
                "<f:flag>6</f:flag>" +
                "</flags></localname>"
        override val expectedJson: String = "{\"paramA\":\"valA\",\"paramB\":1,\"flagValues\":[2,3,4,5,6]}"
    }

    class InvertedPropertyOrder: TestBase<Inverted>(
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

    class CustomSerializedClass: TestBase<CustomContainer>(
        CustomContainer(Custom("foobar")),
        CustomContainer.serializer()
                                                          ) {

        override val expectedXML: String ="<CustomContainer elem=\"foobar\"/>"
        override val expectedJson: String ="{\"nonXmlElemName\":\"foobar\"}"

    }

}


/*
@UseExperimental(ImplicitReflectionSerializer::class)
object testXmlCommon : Spek(
    {

        describe("A container with a property with custom deserialization") {
            val container = CustomContainer(Custom("foobar"))
            val expected = "<CustomContainer elem=\"foobar\"/>"
            context("serialization") {
                val serialized = XML.stringify(container).normalize()
                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }
                it("should parse back to the original") {
                    assertEquals(container, XML.parse(CustomContainer.serializer(), serialized))
                }

            }
        }



        describe("A class with polymorphic children") {
            val poly = Container("lbl", ChildA("data"))
            val expected = "<Container label=\"lbl\"><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"data\"/></member></Container>"
            context ("serialization") {
                val serialized = XML.stringify(poly).normalize()
                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly, XML.parse<Container>(serialized))
                }

            }
        }

        describe("A class with multiple children") {
            val poly2 = Container2("name2", listOf(ChildA("data"),
                                                   ChildB("xxx")))
            val expected = "<Container2 name=\"name2\"><ChildA valueA=\"data\"/><better valueB=\"xxx\"/></Container2>"
            context ("serialization") {
                val serialized = XML.stringify(poly2).normalize()

                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly2, XML.parse<Container2>(serialized))
                }

            }
        }

        describe("A Simpler class with multiple children without specification") {
            val poly2 = Container3("name2", listOf(ChildA("data"),
                                                   ChildB("xxx"),
                                                   ChildA("yyy")))
            val expected = "<container-3 xxx=\"name2\"><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"data\"/></member><member type=\"nl.adaptivity.xml.serialization.ChildB\"><value valueB=\"xxx\"/></member><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"yyy\"/></member></container-3>"
            context ("serialization") {
                val serialized = XML.stringify(poly2).normalize()

                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly2, XML.parse<Container3>(serialized))
                }

            }
        }

        describe("A container with a sealed child") {
            val sealed = SealedSingle("mySealed", SealedA("a-data"))
            val expected = "<SealedSingle name=\"mySealed\"><SealedA valueA=\"a-data\"/></SealedSingle>"
            context ("serialization") {
                val serialized = XML.stringify(sealed).normalize()

                xit("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(sealed, XML.parse<SealedSingle>(serialized))
                }

            }
        }

        describe("A container with sealed children") {
            val sealed = Sealed("mySealed", listOf(SealedA("a-data"),
                                                   SealedB("b-data")))
            val expected = "<Sealed name=\"mySealed\"><SealedA data=\"a-data\" extra=\"2\"/><SealedB main=\"b-data\" ext=\"0.5\"/></Sealed>"
            context ("serialization") {
                val serialized = XML.stringify(sealed).normalize()

                // Disabled because sealed classes are broken when used in lists
                xit("should equal the expected xml form", "Waiting for sealed support") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(sealed, XML.parse<Sealed>(serialized))
                }

                delegate.test("The expected value should also parse to the original", Skip.Yes("Waiting for sealed support")) {
                    assertEquals(sealed, XML.parse<Sealed>(expected))
                }

            }
        }

    })*/
