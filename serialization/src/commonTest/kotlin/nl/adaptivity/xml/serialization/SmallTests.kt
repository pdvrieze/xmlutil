/*
 * Copyright (c) 2022.
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

@file:OptIn(ExperimentalXmlUtilApi::class)

package nl.adaptivity.xml.serialization

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import kotlin.test.*

class SmallTests {

    /**
     * Test class for #112
     */
    @Serializable
    @XmlSerialName("registry", "", "")
    data class Test1(val comment: String) {
        companion object {
            val TESTCOMMENT =
                """|
                   |Copyright 2015-2022 The Khronos Group Inc.
                   |
                   |SPDX-License-Identifier: Apache-2.0 OR MIT
                   |""".trimMargin("|")

            val TESTDATA =
                """|<?xml version="1.0" encoding="UTF-8"?>
                   |<registry>
                   |    <comment>$TESTCOMMENT</comment>
                   |</registry>""".trimMargin("|")

            val xml: XML = XML {
                indent = 4
                xmlDeclMode = XmlDeclMode.Charset
                xmlVersion = XmlVersion.XML10
                policy = object : DefaultXmlSerializationPolicy(pedantic = true, throwOnRepeatedElement = true) {
                    override fun effectiveOutputKind(
                        serializerParent: SafeParentInfo,
                        tagParent: SafeParentInfo,
                        canBeAttribute: Boolean
                    ): OutputKind {
                        return OutputKind.Element
                    }
                }
            }

        }
    }

    @Serializable
    data class Container(val inner: Test1)

    /**
     * Test for #112
     */
    @Test
    fun test1Deserialization() {
        val actual = Test1.xml.decodeFromString<Test1>(Test1.TESTDATA)
        assertEquals(Test1(Test1.TESTCOMMENT), actual)
    }

    /**
     * Test for #112
     */
    @Test
    fun test1DeserializationErrorMessage() {
        val e = assertFails {
            XML.decodeFromString<Test1>(Test1.TESTDATA)
        }
        assertContains(e.message ?: fail("Missing message"), "candidates: comment (Attribute)")
    }

    /**
     * Test for #110
     */
    @Test
    fun test1Serialization() {
        val actual = Test1.xml.encodeToString(Test1(Test1.TESTCOMMENT))
        assertXmlEquals(Test1.TESTDATA, actual)
    }

    @Test
    fun testDetectDuplicateElements() {
        val xml = """<Container><registry comment="value" /><registry comment="value2" /></Container>"""
        val t = assertFailsWith<XmlSerialException> {
            val decoded = XML { recommended() }.decodeFromString<Container>(xml)
            assertEquals(Test1("value"), decoded.inner)
        }
        assertContains(t.message ?: "", "duplicate child", true)
    }

}
