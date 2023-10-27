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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test for #188 */
class TestParseOrder {

    @Test
    fun testOrderedParsing() {
        val xml = XML { defaultPolicy { autoPolymorphic=true } }
        
        val data = """
            <ArrayList>
                <P id="1">11111.11111 111111.111111 -0.111111111</P>
                <P id="2">22222.22222 222222.222222 -0.222222222</P>
                <P id="3">33333.33333 333333.333333 -0.333333333</P>
                <P id="4">44444.44444 444444.444444 -0.444444444</P>
                <P id="5">55555.55555 555555.555555 -0.555555555</P>
                <P id="6">66666.66666 666666.666666 -0.666666666</P>
                <P id="7">77777.77777 777777.777777 -0.777777777</P>
                <P id="8">88888.88888 888888.888888 -0.888888888</P>
                <P id="9">99999.99999 999999.999999 -0.999999999</P>
            </ArrayList>
        """.trimIndent()

        val parsed = xml.decodeFromString(ListSerializer(LandXMLPoint.serializer()), data)
        assertEquals(9, parsed.size)
        assertEquals((1..9).toList(), parsed.map { it.id })
        assertEquals("11111.11111 111111.111111 -0.111111111", parsed[0].dataStr)
        assertEquals("22222.22222 222222.222222 -0.222222222", parsed[1].dataStr)
    }
    
    @Serializable
    @XmlSerialName("P", "", "")
    data class LandXMLPoint(
        val id: Int,
        @XmlValue(true) val dataStr: String,
    )
}
