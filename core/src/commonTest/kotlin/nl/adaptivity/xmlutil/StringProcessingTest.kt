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

package nl.adaptivity.xmlutil

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for #180, that some strings are not collapsed correctly.
 */
class StringProcessingTest {

    @Test
    fun testCollapseEmptyString() {
        val actual = xmlCollapseWhitespace("")
        assertEquals("", actual)
    }

    @Test
    fun testCollapseWS1String() {
        val actual = xmlCollapseWhitespace(" ")
        assertEquals("", actual)
    }

    @Test
    fun testCollapseWS2String() {
        val actual = xmlCollapseWhitespace("  ")
        assertEquals("", actual)
    }

    @Test
    fun testCollapseWSComplexString() {
        val actual = xmlCollapseWhitespace(" \t\n\r ")
        assertEquals("", actual)
    }

    @Test
    fun testCollapseSingleString() {
        val actual = xmlCollapseWhitespace("foo")
        assertEquals("foo", actual)
    }

    @Test
    fun testCollapseSingleWSPrefixString() {
        assertEquals("foo", xmlCollapseWhitespace(" foo"))
    }

    @Test
    fun testCollapseSingleWSSuffixString() {
        assertEquals("foo", xmlCollapseWhitespace("foo "))
    }

    @Test
    fun testCollapseSingleWSString() {
        assertEquals("foo", xmlCollapseWhitespace(" \t\nfoo \t"))
    }

    @Test
    fun testCollapseMultipleWSString() {
        assertEquals("foo bar", xmlCollapseWhitespace(" \t\nfoo\t\nbar \t"))
    }

}
