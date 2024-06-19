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
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test for handling Instant serialization when it is resolved contextually #220 */
class ContextualWithTypeSerializer220 {
    @Serializable
    data class Box(
        @Contextual
        @XmlElement val i: Instant
    )

    @Test
    fun deserializeTest() {
        val box: Box = XML.defaultInstance.decodeFromString(
            """<?xml version="1.0" encoding="UTF-8"?>
               <Box>
                 <i>2023-11-02T15:56:49.364+01:00</i>
               </Box>""".trimIndent()
        )

        val expected = Instant.fromEpochMilliseconds(1698937009364L)

        assertEquals(expected, box.i)
    }

    @Test
    fun serializeTest() {
        val expected =
            """<Box>
                 <i>2023-11-02T14:56:49.364Z</i>
               </Box>""".trimIndent()


        val actual = XML.encodeToString(Box(Instant.fromEpochMilliseconds(1698937009364L)))

        assertXmlEquals(expected, actual)
    }
}
