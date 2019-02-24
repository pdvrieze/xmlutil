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
import kotlinx.serialization.context.SimpleModule
import kotlinx.serialization.json.JSON
import nl.adaptivity.xml.serialization.*
import nl.adaptivity.xmlutil.StAXWriter
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.util.CompactFragment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.style.specification.describe
import java.io.CharArrayWriter

@UseExperimental(ImplicitReflectionSerializer::class)
object testXMLJVM : Spek(
    {
        describe("A simple writer") {
            val writer = XmlStreaming.newWriter(CharArrayWriter())
            it("should be a STaXwriter") {
                assertTrue(writer is StAXWriter)
            }
        }
        describe("A simple data class") {
            val expAddressXml = "<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\"/>"
            val address = Address("10", "Downing Street", "London")
            val addrSerializer = Address.serializer()

            context("serialization with XML") {
                val serialized = XML.stringify(addrSerializer, address)
                it("should be the expected value") {
                    assertEquals(expAddressXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(address, XML.parse(addrSerializer, serialized))
                }
            }

            val expectedJSON = "{\"houseNumber\":\"10\",\"street\":\"Downing Street\",\"city\":\"London\"}"

            context("serialization with JSON") {
                val serialized = JSON.stringify(addrSerializer, address)
                it("should be the expected value") {
                    assertEquals(expectedJSON, serialized)
                }

                it("should parse to the original") {
                    assertEquals(address, JSON.parse(addrSerializer, serialized))
                }
            }
        }

        describe ("A data class with optional boolean") {
            val location = Location(
                Address("1600", "Pensylvania Avenue", "Washington DC"))
            val expectedXml="<Location><address houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\"/></Location>"

            val ser = Location.serializer()

            context("Serialization with XML") {
                val serialized = XML.stringify(ser, location)
                it("should serialize to the expected xml") {
                    assertEquals(expectedXml,serialized)
                }
                it("should also parse to the original") {
                    assertEquals(location, XML.parse(ser, serialized))
                }

            }
        }


        describe("A simple class with a nullable value"){
            val setValue = NullableContainer("myBar")
            val nullValue = NullableContainer()
            val ser = NullableContainer.serializer()
            context("serialization of a set value") {
                val serialized = XML.stringify(ser, setValue)
                it ("should match the expected value") {
                    assertEquals("<p:NullableContainer xmlns:p=\"urn:myurn\" bar=\"myBar\"/>", serialized)
                }
                it ("Should parse back to the original") {
                    assertEquals(setValue, XML.parse(ser, serialized))
                }
            }
            context("serialization of a null value") {
                val serialized = XML.stringify(ser, nullValue)
                it ("should match the expected value") {
                    assertEquals("<p:NullableContainer xmlns:p=\"urn:myurn\"/>", serialized)
                }
                it ("Should parse back to the original") {
                    assertEquals(nullValue, XML.parse(ser, serialized))
                }
            }
        }


        describe("A simple business") {
            val expBusinessXml =
                "<Business name=\"ABC Corp\"><headOffice houseNumber=\"1\" street=\"ABC road\" city=\"ABCVille\"/></Business>"

            val business = Business("ABC Corp", Address("1", "ABC road", "ABCVille"))
            context("serialization") {
                val serialized = XML.stringify(business)
                it("should equal the expected business xml") {
                    assertEquals(expBusinessXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(business, XML.parse<Business>(serialized))
                }
            }
        }

        describe("A chamber of commerce") {
            val expChamber="<chamber name=\"hightech\">"+
                           "<member name=\"foo\"/>" +
                           "<member name=\"bar\"/>" +
                           "</chamber>"
            val chamber = Chamber("hightech", listOf(Business("foo", null),
                                                     Business("bar", null)))

            context("serialization") {
                val serialized = XML.stringify(chamber)
                it("Should equal the chamber xml") {
                    assertEquals(expChamber, serialized)
                }

                it("should parse to the original") {
                    assertEquals(chamber, XML.parse<Chamber>(serialized))
                }
            }
        }

        describe("An empty chamber") {
            val expChamber="<chamber name=\"lowtech\"/>"
            val chamber = Chamber("lowtech", emptyList())

            context("serialization") {
                val serialized = XML.stringify(chamber)
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
            val fragment = CompactFragment(listOf(XmlEvent.NamespaceImpl("p", "urn:ns")), "<p:a>someA</p:a><b>someB</b>")

            context("serialization with XML") {
                val serialized = XML.stringify(fragment)
                it("Should equal the expected fragment xml") {
                    assertEquals(expectedXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(fragment, XML.parse<CompactFragment>(serialized))
                }

            }
            val expectedJSON = "{\"namespaces\":[{\"prefix\":\"p\",\"namespaceURI\":\"urn:ns\"}],\"content\":\"<p:a>someA</p:a><b>someB</b>\"}"

            context("serialization with JSON") {
                val module = SimpleModule(CompactFragment::class, CompactFragmentSerializer)

                val serialized = JSON().apply { install(module) }.stringify(CompactFragmentSerializer, fragment)
                it("Should equal the expected fragment JSON") {
                    assertEquals(expectedJSON, serialized)
                }

                it("should parse to the original") {
                    assertEquals(fragment, JSON.apply { install(module) }.parse<CompactFragment>(CompactFragmentSerializer, serialized))
                }

            }
        }

        describe("a more complex element") {
            val special = Special()
            val expectedSpecial="""<localname xmlns="urn:namespace" paramA="valA"><paramb xmlns="urn:ns2">1</paramb><flags xmlns:f="urn:flag">"""+
                                "<f:flag>2</f:flag>" +
                                "<f:flag>3</f:flag>" +
                                "<f:flag>4</f:flag>" +
                                "<f:flag>5</f:flag>" +
                                "<f:flag>6</f:flag>" +
                                "</flags></localname>"

            context("serialization") {
                val serialized = XML.stringify(special)
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
                val serialized = XML.stringify(inverted)
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
                assertThrows<MissingFieldException> {
                    XML.parse<Inverted>(xml)
                }
            }
        }

        describe("An incomplete xml specification for inverted") {
            val xml = "<Inverted arg='5' argx='4'><elem>v5</elem></Inverted>"
            it("should throw an exception when parsing") {
                assertThrows<SerializationException> {
                    XML.parse<Inverted>(xml)
                }
            }
        }

        describe("A class with polymorphic children") {
            val poly = Container("lbl", ChildA("data"))
            val expected = "<Container label=\"lbl\"><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"data\"/></member></Container>"
            context ("serialization") {
                val serialized = XML.stringify(poly)
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
                val serialized = XML.stringify(poly2)

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
                val serialized = XML.stringify(poly2)

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
                val serialized = XML.stringify(sealed)

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
                val serialized = XML.stringify(sealed)

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

    })