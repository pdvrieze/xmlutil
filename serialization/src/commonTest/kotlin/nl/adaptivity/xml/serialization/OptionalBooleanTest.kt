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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.XmlCompositeDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OptionalBooleanTest : TestBase<OptionalBooleanTest.Location>(
    Location(Address("1600", "Pensylvania Avenue", "Washington DC")),
    Location.serializer()
) {
    override val expectedXML: String =
        "<Location><address houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\" status=\"VALID\"/></Location>"
    override val expectedJson: String =
        "{\"addres\":{\"houseNumber\":\"1600\",\"street\":\"Pensylvania Avenue\",\"city\":\"Washington DC\",\"status\":\"VALID\"},\"temperature\":NaN}"

    private val noisyXml: String
        get() =
            "<Location><unexpected><address>Foo</address></unexpected><address houseNumber=\"1600\" street=\"Pensylvania Avenue\" city=\"Washington DC\" status=\"VALID\"/></Location>"

    @Test
    fun fails_with_unexpected_child_tags() {
        val e = assertFailsWith<UnknownXmlFieldException> {
            XML.decodeFromString(serializer, noisyXml)
        }
        try {
            assertEquals(
                "Could not find a field for name Location/unexpected\n  candidates: address, temperature",
                e.message?.substringBeforeLast(" at position")
            )
        } catch (f: AssertionError) {
            f.addSuppressed(e);
            throw f
        }
    }


    @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
    @Test
    fun deserialize_with_unused_attributes_and_custom_handler() {
        var ignoredName: QName? = null
        var ignoredKind: InputKind? = null
        var ignoredDescriptor: XmlDescriptor? = null
        var ignoredCandidates: Collection<Any>? = null
        val xml = XML {
            unknownChildHandler = UnknownChildHandler { _, inputKind, descriptor, name, candidates ->
                ignoredName = name
                ignoredKind = inputKind
                ignoredDescriptor = descriptor
                ignoredCandidates = candidates
                emptyList()
            }
        }
        assertEquals(value, xml.decodeFromString(serializer, noisyXml))
        assertEquals(QName(XMLConstants.NULL_NS_URI, "unexpected", ""), ignoredName)
        assertEquals(InputKind.Element, ignoredKind)
        assertTrue(ignoredDescriptor is XmlCompositeDescriptor)
        assertEquals(QName("Location"), ignoredDescriptor?.tagName)
        assertEquals(QName("address"), ignoredDescriptor?.getElementDescriptor(0)?.tagName)
        assertEquals(QName("temperature"), ignoredDescriptor?.getElementDescriptor(1)?.tagName)
        assertEquals(setOf(QName("address"), QName("temperature")), ignoredCandidates?.toSet())
    }

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    @XmlSerialName("address")
    data class Address(
        val houseNumber: String,
        val street: String,
        val city: String,
        @XmlElement(false) val status: AddresStatus = AddresStatus.VALID
    )

    @Serializable
    data class Location(
        val addres: Address,
        @XmlDefault("NaN")
        val temperature: Double = Double.NaN
    )

}
