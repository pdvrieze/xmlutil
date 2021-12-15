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
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassWithNullableUDValueNULL : TestBase<ClassWithNullableUDValueNULL.ContainerOfUserNullable>(
    ContainerOfUserNullable(null),
    ContainerOfUserNullable.serializer()
) {
    override val expectedXML: String = "<ContainerOfUserNullable/>"
    override val expectedJson: String = "{\"data\":null}"
    val expectedXMLNil: String =
        "<ContainerOfUserNullable><SimpleUserType xmlns:xsi=\"${XMLConstants.XSI_NS_URI}\" xsi:nil=\"true\"/></ContainerOfUserNullable>"

    @Test
    fun testSerializeXmlWithXSINil() {
        val xml = baseXmlFormat.copy { nilAttribute = NIL_ATTRIBUTE_NAME to "true"}
        val serialized = xml.encodeToString(serializer, value).replace(" />", "/>")
        assertEquals(expectedXMLNil, serialized)
    }

    @Test
    fun testDeserializeXmlWithXSINil() {
        val deserialized = baseXmlFormat.decodeFromString(serializer, expectedXMLNil)
        assertEquals(value, deserialized)
    }

    @Test
    fun testSerializeXmlWithNilAttribute() {
        val expected =expectedXMLNil
            .replace(XMLConstants.XSI_NS_URI, "urn:foo")
            .replace("xmlns:xsi", "xmlns:ns5")
            .replace("xsi:nil","ns5:isNull")
            .replace("true", "yes")
        val xml = baseXmlFormat.copy { nilAttribute = QName("urn:foo", "isNull", "ns5") to "yes"}
        val serialized = xml.encodeToString(serializer, value).replace(" />", "/>")
        assertXmlEquals(expected, serialized)
    }

    @Test
    fun testDeserializeXmlWithNilAttribute() {
        val serializedXml =expectedXMLNil
            .replace(XMLConstants.XSI_NS_URI, "urn:foo")
            .replace("xmlns:xsi", "xmlns:tada")
            .replace("xsi:nil","tada:isNull")
            .replace("true", "yes")
        val xml = baseXmlFormat.copy { nilAttribute = QName("urn:foo", "isNull", "ns5") to "yes"}
        val newValue = xml.decodeFromString(serializer, serializedXml)
        assertEquals(value, newValue)
    }

    @Serializable
    data class ContainerOfUserNullable(val data: SimpleUserType?)

    @Serializable
    data class SimpleUserType(val data: String)


    companion object {
        val NIL_ATTRIBUTE_NAME = QName(XMLConstants.XSI_NS_URI, "nil", XMLConstants.XSI_PREFIX)
    }

}
