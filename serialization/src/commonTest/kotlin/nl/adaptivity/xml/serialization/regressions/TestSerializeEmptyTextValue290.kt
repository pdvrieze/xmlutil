/*
 * Copyright (c) 2025.
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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * When empty text is written ignore this so that it can still result in an empty tag.
 */
class TestSerializeEmptyTextValue290 {

    val xml = XML {
        recommended()
        xmlDeclMode = XmlDeclMode.None // easier comparison
        defaultToGenericParser = true // Required to ensure the generic serializer is used.
    }

    @Test
    fun testSerializeEmptyText() {
        val actual =  xml.encodeToString(TextContainer(""))
        assertEquals("<text />", actual)
    }

    @Test
    fun testDeserializeEmptyText() {
        val actual =  xml.decodeFromString<TextContainer>("<text />")
        assertEquals("", actual.text)
    }

    @Serializable
    @SerialName("text")
    class TextContainer(@XmlValue val text: String)
}
