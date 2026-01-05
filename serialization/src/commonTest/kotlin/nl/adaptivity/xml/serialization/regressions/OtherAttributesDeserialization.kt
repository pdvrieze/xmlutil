/*
 * Copyright (c) 2023-2026.
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

@file:MustUseReturnValues

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import kotlin.test.Test
import kotlin.test.assertEquals

class OtherAttributesDeserialization {
    @Test
    fun testXmlMapSerialization() {
        @Serializable
        data class Container(
            @XmlOtherAttributes
            val attributes: Map<String, String>
        )

        val container = Container(
            mapOf(
                "key1" to "value1",
                "key2" to "value2",
            ),
        )

        @Suppress("DEPRECATION")
        val format = XML.compat { defaultPolicy { pedantic = true } }

        val xml = format.encodeToString(Container.serializer(), container)
        assertXmlEquals("<Container key1=\"value1\" key2=\"value2\"/>", xml)

        val container2 = format.decodeFromString<Container>(xml)
        println("Deserialized: $container2")

        val xml2 = format.encodeToString(Container.serializer(), container2)
        assertXmlEquals(xml, xml2)

        assertEquals(container, container2)
    }
}
