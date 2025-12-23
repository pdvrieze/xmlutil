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

@file:MustUseReturnValues

package net.devrieze.serialization.examples.dynamictagnames

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicContainerSerializerTest {

    val xml = XML.compat { setIndent(2) }

    @Test
    fun testEncode() {
        val actual = xml.encodeToString(ktData)

        assertXmlEquals(XML_DATA, actual)
    }

    @Test
    fun testDecode() {
        val decoded = xml.decodeFromString<Container>(XML_DATA)
        assertEquals(ktData, decoded)
    }

    companion object {
        const val XML_DATA =
            "<Container><Test_123 attr=\"42\"><data>someData</data></Test_123><Test_456 attr=\"71\"><data>moreData</data></Test_456></Container>"

        val ktData = Container(
            listOf(
                TestElement(123, 42, "someData"),
                TestElement(456, 71, "moreData")
            )
        )

    }
}
