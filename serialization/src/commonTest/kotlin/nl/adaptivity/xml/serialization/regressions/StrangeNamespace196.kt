/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for but #196 with strange namespaces.
 */
class StrangeNamespace196 {

    @Test
    fun testSerialize() {
        val xml = XML {
            recommended_0_87_0 { pedantic = true }
            repairNamespaces = false
            indentString = ""
        }

//        val data = Container(Code("ABC", "null", "null", "null", LanguageTagCode("null")))
        val data = Container(Code("ABC", "null", "null", "null", LanguageTagCode("xxxx")))

        val serialized = xml.encodeToString(Container.serializer(), data)
        assertEquals(
            "<Container><ReceivingSystem xmlns=\"http://myrealns.example.com/foo\" listID=\"null\" listVersionID=\"null\" name=\"null\" languageID=\"xxxx\">ABC</ReceivingSystem></Container>",
            serialized
        )

    }

    @Serializable
    class Container(
        @XmlElement
        @XmlSerialName(
            value = "ReceivingSystem",
            namespace = "http://myrealns.example.com/foo",
        )
        val receivingSystem: Code? = null
    )


    @Serializable
    @XmlSerialName(
        value = "Code",
        namespace = "http://myrealns.example.com/bar",
    )
    data class Code(
        @XmlValue
        val `value`: String,
        val listID: String? = null,
        val listVersionID: String? = null,
        val name: String? = null,
        val languageID: LanguageTagCode? = null,
    )

    @Serializable
    @JvmInline
    value class LanguageTagCode(val lang: String)

}
