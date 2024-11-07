/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test

/**
 * Test case that tests a regression in the automatic allocation of fallback prefixes. The code
 * has an infinite loop (#251).
 *
 * This also tests some other parts of the behaviour that weren't caught by this particular bug.
 * It ensures that existing prefixes are reused if possible, that namespaced attributes will
 * not be written without prefix etc.
 */
class AutoPrefixLoop251 {

    @Test
    fun testPrefixLoopWorksForAttributes() {
        val actual = XML { recommended_0_90_2() }.encodeToString(Outer1("value1", "value2", "value3", "value4", "value5"))
        assertXmlEquals("<Outer1 " +
                "xmlns:n1=\"a\" " +
                "xmlns:n2=\"b\" " +
                "xmlns:n3=\"c\" " +
                "xmlns:x=\"d\" " +
                "n1:a=\"value1\" " +
                "n2:b=\"value2\" " +
                "n3:c=\"value3\" " +
                "x:d=\"value4\" " +
                "n1:e=\"value5\" " +
                "/>", actual)
    }

    @Test
    fun testPrefixLoopWorksForElements() {
        val actual = XML { recommended_0_90_2() }.encodeToString(Outer2("value1", "value2"))
        assertXmlEquals("<Outer2 xmlns:n2=\"b\"><a xmlns=\"a\">value1</a><b xmlns=\"b\">value2</b></Outer2>", actual)
    }

    @Serializable
    class Outer1(
        @XmlSerialName("a", "a", "") // not a valid attribut prefix, should revert to n1
        val a: String,

        @XmlSerialName("b", "b", "n1") // already used for a, so should revert to n2
        val b: String,

        @XmlSerialName("c", "c") // automatically n3
        val c: String,

        @XmlSerialName("d", "d", "x")
        val d: String,

        @XmlSerialName("e", "a", "y")
        val e: String,
    )

    @Serializable
    class Outer2(
        @XmlSerialName("a", "a","")
        @XmlElement(true)
        val a: String,

        @XmlSerialName("b", "b")
        @XmlElement(true)
        val b: String,
    )
}
