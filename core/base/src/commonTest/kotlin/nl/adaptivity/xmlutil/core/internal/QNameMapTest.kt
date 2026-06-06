/*
 * Copyright (c) 2026.
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

package nl.adaptivity.xmlutil.core.internal

import nl.adaptivity.xmlutil.QName
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class QNameMapTest {

    private lateinit var defaultMap: QNameMap<String>

    @BeforeTest
    fun init() {
        defaultMap = QNameMap<String>().apply {
            put("ns1", "local1", "val1")
            put("ns1", "local2", "val2")
            put("ns2", "local1", "val3")
        }

    }

    @Test
    fun testPutItem() {
        val map = QNameMap<String>()
        map.put("urn:xxxx", "local", "value")
        assertEquals(1, map.size)
    }

    @Test
    fun testSize() {
        assertEquals(3, defaultMap.size)
    }


    @Test
    fun testIterateEntries() {
        val iterated = defaultMap.entries.toList()
        assertEquals(3, iterated.size)

        assertEquals(QName("ns1", "local1"), iterated[0].key)
        assertEquals("val1", iterated[0].value)

        assertEquals(QName("ns1", "local2"), iterated[1].key)
        assertEquals("val2", iterated[1].value)

        assertEquals(QName("ns2", "local1"), iterated[2].key)
        assertEquals("val3", iterated[2].value)
    }

    @Test
    fun testIterateKeys() {
        val iterated = defaultMap.keys.toList()
        assertEquals(3, iterated.size)

        assertEquals(QName("ns1", "local1"), iterated[0])

        assertEquals(QName("ns1", "local2"), iterated[1])

        assertEquals(QName("ns2", "local1"), iterated[2])
    }

    @Test
    fun testRemoveNS() {
        defaultMap.remove("ns2", "local1")
        assertEquals(2, defaultMap.size)
        assertFalse(defaultMap.containsKey("ns2", "local1"))

        assertEquals(
            listOf(QName("ns1", "local1"), QName("ns1", "local2")),
            defaultMap.keys.sortedBy { it.getLocalPart() })
    }

    @Test
    fun entriesAddOnlyIncrementsSizeOnce() {
        val map = QNameMap<String>()

        map.entries.add(object : MutableMap.MutableEntry<QName, String> {
            override val key: QName = QName("urn:test", "value")
            override val value: String = "payload"
            override fun setValue(newValue: String): String = error("unused")
        })

        assertEquals(1, map.size)
        assertEquals("payload", map[QName("urn:test", "value")])
    }

    @Test
    fun containsValueHandlesMultipleEntriesInSameNamespace() {
        val map = QNameMap<String>()
        map[QName("urn:test", "first")] = "one"
        map[QName("urn:test", "second")] = "two"

        assertFalse(map.containsValue("missing"))
    }

}
