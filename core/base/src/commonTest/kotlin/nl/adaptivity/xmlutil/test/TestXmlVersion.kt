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

package nl.adaptivity.xmlutil.test

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.core.XmlVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [nl.adaptivity.xmlutil.core.XmlVersion].
 */
@OptIn(ExperimentalXmlUtilApi::class)
class TestXmlVersion {

    @Test
    fun testFromStringOrNullReturns10For10() {
        assertEquals(XmlVersion.XML10, XmlVersion.fromStringOrNull("1.0"))
    }

    @Test
    fun testFromStringOrNullReturns11For11() {
        assertEquals(XmlVersion.XML11, XmlVersion.fromStringOrNull("1.1"))
    }

    @Test
    fun testFromStringOrNullReturnsNullForUnknown() {
        assertNull(XmlVersion.fromStringOrNull("2.0"))
        assertNull(XmlVersion.fromStringOrNull(""))
        assertNull(XmlVersion.fromStringOrNull("1"))
    }

    @Test
    fun testVersionStringValues() {
        assertEquals("1.0", XmlVersion.XML10.versionString)
        assertEquals("1.1", XmlVersion.XML11.versionString)
    }
}
