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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XML1_0
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributeWithNamespace179 {
    val xml get() = XML1_0.recommended {
            policy {
                isStrictAttributeNames = true
                pedantic = true
            }
            defaultToGenericParser = true
            xmlDeclMode = XmlDeclMode.None
            setIndent(0)
        }

    @Test
    fun testSerializeDefaultInstance() {
        @Suppress("DEPRECATION")
        testSerializeDefaultInstance(XML.compat.instance)

    }

    @Test
    fun testSerializeDefaultInstance1_0() {
        @Suppress("DEPRECATION")
        testSerializeDefaultInstance(XML1_0.compactInstance)

    }

    private fun testSerializeDefaultInstance(format: XML) {
        val food = Food("veryTasty", "burgers", "pricy", "Pricy Burgers", 500)
        val expected = "<ns:food xmlns:ns=\"https://schema.restaurant.info\" ns:istasty=\"veryTasty\">" +
                "<name>burgers</name>" +
                "<price>pricy</price>" +
                "<description>Pricy Burgers</description>" +
                "<calories>500</calories>" +
                "</ns:food>"


        @Suppress("DEPRECATION")
        val serialized = format.encodeToString(food)
        assertEquals(expected, serialized)
    }

    @Test
    fun testSerializeConfiguredInstance() {
        val food = Food("veryTasty", "burgers", "pricy", "Pricy Burgers", 500)
        val expected = "<ns:food xmlns:ns=\"https://schema.restaurant.info\" ns:istasty=\"veryTasty\">" +
                "<name>burgers</name>" +
                "<price>pricy</price>" +
                "<description>Pricy Burgers</description>" +
                "<calories>500</calories>" +
                "</ns:food>"

        val serialized = xml.encodeToString(food)
        assertEquals(expected, serialized)

    }

    @Test
    fun testSerializeComplex() {
        val menu = FoodMenu(listOf(Food("true")))
        val expected = "<?xml version='1.1' encoding='UTF-8' ?>\n" +
                "<ns:breakfast_menu xmlns:ns=\"https://schema.restaurant.info\">\n" +
                " <ns:food ns:istasty=\"true\" />\n" +
                "</ns:breakfast_menu>"

        val xml = XML1_0.recommended {
            xmlDeclMode = XmlDeclMode.Charset

            repairNamespaces = true
            setIndent(1)
            defaultToGenericParser = true
        }

        val serialized = xml.encodeToString(menu)
        assertEquals(expected, serialized)

    }

    @Serializable
    @XmlSerialName("breakfast_menu", "https://schema.restaurant.info", "ns" )
    class FoodMenu(val food: List<Food>)

    @Serializable
    @XmlSerialName("food", "https://schema.restaurant.info", "ns" )
    data class Food (
        @XmlElement(false)
        @XmlSerialName("istasty", "https://schema.restaurant.info", "ns" )
        var istasty : String,
        @XmlElement(true)
        @XmlSerialName("name", "", "")
        var name : String? = null,
        @XmlElement(true)
        @XmlSerialName("price", "", "")
        var price : String? = null,
        @XmlElement(true)
        @XmlSerialName("description", "", "")
        var description : String? = null,
        @XmlElement(true)
        @XmlSerialName("calories","", "")
        var calories: Int? = null
    )
}
