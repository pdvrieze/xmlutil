/*
 * Copyright (c) 2024-2026.
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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.adaptivity.xml.serialization.pedantic
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test

/** Regression test where `XmlChildrenName` doesn't handle default parameters correctly. */
class XmlChildrenNameDefaultNamespace252 {

    val xml get() = XML.v1.pedantic()

    @Serializable
    @XmlSerialName("container", "mynamespace", "")
    data class Container(
        @XmlSerialName("children")
        @XmlChildrenName("child" )
        val children: List<String>
    )

    @Test
    fun testSerializeList() {
        val actual = xml.encodeToString(Container(listOf("a", "b", "c")))
        assertXmlEquals("<container xmlns=\"mynamespace\"><children><child>a</child><child>b</child><child>c</child></children></container>", actual)
    }
}
