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
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SimpleDataTest : TestBase<SimpleDataTest.Address>(
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
            XML.decodeFromString(serializer, unknownValues)
        }

        val expectedMsgStart = "Could not find a field for name {http://www.w3.org/XML/1998/namespace}lang\n" +
                "  candidates: houseNumber, street, city, status at position "
        val msgSubstring = e.message?.let { it.substring(0, minOf(it.length, expectedMsgStart.length)) }

        assertEquals(expectedMsgStart, msgSubstring)
    }


    @Test
    fun deserialize_with_unused_attributes_and_custom_handler() {
        var ignoredName: QName? = null
        var ignoredKind: InputKind? = null
        val xml = XML {
            unknownChildHandler = UnknownChildHandler(){ _, inputKind, _, name, _ ->
                ignoredName = name
                ignoredKind = inputKind
                emptyList()
            }
        }
        assertEquals(value, xml.decodeFromString(serializer, unknownValues))
        assertEquals(QName(XMLConstants.XML_NS_URI, "lang", "xml"), ignoredName)
        assertEquals(InputKind.Attribute, ignoredKind)
    }

    enum class AddresStatus {
        VALID,
        INVALID,
        TEMPORARY
    }

    @Serializable
    @XmlSerialName("address", namespace = "", prefix = "")
    data class Address(
        val houseNumber: String,
        val street: String,
        val city: String,
        @XmlElement(false) val status: AddresStatus = AddresStatus.VALID
                      )

}
