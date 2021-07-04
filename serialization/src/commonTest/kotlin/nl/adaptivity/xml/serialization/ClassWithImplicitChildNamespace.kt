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
import nl.adaptivity.xmlutil.serialization.UnknownXmlFieldException
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ClassWithImplicitChildNamespace : TestBase<ClassWithImplicitChildNamespace.Namespaced>(
    Namespaced("foo", "bar", "bla", "lalala", "tada"),
    Namespaced.serializer()
                                                                                            ) {
    override val expectedXML: String =
        ExpectedSerialization.classWithImplicitChildNamespaceXml
    val invalidXml =
        "<xo:namespaced xmlns:xo=\"http://example.org\" xmlns:p3=\"http://example.org/2\" p3:Elem3=\"bla\" elem4=\"lalala\" xmlns:n1=\"urn:foobar\" n1:Elem5=\"tada\"><elem1>foo</elem1><xo:elem2>bar</xo:elem2></xo:namespaced>"
    override val expectedJson: String =
        "{\"elem1\":\"foo\",\"elem2\":\"bar\",\"elem3\":\"bla\",\"elem4\":\"lalala\",\"elem5\":\"tada\"}"

    @Test
    fun invalidXmlDoesNotDeserialize() {
        assertFailsWith<UnknownXmlFieldException> {
            XML.decodeFromString(serializer, invalidXml)
        }
    }

    @Serializable
    @XmlSerialName("namespaced", "http://example.org", "xo")
    data class Namespaced(
        @XmlElement(true)
        val elem1: String,
        @XmlSerialName("Elem2", "urn:myurn", "p2")
        @XmlElement(true)
        val elem2: String,
        @XmlSerialName("Elem3", "http://example.org/2", "p3")
        @XmlElement(false)
        val elem3: String,
        @XmlElement(false)
        val elem4: String,
        @XmlSerialName("Elem5", "urn:foobar", "")
        @XmlElement(false)
        val elem5: String
                         )

}
