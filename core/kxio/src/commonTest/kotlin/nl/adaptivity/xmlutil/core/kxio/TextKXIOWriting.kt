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

package nl.adaptivity.xmlutil.core.kxio

import kotlinx.io.Buffer
import kotlinx.io.readString
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertEquals

class TextKXIOWriting {
    @Test
    fun testWriteGenericSmartTag() {
        val expected = "<a xmlns=\"ns/a\"><b xmlns=\"ns/b\"><c xmlns=\"\" val=\"value\"/></b></a>"
        val sink: Buffer = Buffer()

        xmlStreaming.newGenericWriter(sink).use { out ->
            out.smartStartTag("ns/a", "a", "") {
                smartStartTag("ns/b", "b", "") {
                    smartStartTag("", "c", "")
                    attribute("", "val", "", "value")
                    endTag("", "c", "")
                }
            }
        }
        sink.flush()

        assertEquals(expected, sink.readString().replace(" />", "/>"))
    }

}
