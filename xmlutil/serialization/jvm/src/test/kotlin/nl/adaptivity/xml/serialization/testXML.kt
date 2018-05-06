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

import nl.adaptivity.util.xml.CompactFragment
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.jupiter.api.Assertions.assertEquals

object testXML : Spek(
    {
        given("A simple data class") {
            val expAddressXml: String = "<address houseNumber=\"10\" street=\"Downing Street\" city=\"London\"></address>"
            val address = Address("10", "Downing Street", "London")
            on("serialization") {
                val serialized = XML.stringify(address)
                it("should be the expected value") {
                    assertEquals(expAddressXml, serialized)
                }
            }
        }

        given("A simple business") {
            val expBusinessXml =
                "<Business name=\"ABC Corp\"><headOffice houseNumber=\"1\" street=\"ABC road\" city=\"ABCVille\"></headOffice></Business>"

            val business = Business("ABC Corp", Address("1", "ABC road", "ABCVille"))
            on("serialization") {
                val serialized = XML.stringify(business)
                it("should equal the expected business xml") {
                    assertEquals(expBusinessXml, serialized)
                }
            }
        }

        given("A chamber of commerce") {
            val expChamber="<chamber name=\"hightech\">"+
                           "<member name=\"foo\"></member>" +
                           "<member name=\"bar\"></member>" +
                           "</chamber>"
            val chamber = Chamber("hightech", listOf(Business("foo", null), Business("bar", null)))

            on("serialization") {
                val serialized = XML.stringify(chamber)
                it("Should equal the chamber xml") {
                    assertEquals(expChamber, serialized)
                }
            }
        }

        given("A compactFragment") {
            val expectedXml = "<compactFragment><a>someA</a><b>someB</b></compactFragment>"
            val fragment = CompactFragment("<a>someA</a><b>someB</b>")
            on("serialization") {
                val serialized = XML.stringify(fragment)
                it("Should equal the expected fragment xml") {
                    assertEquals(expectedXml, serialized)
                }

            }
        }
    })