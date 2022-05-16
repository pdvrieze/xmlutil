/*
 * Copyright (c) 2022.
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

import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestStringReader {

    @Test
    fun testStringReaderReadSingle() {
        val string = CharArray(1000) { Random.nextInt(32, 0x7fff).toChar() }.concatToString()
        val reader = StringReader(string)
        var currentReadCount = 0
        while (true) {
            val ch = reader.read()
            if (ch < 0) break
            assertEquals(
                string[currentReadCount].code.toString(16),
                ch.toString(16),
                "Character at pos $currentReadCount is expected to be read"
            )
            currentReadCount++
        }
    }

    @Test
    fun testStringReaderReadBuffer() {
        val string = CharArray(30) { Random.nextInt(32, 0x7fff).toChar() }.concatToString()
        assertEquals(30, string.length)
        val buffer = CharArray(20)
        val reader = StringReader(string)
        var currentReadCount = 0
        while (true) {
            val bufferContentSize = reader.read(buffer, 0, buffer.size)
            if (bufferContentSize < 0) break
            for (j in 0 until bufferContentSize) {
                assertEquals(
                    string[currentReadCount + j].code.toString(16),
                    buffer[j].code.toString(16),
                    "Character at pos $currentReadCount is expected to be read"
                )
            }
            currentReadCount += bufferContentSize
        }
        assertEquals(string.length, currentReadCount)
    }

    @Test
    fun testStringReaderReadBufferPartial() {
        val string = CharArray(1000) { Random.nextInt(32, 0x7fff).toChar() }.concatToString()
        val buffer = CharArray(30)
        val reader = StringReader(string)
        var currentReadCount = 0
        while (true) {
            val bufferContentSize = reader.read(buffer, 0, 15)
            assertTrue(bufferContentSize <= 15)
            assertTrue(buffer.drop(15).all { it.code == 0 }, "The first remaining bytes after 15 are expected to remain 0")
            if (bufferContentSize < 0) break
            for (j in 0 until bufferContentSize) {
                assertEquals(
                    string[currentReadCount + j].code.toString(16),
                    buffer[j].code.toString(16),
                    "Character at pos $currentReadCount is expected to be read"
                )
            }
            currentReadCount += bufferContentSize
        }
        assertEquals(string.length, currentReadCount)
    }

    @Test
    fun testStringReaderReadBufferOffset() {
        val offset = 13

        val string = CharArray(1000) { Random.nextInt(32, 0x7fff).toChar() }.concatToString()
        var buffer = CharArray(30)
        val reader = StringReader(string)
        var currentReadCount = 0
        while (true) {
            val bufferContentSize = reader.read(buffer, offset, buffer.size - offset)
            assertTrue(buffer.take(offset).all { it.code == 0 }, "The first $offset bytes are expected to remain 0")
            if (bufferContentSize < 0) break
            for (j in 0 until bufferContentSize) {
                assertEquals(
                    string[currentReadCount + j].code.toString(16),
                    buffer[offset+j].code.toString(16),
                    "Character at pos $currentReadCount is expected to be read"
                )
            }
            currentReadCount += bufferContentSize
        }
        assertEquals(string.length, currentReadCount)
    }


}
