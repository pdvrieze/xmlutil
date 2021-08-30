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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.*
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

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
            format.encodeToString(serializer, data)
        }
        assertTrue(e is XmlSerialException)
        assertTrue(e.message?.contains("@XmlValue") == true)
    }

    @Test
    fun testDeserializeInvalid1() {
        val e = assertFails {
            format.decodeFromString(serializer, invalidXML1)
        }
        assertTrue(e is XmlSerialException)
        assertTrue(e.message?.contains("@XmlValue") == true)
    }

    @Test
    fun testDeserializeInvalid2() {
        val e = assertFails {
            format.decodeFromString(serializer, invalidXML2)
        }
        assertTrue(e is XmlSerialException)
        assertTrue(e.message?.contains("@XmlValue") == true)
    }

    @Test
    fun testDeserializeInvalid3() {
        val e = assertFails {
            format.decodeFromString(serializer, invalidXML3)
        }
        assertTrue(e is XmlSerialException)
        assertTrue(e.message?.contains("@XmlValue") == true)
    }

    @Serializable
    data class InvalidValueContainer(@XmlValue val content:String, val element: Address)

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    @XmlSerialName("address")
    data class Address(
        val houseNumber: String,
        val street: String,
        val city: String,
        @XmlElement(false) val status: AddresStatus = AddresStatus.VALID
    )

}
