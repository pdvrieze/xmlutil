/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.test

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.test.sunExpected.AGAttrUseDefaults
import io.github.pdvrieze.formats.xmlschema.test.sunExpected.AGAttrWCardDefaults
import io.github.pdvrieze.formats.xmlschema.test.sunExpected.AGNameDefaults
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestSunData {
    @Nested
    inner class AGroupDef {
        @Nested
        inner class AGAttrUse: ResourceTestBase("xsts/sunData/AGroupDef/AG_attrUse/AG_attrUseNS00101m/") {

            @Test
            fun testXmlDescriptorToString() {
                val desc = format.xmlDescriptor(XSSchema.serializer())
                assertNotNull(desc.toString())
            }

            @Test
            fun testDeserializeValid() {
                assertEquals(AGAttrUseDefaults.expectedSchema, deserializeXsd("AG_attrUseNS00101m1_p.xsd"))
            }

            @Test
            fun testSerializeValid() {
                assertXmlEquals(
                    readResourceAsString("AG_attrUseNS00101m1_serialform.xsd"),
                    format.encodeToString(XSSchema.serializer(), AGAttrUseDefaults.expectedSchema)
                )
            }
        }

        @Nested
        inner class AGAttrWCard: ResourceTestBase("xsts/sunData/AGroupDef/AG_attrWCard/AG_attrWCard00101m/") {

            @Test
            fun testDeserializeValid() {
                assertEquals(AGAttrWCardDefaults.expectedSchema, deserializeXsd("AG_attrWCard00101m1.xsd"))
            }

            @Test
            fun testSerializeValid() {
                assertXmlEquals(
                    readResourceAsString("AG_attrWCard00101m1.xsd"),
                    format.encodeToString(XSSchema.serializer(), AGAttrWCardDefaults.expectedSchema, "xsd")
                )
            }

        }

        @Nested
        inner class AGName: ResourceTestBase("xsts/sunData/AGroupDef/AG_name/AG_name00101m/") {

            @Test
            fun testDeserializeValid() {
                assertEquals(AGNameDefaults.expectedSchema, deserializeXsd("AG_name00101m1_p.xsd"))
            }

            @Test
            fun testSerializeValid() {
                assertXmlEquals(
                    readResourceAsString("AG_name00101m1_p.xsd"),
                    format.encodeToString(XSSchema.serializer(), AGNameDefaults.expectedSchema, "xsd")
                )
            }

        }
    }
}
