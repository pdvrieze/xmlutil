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
import nl.adaptivity.xml.serialization.OtherAttributeContainer.MixedAttributeContainer
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import kotlin.test.Test

class OtherAttributeContainer : PlatformXmlTestBase<MixedAttributeContainer>(
    MixedAttributeContainer(
        "value1",
        mapOf(
            QName("a", "b", "a") to "dyn1",
            QName("c", "d", "e") to "dyn2",
        ),
        "value2"
    ),
    MixedAttributeContainer.serializer(),
) {
    override val expectedXML: String =
        "<MixedAttributeContainer xmlns:a=\"a\" xmlns:e=\"c\" attr1=\"value1\" a:b=\"dyn1\" e:d=\"dyn2\" attr2=\"value2\"/>"

    @Test
    override fun testSerializeXml() {
        super.testSerializeXml()
    }

    @Test
    override fun testDeserializeXml() {
        super.testDeserializeXml()
    }

    @Serializable
    data class MixedAttributeContainer(
        val attr1: String,
        @XmlOtherAttributes
        val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>,
        val attr2: String
    )

}
