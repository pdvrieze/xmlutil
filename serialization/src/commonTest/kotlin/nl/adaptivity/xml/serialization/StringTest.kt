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

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.smartStartTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StringTest : PlatformTestBase<String>(
    "foobar",
    String.serializer()
) {
    override val expectedXML: String =
        "<xsd:string xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">foobar</xsd:string>"

    override val expectedJson: String =
        "\"foobar\""

    @Test
    fun testNoXMLNSDecl() {
        val serialized = baseXmlFormat.encodeToString<String>("baz")
        assertFalse("xmlns:xmlns" in serialized)
    }

    @Test
    fun testSmartTag() {
        val result = buildString {
            XmlStreaming.newWriter(this).use {
                it.smartStartTag(QName("http://www.w3.org/2001/XMLSchema", "string", "xsd")) {
                    text("foo")
                }
            }
        }
        assertEquals("<xsd:string xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">foo</xsd:string>", result)
    }

}
