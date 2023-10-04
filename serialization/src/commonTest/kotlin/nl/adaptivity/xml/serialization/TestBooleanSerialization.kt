/*
 * Copyright (c) 2023.
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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.XmlBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestBooleanSerialization {

    @Serializable
    data class BooleanHolder(@XmlValue val bool: Boolean)

    @Serializable
    data class XmlBooleanHolder(@XmlValue val bool: XmlBoolean)

    @Test
    fun testSerializeBooleanNormal() {
        val xml = XML { recommended() }
        assertXmlEquals("<BooleanHolder>true</BooleanHolder>", xml.encodeToString(BooleanHolder(true)))
    }

    @Test
    fun testSerializeBooleanStrict() {
        val xml = XML { recommended { isStrictBoolean = true } }
        assertXmlEquals("<BooleanHolder>true</BooleanHolder>", xml.encodeToString(BooleanHolder(true)))
    }

    @Test
    fun testDeserializeBooleanNormalTrue() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>true</BooleanHolder>")
        assertEquals(BooleanHolder(true), decoded)
    }

    @Test
    fun testDeserializeBooleanNormalTrueCaps() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>TRUE</BooleanHolder>")
        assertEquals(BooleanHolder(true), decoded)
    }

    @Test
    fun testDeserializeBooleanNormalOne() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>1</BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanNormalFalse() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>false</BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanNormalFalseCaps() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>FALSE</BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanNormalZero() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>0</BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanNormalEmpty() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder></BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanNormalRandom() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>some Random value</BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanStrictTrue() {
        val xml = XML { recommended { isStrictBoolean = true } }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>true</BooleanHolder>")
        assertEquals(BooleanHolder(true), decoded)
    }

    @Test
    fun testDeserializeBooleanStrictTrueCaps() {
        val xml = XML { recommended { isStrictBoolean = true } }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>TRUE</BooleanHolder>")
            assertEquals(BooleanHolder(true), decoded)
        }
    }

    @Test
    fun testDeserializeBooleanStrictOne() {
        val xml = XML { recommended { isStrictBoolean = true } }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>1</BooleanHolder>")
        assertEquals(BooleanHolder(true), decoded)
    }

    @Test
    fun testDeserializeBooleanStrictFalse() {
        val xml = XML { recommended { isStrictBoolean = true } }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>false</BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanStrictFalseCaps() {
        val xml = XML { recommended { isStrictBoolean = true } }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>FALSE</BooleanHolder>")
            assertEquals(BooleanHolder(false), decoded)
        }
    }

    @Test
    fun testDeserializeBooleanStrictZero() {
        val xml = XML { recommended { isStrictBoolean = true } }
        val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>0</BooleanHolder>")
        assertEquals(BooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeBooleanStrictEmpty() {
        val xml = XML { recommended { isStrictBoolean = true } }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder></BooleanHolder>")
            assertEquals(BooleanHolder(false), decoded)
        }
    }

    @Test
    fun testDeserializeBooleanStrictRandom() {
        val xml = XML { recommended { isStrictBoolean = true } }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<BooleanHolder>("<BooleanHolder>some Random value</BooleanHolder>")
            assertEquals(BooleanHolder(false), decoded)
        }
    }

    @Test
    fun testDeserializeXmlBooleanTrue() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder>true</XmlBooleanHolder>")
        assertEquals(XmlBooleanHolder(true), decoded)
    }

    @Test
    fun testDeserializeXmlBooleanTrueCaps() {
        val xml = XML { recommended() }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder>TRUE</XmlBooleanHolder>")
            assertEquals(XmlBooleanHolder(true), decoded)
        }
    }

    @Test
    fun testDeserializeXmlBooleanOne() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder>1</XmlBooleanHolder>")
        assertEquals(XmlBooleanHolder(true), decoded)
    }

    @Test
    fun testDeserializeXmlBooleanFalse() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder>false</XmlBooleanHolder>")
        assertEquals(XmlBooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeXmlBooleanFalseCaps() {
        val xml = XML { recommended() }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder>FALSE</XmlBooleanHolder>")
            assertEquals(XmlBooleanHolder(false), decoded)
        }
    }

    @Test
    fun testDeserializeXmlBooleanZero() {
        val xml = XML { recommended() }
        val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder>0</XmlBooleanHolder>")
        assertEquals(XmlBooleanHolder(false), decoded)
    }

    @Test
    fun testDeserializeXmlBooleanEmpty() {
        val xml = XML { recommended() }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder></XmlBooleanHolder>")
            assertEquals(XmlBooleanHolder(false), decoded)
        }
    }

    @Test
    fun testDeserializeXmlBooleanRandom() {
        val xml = XML { recommended() }
        assertFailsWith<NumberFormatException> {
            val decoded = xml.decodeFromString<XmlBooleanHolder>("<XmlBooleanHolder>some Random value</XmlBooleanHolder>")
            assertEquals(XmlBooleanHolder(false), decoded)
        }
    }

}
