/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.core.kxio

import kotlinx.io.Buffer
import kotlinx.io.writeString
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.internal.codepointAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val source = Buffer().apply { writeString("\ufeff<baz:SimpleData xmlns:baz='http://example.org/foo'>bar</baz:SimpleData>"); flush() }
        val r = SourceUnicodeReader(source)
        val kt = KtXmlReader(r)
        var cnt = 0
        while (kt.hasNext()) {
            val e = kt.next()
            if (e == EventType.START_ELEMENT) {
                assertEquals("http://example.org/foo", kt.namespaceURI)
                assertEquals("SimpleData", kt.localName)
                assertEquals("baz", kt.prefix)

                val decls = kt.namespaceDecls
                assertEquals(1, decls.size)
                assertEquals("baz", decls[0].prefix)
                assertEquals("http://example.org/foo", decls[0].namespaceURI)
            }
            ++cnt
        }
        assertEquals(5, cnt)
    }

    /**
     * Test that a large buffer can be read from a source. #373.
     */
    @Test
    fun testLargeUnicodeBuffer373() {
        val innerText = "<baz:SimpleData xmlns:baz='http://example.org/foo'>bar</baz:SimpleData>\n"
        val source = Buffer().apply {
            writeString("<root>\n")
            repeat(500) {
                writeString(innerText)
            }
            writeString("</root>")
            flush()
        }

        assertEquals(14+500*innerText.length, source.size.toInt())

        val buffer = CharArray(innerText.length)

        val r = SourceUnicodeReader(source)
        assertEquals(7, r.read(buffer, 0, 7), "7 characters for root")
        assertEquals("<root>\n", buffer.concatToString(0, 7), "Expected to read root")
        repeat(500) {
            buffer.fill('\u0000')
            val readCount = fullRead(r, buffer, buffer.size)
            val actualRead = when {
                readCount < 0 -> ""
                else -> buffer.concatToString(0, readCount)
            }
            assertEquals(innerText, actualRead, "Expected to read all text for iteration $it, instead read: '$actualRead'")
        }
        assertEquals(7, r.read(buffer, 0, 7))
        assertEquals("</root>", buffer.concatToString(0, 7))
        assertTrue(r.read() < 0) // end of file
    }

    private fun fullRead(reader: SourceUnicodeReader, buffer: CharArray, len: Int): Int {
        var totalRead = 0
        while (totalRead < len) {
            val read = reader.read(buffer, totalRead, buffer.size)
            if (read < 0) {
                return totalRead
            }
            totalRead += read
        }
        return totalRead
    }

    @Test
    fun testReadWithRussianCharacter() {
        val INPUT = """<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<fittings count="1225">
    <fitting name="Vargurgur 2чSmart" creationTime="1730807493413">
    </fitting>
</fittings>"""

        val source = Buffer().apply { writeString(INPUT); flush() }
        val r = SourceUnicodeReader(source)

        for (i in INPUT.indices) {
            val expected = INPUT.codepointAt(i)
            val actual = r.read()
            assertEquals(expected, actual, "Unexpected character at index $i (char: ${expected.toChar()})")
        }

    }

    @Test
    fun testReadNonAsciiCharacters_384() {
        val INPUT = "ч\u1fff🙂"

        val source = Buffer().apply { writeString(INPUT); flush() }
        val r = SourceUnicodeReader(source)

        for (i in INPUT.indices) {
            val expected = INPUT[i].code
            val actual = r.read()
            assertEquals(expected, actual, "Unexpected character at index $i (char: ${expected.toChar()})")
        }

    }

    @Test
    fun testReadNonAsciiCharactersBulk_384() {
        val INPUT = "ч\u1fff🙂"

        val source = Buffer().apply { writeString(INPUT); flush() }
        val r = SourceUnicodeReader(source)

        val outBuffer = CharArray(INPUT.length)
        val x = r.read(outBuffer, 0, INPUT.length)
        assertEquals(INPUT.length, x)
        assertEquals(INPUT, outBuffer.concatToString())

    }
}
