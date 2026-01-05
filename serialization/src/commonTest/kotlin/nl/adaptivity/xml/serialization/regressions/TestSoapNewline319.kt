/*
 * Copyright (c) 2025-2026.
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

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.serialization.recommended
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSoapNewline319 {

    @Test
    fun faultExampleWithTypedDetails() {
        val xml = XML.v1.recommended(
            serializersModule = SerializersModule {
                polymorphic(Any::class, FooString::class, FooString.serializer())
            }
        )

        val faultMessage = SoapFault(detail = FooString)

        assertEquals(
            expected = faultMessage,
            actual = xml.decodeFromString(
                SoapFault.serializer(),
                // language=xml
                """|<Fault xmlns="http://schemas.xmlsoap.org/soap/envelope/">
                          |  <detail xmlns="">
                          |    <FooString xmlns="http://example.com/bar"/></detail>
                          |</Fault>
                          """.trimMargin(),
            ),
        )
    }

    @Test
    fun faultExampleWithDefaultIgnoredWhitespaceTypedDetails() {
        val xml = XML.v1.recommended(SerializersModule {
            polymorphic(Any::class, FooString::class, FooString.serializer())
        }) { defaultToGenericParser = true }

        val faultMessage = SoapFault(detail = FooString)

        assertEquals(
            expected = faultMessage,
            actual = xml.decodeFromString(
                SoapFault.serializer(),
                // language=xml
                """<Fault xmlns="http://schemas.xmlsoap.org/soap/envelope/">
                        |  <detail xmlns="">
                        |    <FooString xmlns="http://example.com/bar"/>
                        |  </detail>
                        |</Fault>
                        """.trimMargin(),
            ),
        )
    }

    @ConsistentCopyVisibility
    @Serializable
    @XmlSerialName("Fault", "http://schemas.xmlsoap.org/soap/envelope/")
    data class SoapFault private constructor(
        @XmlSerialName("detail", "")
        private val detailHolder: DetailHolder2? = null,
    ) {
        constructor(detail: Any) : this(detailHolder = DetailHolder2(detail))
    }

    @Serializable
    @XmlSerialName("detail", "")
    private data class DetailHolder2(
        @XmlValue
        val detail: @Polymorphic Any
    )

    @Serializable
    @XmlSerialName("FooString", "http://example.com/bar")
    object FooString
}
