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

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.parse
import nl.adaptivity.xml.serialization.*
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.util.CompactFragment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private fun String.normalize() = replace(" />", "/>")

@UseExperimental(ImplicitReflectionSerializer::class)
object testXmlCommon : Spek(
    {
        describe("A simple data class") {
            val expAddressXml =
                "<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>"
            val address = Address("10", "Downing Street", "London")
            val addrSerializer = Address.serializer()

            context("serialization with XML") {
                val serialized = XML.stringify(addrSerializer, address).normalize()
                it("should be the expected value") {
                    assertEquals(expAddressXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(address, XML.parse(addrSerializer, serialized))
                }
            }

            val expectedJSON =
                "{\"houseNumber\":\"10\",\"street\":\"Downing Street\",\"city\":\"London\",\"status\":\"VALID\"}"

            context("serialization with JSON") {
                val serialized = Json.stringify(addrSerializer, address).normalize()
                it("should be the expected value") {
                    assertEquals(expectedJSON, serialized)
                }

                it("should parse to the original") {
                    assertEquals(address, Json.parse(addrSerializer, serialized))
                }
            }
        }

        describe("A data class with optional boolean") {
            val location = Location(
                Address("1600", "Pensylvania Avenue", "Washington DC")
                                   )
            val expectedXml =
                "<Location><address houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\" status=\"VALID\"/></Location>"

            val ser = Location.serializer()

            context("Serialization with XML") {
                val serialized = XML.stringify(ser, location).normalize()
                it("should serialize to the expected xml") {
                    assertEquals(expectedXml, serialized)
                }
                it("should also parse to the original") {
                    assertEquals(location, XML.parse(ser, serialized))
                }

            }
        }


        describe("A simple class with a nullable value") {
            val setValue = NullableContainer("myBar")
            val nullValue = NullableContainer()
            val ser = NullableContainer.serializer()
            context("serialization of a set value") {
                val serialized = XML.stringify(ser, setValue).normalize()
                it("should match the expected value") {
                    assertEquals("<p:NullableContainer xmlns:p=\"urn:myurn\" bar=\"myBar\"/>", serialized)
                }
                it("Should parse back to the original") {
                    assertEquals(setValue, XML.parse(ser, serialized))
                }
            }
            context("serialization of a null value") {
                val serialized = XML.stringify(ser, nullValue).normalize()
                it("should match the expected value") {
                    assertEquals("<p:NullableContainer xmlns:p=\"urn:myurn\"/>", serialized)
                }
                it("Should parse back to the original") {
                    assertEquals(nullValue, XML.parse(ser, serialized))
                }
            }
        }


        describe("A simple business") {
            val expBusinessXml =
                "<Business name=\"ABC Corp\"><headOffice houseNumber=\"1\" street=\"ABC road\" city=\"ABCVille\" status=\"VALID\"/></Business>"

            val business = Business("ABC Corp", Address("1", "ABC road", "ABCVille"))
            context("serialization") {
                val serialized = XML.stringify(business).normalize()
                it("should equal the expected business xml") {
                    assertEquals(expBusinessXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(business, XML.parse<Business>(serialized))
                }
            }
        }

        describe("A chamber of commerce") {
            val expChamber = "<chamber name=\"hightech\">" +
                    "<member name=\"foo\"/>" +
                    "<member name=\"bar\"/>" +
                    "</chamber>"
            val chamber = Chamber(
                "hightech", listOf(
                    Business("foo", null),
                    Business("bar", null)
                                  )
                                 )

            context("serialization") {
                val serialized = XML.stringify(chamber).normalize()
                it("Should equal the chamber xml") {
                    assertEquals(expChamber, serialized)
                }

                it("should parse to the original") {
                    assertEquals(chamber, XML.parse<Chamber>(serialized))
                }
            }
        }

        describe("An empty chamber") {
            val expChamber = "<chamber name=\"lowtech\"/>"
            val chamber = Chamber("lowtech", emptyList())

            context("serialization") {
                val serialized = XML.stringify(chamber).normalize()
                it("Should equal the chamber xml") {
                    assertEquals(expChamber, serialized)
                }

                it("should parse to the original") {
                    assertEquals(chamber, XML.parse<Chamber>(serialized))
                }
            }
        }

        describe("a compactFragment") {
            val expectedXml = "<compactFragment xmlns:p=\"urn:ns\"><p:a>someA</p:a><b>someB</b></compactFragment>"
            val fragment =
                CompactFragment(listOf(XmlEvent.NamespaceImpl("p", "urn:ns")), "<p:a>someA</p:a><b>someB</b>")

            context("serialization with XML") {
                val serialized = XML.stringify(fragment).normalize()
                it("Should equal the expected fragment xml") {
                    assertEquals(expectedXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(fragment, XML.parse<CompactFragment>(serialized))
                }

            }
            val expectedJSON =
                "{\"namespaces\":[{\"prefix\":\"p\",\"namespaceURI\":\"urn:ns\"}],\"content\":\"<p:a>someA</p:a><b>someB</b>\"}"

            context("serialization with JSON") {
                val module = serializersModuleOf(CompactFragment::class, CompactFragmentSerializer)

                val serialized = Json(context = module).stringify(CompactFragmentSerializer, fragment).normalize()
                it("Should equal the expected fragment JSON") {
                    assertEquals(expectedJSON, serialized)
                }

                it("should parse to the original") {
                    assertEquals(fragment, Json(context = module).parse(CompactFragmentSerializer, serialized))
                }

            }
        }

        describe("A class with a namespace, but not explicit on its children") {
            val value = Namespaced("foo", "bar")
            val expectedXml =
                "<xo:namespaced xmlns:xo=\"http://example.org\"><xo:elem1>foo</xo:elem1><xo:elem2>bar</xo:elem2></xo:namespaced>"

            context("Serialization") {
                val serialized = XML.stringify(value).normalize()
                it("should equal the expected xml") {
                    assertEquals(expectedXml, serialized)
                }
                it("should deserialize to the original") {
                    assertEquals(value, XML.parse(Namespaced.serializer(), serialized))
                }
            }
            context("Invalid xml") {
                val invalidXml =
                    "<xo:namespaced xmlns:xo=\"http://example.org\"><elem1>foo</elem1><xo:elem2>bar</xo:elem2></xo:namespaced>"
                it("should fail") {
                    assertFailsWith<UnknownXmlFieldException> {
                        XML.parse(Namespaced.serializer(), invalidXml)
                    }
                }

            }
        }

        describe("a more complex element") {
            val special = Special()
            val expectedSpecial =
                """<localname xmlns="urn:namespace" paramA="valA"><paramb xmlns="urn:ns2">1</paramb><flags xmlns:f="urn:flag">""" +
                        "<f:flag>2</f:flag>" +
                        "<f:flag>3</f:flag>" +
                        "<f:flag>4</f:flag>" +
                        "<f:flag>5</f:flag>" +
                        "<f:flag>6</f:flag>" +
                        "</flags></localname>"

            context("serialization") {
                val serialized = XML.stringify(special).normalize()
                it("Should equal the special xml") {
                    assertEquals(expectedSpecial, serialized)
                }

                it("should parse to the original") {
                    assertEquals(special, XML.parse<Special>(serialized))
                }
            }
        }

        describe("a class that has inverted property order") {
            val inverted = Inverted("value2", 7)
            val expected = """<Inverted arg="7"><elem>value2</elem></Inverted>"""

            context("serialization") {
                val serialized = XML.stringify(inverted).normalize()
                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(inverted, XML.parse<Inverted>(serialized))
                }

            }

        }

        describe("a missing child for inverted") {
            val xml = "<Inverted arg='5'/>"
            it("should throw an exception when parsing") {
                assertFailsWith<MissingFieldException> {
                    XML.parse<Inverted>(xml)
                }
            }
        }

        describe("An incomplete xml specification for inverted") {
            val xml = "<Inverted arg='5' argx='4'><elem>v5</elem></Inverted>"
            it("should throw an exception when parsing") {
                assertFailsWith<SerializationException> {
                    XML.parse<Inverted>(xml)
                }
            }
        }

        describe("A class with a polymorphic child") {
            val poly = Container("lbl", ChildA("data"))
            val expected =
                "<Container label=\"lbl\"><member type=\".ChildA\"><value valueA=\"data\"/></member></Container>"
            context("serialization") {
                val serialized = XML(context = baseModule).stringify(poly).normalize()
                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly, XML(context = baseModule).parse(Container.serializer(), serialized))
                }

            }
        }

        describe("A class with multiple children") {
            val poly2 = Container2(
                "name2", listOf(
                    ChildA("data"),
                    ChildB("xxx")
                               )
                                  )
            val expected = "<Container2 name=\"name2\"><ChildA valueA=\"data\"/><better valueB=\"xxx\"/></Container2>"
            context("serialization") {
                val serialized = XML(context = baseModule).stringify(poly2).normalize()

                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly2, XML(context = baseModule).parse<Container2>(serialized))
                }

            }
        }

        describe("A Simpler class with multiple children without specification") {
            val poly2 = Container3(
                "name2", listOf(
                    ChildA("data"),
                    ChildB("xxx"),
                    ChildA("yyy")
                               )
                                  )
            val expected =
                "<container-3 xxx=\"name2\"><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"data\"/></member><member type=\"nl.adaptivity.xml.serialization.ChildB\"><value valueB=\"xxx\"/></member><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"yyy\"/></member></container-3>"
            context("serialization") {
                val serialized = XML(context = baseModule).stringify(poly2).normalize()

                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly2, XML(context = baseModule).parse<Container3>(serialized))
                }

            }
        }

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

        describe("A container with a sealed child") {
            val sealed = SealedSingle("mySealed", SealedA("a-data"))
            val expected = "<SealedSingle name=\"mySealed\"><SealedA data=\"a-data\" extra=\"2\"/></SealedSingle>"
            context("serialization") {
                val serialized = XML.stringify(sealed).normalize()

                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(sealed, XML.parse<SealedSingle>(serialized))
                }

            }
        }

        describe("A container with sealed children") {
            val sealed = Sealed(
                "mySealed", listOf(
                    SealedA("a-data"),
                    SealedB("b-data")
                                  )
                               )
            val expected =
                "<Sealed name=\"mySealed\"><SealedA data=\"a-data\" extra=\"2\"/><SealedB main=\"b-data\" ext=\"0.5\"/></Sealed>"
            context("serialization") {
                val serialized = XML(context = sealedModule).stringify(sealed).normalize()

                // Disabled because sealed classes are broken when used in lists
                xit("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(sealed, XML(context = sealedModule).parse<Sealed>(serialized))
                }

                delegate.test(
                    "The expected value should also parse to the original",
                    Skip.Yes("Waiting for sealed support")
                             ) {
                    assertEquals(sealed, XML(context = sealedModule).parse<Sealed>(expected))
                }

            }
        }

    })
