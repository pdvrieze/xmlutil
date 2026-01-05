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

package net.devrieze.serialization.examples.anylist

import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AnyListStringTest {
    val parser = XML.v1(anyListModule)

    @Test
    fun testSerializeAnyListWithSpace() {
        val decoded2 = parser.decodeFromString(serializer<Orders2>(), xmlData("Random string content"))
        assertEquals(4, decoded2.orders.size)

        for (elem in arrayOf(0,2,3).map { decoded2.orders[it] }) {
            assertIs<XmlEntity>(elem)
        }

        val decodedString = assertIs<String>(decoded2.orders[1])
        assertEquals("Random string content", decodedString)
    }
}
