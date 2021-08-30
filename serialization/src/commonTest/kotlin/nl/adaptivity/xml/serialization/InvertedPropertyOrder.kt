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

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import nl.adaptivity.xmlutil.serialization.UnknownXmlFieldException
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import kotlin.test.Test
import kotlin.test.assertFailsWith

class InvertedPropertyOrder : TestBase<InvertedPropertyOrder.Inverted>(
    Inverted("value2", 7),
    Inverted.serializer()
                                                                      ) {
    override val expectedXML: String = """<Inverted arg="7"><elem>value2</elem></Inverted>"""
    override val expectedJson: String = "{\"elem\":\"value2\",\"arg\":7}"

    @Test
    fun noticeMissingChild() {
        val xml = "<Inverted arg='5'/>"
        assertFailsWith<SerializationException> {
            XML.decodeFromString(serializer, xml)
        }
    }

    @Test
    fun noticeIncompleteSpecification() {
        val xml = "<Inverted arg='5' argx='4'><elem>v5</elem></Inverted>"
        assertFailsWith<UnknownXmlFieldException>("Could not find a field for name argx") {
            XML.decodeFromString(serializer, xml)
        }

    }

    @Serializable
    data class Inverted(
        @Required
        @XmlElement
        val elem: String = "value",
        @Required
        val arg: Short = 6
    )

}
