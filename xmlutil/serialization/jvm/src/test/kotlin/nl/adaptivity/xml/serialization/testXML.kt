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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.jupiter.api.Assertions.assertEquals

@Serializable
@XmlSerialName("address")
data class Address(val houseNumber: String, val street: String, val city: String)

@Serializable
data class Business(val name: String, val headOffice: Address)


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
                """|<Business name="ABC Corp">
                   |  <Address houseNumber="1" street="ABC road" city="ABCVille"/>
                   |</Business>""".trimMargin("|")

            val business = Business("ABC Corp", Address("1", "ABC road", "ABCVille"))
            on("serialization") {
                val serialized = XML.stringify(business)
                it("should be the expected value") {
                    assertEquals(expBusinessXml, serialized)
                }
            }
        }
    })