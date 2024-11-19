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

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StringOrCompositePolymorphism253 {

    val xml = XML(ExtensionDto.module()) { recommended_0_90_2() }

    @Test
    fun testParse() {
        val result = xml.decodeFromString<List<ExtensionDto>>(SAMPLE, QName("Extensions"))
        assertEquals(2, result.size)
        assertEquals("{\"savedData\":\"\"}", assertIs<String>(result[0].value[0]).trim())
        for (e in result[1].value) {
            if(e is String) assertEquals("", e.trim())
        }
        val e2 = result[1].value.filterIsInstance<AdVerificationsDto>().single()
        assertEquals(1, e2.verifications?.size)
        assertIs<Element>(e2.verifications?.single())
    }


    @Serializable
    @SerialName("Extension")
    data class ExtensionDto(
        val source: String? = null,
        val type: String? = null,
        @XmlValue
        var value: List<@Polymorphic Any> = listOf()
    ) {
        companion object {
            fun module() = SerializersModule {
                polymorphic(Any::class) {
                    subclass(String::class)
                    subclass(AdVerificationsDto::class)
                }
            }
        }
    }

    @Serializable
    @SerialName("AdVerifications")
    data class AdVerificationsDto(
        @XmlElement(true)
        @XmlValue
        var verifications: MutableList<Element>? = mutableListOf()
    )

    val SAMPLE = """
        |<Extensions>
        |    <Extension source="mySource">
        |        <![CDATA[{"savedData":""}]]>
        |    </Extension>
        |    <Extension type="AdVerifications">
        |        <AdVerifications>
        |            <Verification vendor="Something">
        |                <JavaScriptResource apiFramework="omid" browserOptional="true">
        |                    <![CDATA[https://google.com/video.js]]>
        |                </JavaScriptResource>
        |                <VerificationParameters>
        |                    <![CDATA[{"key":"21649"}]]>
        |                </VerificationParameters>
        |            </Verification>
        |        </AdVerifications>
        |    </Extension>
        |</Extensions>
    """.trimMargin()
}
