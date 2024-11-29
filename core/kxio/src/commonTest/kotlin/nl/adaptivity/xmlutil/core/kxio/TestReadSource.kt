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
import kotlinx.io.writeString
import nl.adaptivity.xmlutil.core.KtXmlReader
import kotlin.test.Test
import kotlin.test.assertEquals

class TestReadSource {
    @Test
    fun readAllText() {
        val expected = "ajfkldfjaskvoock"
        val input = Buffer().apply { writeString(expected) }
        val inputReader = SourceUnicodeReader(input)
        val actual = buildString {
            var i = inputReader.read()
            while (i>=0) {
                append(i.toChar())
                i = inputReader.read()
            }
        }
        assertEquals(expected, actual)
    }

    @Test
    fun testKtXmlReaderFromBuffer() {
        val source = Buffer().apply { writeString("<SimpleData>bar</SimpleData>"); flush() }
        val r = SourceUnicodeReader(source)
        val kt = KtXmlReader(r)
        var cnt = 0
        while (kt.hasNext()) {
            kt.next()
            ++cnt
        }
        assertEquals(5, cnt)
    }
}
