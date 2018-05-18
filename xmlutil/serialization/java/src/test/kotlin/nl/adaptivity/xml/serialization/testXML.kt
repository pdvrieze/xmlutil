/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml.serialization

import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerialContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JSON
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.xml.XmlEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows

object testXML : Spek(
    {
        given("A simple data class") {
            val expAddressXml = "<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\"/>"
            val address = Address("10", "Downing Street", "London")

            on("serialization with XML") {
                val serialized = XML.stringify(address)
                it("should be the expected value") {
                    assertEquals(expAddressXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(address, XML.parse<Address>(serialized))
                }
            }

            val expectedJSON = "{\"houseNumber\":\"10\",\"street\":\"Downing Street\",\"city\":\"London\"}"

            on("serialization with JSON") {
                val serialized = JSON.stringify(address)
                it("should be the expected value") {
                    assertEquals(expectedJSON, serialized)
                }

                it("should parse to the original") {
                    assertEquals(address, JSON.parse<Address>(serialized))
                }
            }
        }

        given ("A data class with optional boolean") {
            val location = Location(Address("1600", "Pensylvania Avenue", "Washington DC"))
            val expectedXml="<Location><addres houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\"/></Location>"

            on("Serialization with XML") {
                val serialized = XML.stringify(location)
                it("should serialize to the expected xml") {
                    assertEquals(expectedXml,serialized)
                }
                it("should also parse to the original") {
                    assertEquals(location, XML.parse<Location>(serialized))
                }

            }
        }


        given("A simple class with a nullable value"){
            val setValue = NullableContainer("myBar")
            val nullValue = NullableContainer()
            on("serialization of a set value") {
                val serialized = XML.stringify(setValue)
                it ("should match the expected value") {
                    assertEquals("<p:NullableContainer xmlns:p=\"urn:myurn\" bar=\"myBar\"/>", serialized)
                }
                it ("Should parse back to the original") {
                    assertEquals(setValue, XML.parse<NullableContainer>(serialized))
                }
            }
            on("serialization of a null value") {
                val serialized = XML.stringify(nullValue)
                it ("should match the expected value") {
                    assertEquals("<p:NullableContainer xmlns:p=\"urn:myurn\"/>", serialized)
                }
                it ("Should parse back to the original") {
                    assertEquals(nullValue, XML.parse<NullableContainer>(serialized))
                }
            }
        }


        given("A simple business") {
            val expBusinessXml =
                "<Business name=\"ABC Corp\"><headOffice houseNumber=\"1\" street=\"ABC road\" city=\"ABCVille\"/></Business>"

            val business = Business("ABC Corp", Address("1", "ABC road", "ABCVille"))
            on("serialization") {
                val serialized = XML.stringify(business)
                it("should equal the expected business xml") {
                    assertEquals(expBusinessXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(business, XML.parse<Business>(serialized))
                }
            }
        }

        given("A chamber of commerce") {
            val expChamber="<chamber name=\"hightech\">"+
                           "<member name=\"foo\"/>" +
                           "<member name=\"bar\"/>" +
                           "</chamber>"
            val chamber = Chamber("hightech", listOf(Business("foo", null), Business("bar", null)))

            on("serialization") {
                val serialized = XML.stringify(chamber)
                it("Should equal the chamber xml") {
                    assertEquals(expChamber, serialized)
                }

                it("should parse to the original") {
                    assertEquals(chamber, XML.parse<Chamber>(serialized))
                }
            }
        }

        given("An empty chamber") {
            val expChamber="<chamber name=\"lowtech\"/>"
            val chamber = Chamber("lowtech", emptyList())

            on("serialization") {
                val serialized = XML.stringify(chamber)
                it("Should equal the chamber xml") {
                    assertEquals(expChamber, serialized)
                }

                it("should parse to the original") {
                    assertEquals(chamber, XML.parse<Chamber>(serialized))
                }
            }
        }

        given("a compactFragment") {
            val expectedXml = "<compactFragment xmlns:p=\"urn:ns\"><p:a>someA</p:a><b>someB</b></compactFragment>"
            val fragment = CompactFragment(listOf(XmlEvent.NamespaceImpl("p", "urn:ns")), "<p:a>someA</p:a><b>someB</b>")

            on("serialization with XML") {
                val serialized = XML.stringify(fragment)
                it("Should equal the expected fragment xml") {
                    assertEquals(expectedXml, serialized)
                }

                it("should parse to the original") {
                    assertEquals(fragment, XML.parse<CompactFragment>(serialized))
                }

            }
            val expectedJSON = "{\"namespaces\":[{\"prefix\":\"p\",\"namespaceURI\":\"urn:ns\"}],\"content\":\"<p:a>someA</p:a><b>someB</b>\"}"

            on("serialization with JSON") {
                val context = SerialContext().apply {
                    registerSerializer(CompactFragment::class, CompactFragmentSerializer)
                }
                val serialized = JSON(context = context).stringify(fragment)
                it("Should equal the expected fragment JSON") {
                    assertEquals(expectedJSON, serialized)
                }

                it("should parse to the original") {
                    assertEquals(fragment, JSON(context = context).parse<CompactFragment>(serialized))
                }

            }
        }

        given("a more complex element") {
            val special = Special()
            val expectedSpecial="""<localname xmlns="urn:namespace" paramA="valA"><paramb xmlns="urn:ns2">1</paramb><flags xmlns:f="urn:flag">"""+
                                "<f:flag>2</f:flag>" +
                                "<f:flag>3</f:flag>" +
                                "<f:flag>4</f:flag>" +
                                "<f:flag>5</f:flag>" +
                                "<f:flag>6</f:flag>" +
                                "</flags></localname>"

            on("serialization") {
                val serialized = XML.stringify(special)
                it("Should equal the special xml") {
                    assertEquals(expectedSpecial, serialized)
                }

                it("should parse to the original") {
                    assertEquals(special, XML.parse<Special>(serialized))
                }
            }
        }

        given("a class that has inverted property order") {
            val inverted = Inverted("value2", 7)
            val expected = """<Inverted arg="7"><elem>value2</elem></Inverted>"""

            on("serialization") {
                val serialized = XML.stringify(inverted)
                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(inverted, XML.parse<Inverted>(serialized))
                }

            }

        }

        given("a missing child for inverted") {
            val xml = "<Inverted arg='5'/>"
            it("should throw an exception when parsing") {
                assertThrows<MissingFieldException> {
                    XML.parse<Inverted>(xml)
                }
            }
        }

        given("An incomplete xml specification for inverted") {
            val xml = "<Inverted arg='5' argx='4'><elem>v5</elem></Inverted>"
            it("should throw an exception when parsing") {
                assertThrows<SerializationException> {
                    XML.parse<Inverted>(xml)
                }
            }
        }

        given("A class with polymorphic children") {
            val poly = Container("lbl", ChildA("data"))
            val expected = "<Container label=\"lbl\"><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"data\"/></member></Container>"
            on ("serialization") {
                val serialized = XML.stringify(poly)
                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly, XML.parse<Container>(serialized))
                }

            }
        }

        given("A class with multiple children") {
            val poly2 = Container2("name2", listOf(ChildA("data"), ChildB("xxx")))
            val expected = "<Container2 name=\"name2\"><ChildA valueA=\"data\"/><better valueB=\"xxx\"/></Container2>"
            on ("serialization") {
                val serialized = XML.stringify(poly2)

                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly2, XML.parse<Container2>(serialized))
                }

            }
        }

        given("A Simpler class with multiple children without specification") {
            val poly2 = Container3("name2", listOf(ChildA("data"), ChildB("xxx"), ChildA("yyy")))
            val expected = "<Container3 xxx=\"name2\"><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"data\"/></member><member type=\"nl.adaptivity.xml.serialization.ChildB\"><value valueB=\"xxx\"/></member><member type=\"nl.adaptivity.xml.serialization.ChildA\"><value valueA=\"yyy\"/></member></Container3>"
            on ("serialization") {
                val serialized = XML.stringify(poly2)

                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(poly2, XML.parse<Container3>(serialized))
                }

            }
        }

        given("A container with a sealed child") {
            val sealed = SealedSingle("mySealed", SealedA("a-data"))
            val expected = "<SealedSingle name=\"mySealed\"><SealedA valueA=\"a-data\"/></SealedSingle>"
            on ("serialization") {
                val serialized = XML.stringify(sealed)

                xit("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(sealed, XML.parse<SealedSingle>(serialized))
                }

            }
        }

        xgiven("A container with sealed children") {
            val sealed = Sealed("mySealed", listOf(SealedA("a-data"), SealedB("b-data")))
            val expected = "<Sealed name=\"mySealed\"><SealedA data=\"a-data\" extra=\"2\"/><SealedB main=\"b-data\" ext=\"0.5\"/></Sealed>"
            on ("serialization") {
                val serialized = XML.stringify(sealed)

                // Disabled because sealed classes are broken when used in lists
                it("should equal the expected xml form") {
                    assertEquals(expected, serialized)
                }

                it("should parse to the original") {
                    assertEquals(sealed, XML.parse<Sealed>(serialized))
                }

                test("The expected value should also parse to the original") {
                    assertEquals(sealed, XML.parse<Sealed>(expected))
                }

            }
        }

    })