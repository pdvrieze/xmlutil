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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test

class TestCachedDescriptorCache {

    @Test
    fun testParameterisedSerializerCache() {
        val format = XML {}

        val serialized1 = format.encodeToString(Outer(Inner1(1, 2)))
        assertXmlEquals("<Outer><Inner1 data1=\"1\" data2=\"2\"/></Outer>", serialized1)

        val serialized2 = format.encodeToString(Outer(Inner2("a", "b", "c")))
        assertXmlEquals("<Outer><Inner2 data3=\"a\" data4=\"b\" data5=\"c\"/></Outer>", serialized2)
    }

    @Serializable
    data class Outer<T>(val data: T)

    @Serializable
    data class Inner1(val data1: Int, val data2: Int)

    @Serializable
    data class Inner2(val data3: String, val data4: String, val data5: String)
}
