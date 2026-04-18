/*
 * Copyright (c) 2026.
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
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialException
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.*

class LinePosition253 {


    @Serializable
    @XmlSerialName("item")
    data class Item(
        @SerialName("long_description") val longDescription: String? = null,
        @SerialName("price") val price: Double? = null,
    )

    @Test
    fun locationColumnDrift() {
        val xml = """<?xml version="1.0"?>   
            |     <item long_description="x" price="abc"/>""".trimMargin()

        val e = assertFailsWith<XmlSerialException> {
            XML.v1 { defaultToGenericParser = true }.decodeFromString<Item>(xml)
        }

        val location = e.extLocationInfo
        assertNotNull(location)
        assertEquals("For input string: \"abc\"", e.rawMessage)
        assertEquals("item/price", e.errContext)
        assertIs<NumberFormatException>(e.cause,  "The underlying exception should be a NumberFormatException")
        val extLocationInfo = location as XmlReader.ExtLocationInfo
        assertEquals(2, extLocationInfo.line)
        assertEquals(6, extLocationInfo.col)
    }
}
