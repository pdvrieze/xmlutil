/*
 * Copyright (c) 2024-2025.
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
import nl.adaptivity.xml.serialization.pedantic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML1_0
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Value members should not be ordered differently.
 */
class ValueMemberSerialOrder195 {

    val xml get() = XML1_0.pedantic { setIndent(0); xmlDeclMode = XmlDeclMode.None }

    @Test
    fun testOrder() {
        val foo = Foo("a", Foo.Code("b"))
        assertEquals(
            expected = "<Foo><ServiceName>a</ServiceName><SendingSystem>b</SendingSystem></Foo>",
            actual = xml.encodeToString(foo) // <Foo><SendingSystem>b</SendingSystem><ServiceName>a</ServiceName></Foo>
        )
    }

    @Serializable
    @XmlSerialName("Foo")
    data class Foo(
        @XmlElement(true)
        @XmlSerialName("ServiceName","", "")
        val serviceName: String? = null,

        @XmlElement
        @XmlSerialName("SendingSystem","", "")
        val sendingSystem: Code? = null,
    ) {
        @JvmInline
        @Serializable
        @XmlSerialName("Code","", "")
        value class Code(
            val `value`: String,
        )
    }
}
