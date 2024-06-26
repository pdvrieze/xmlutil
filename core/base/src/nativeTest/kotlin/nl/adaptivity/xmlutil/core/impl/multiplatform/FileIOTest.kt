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

package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*

@OptIn(ExperimentalForeignApi::class)
class FileIOTest {

    private var _testFile: FilePtr? = null
    private val testFile: FilePtr get() = _testFile!!
    private lateinit var writer: OutputStreamWriter

    @BeforeTest
    fun createTestTmpFile() {
        memScoped {
            val fileName: CPointer<ByteVar> = "FILEIOTEST.XXXXXX".cstr.ptr
            val fileDescriptor = mkstemp(fileName).takeIf { it >= 0 } ?: throw IOException.fromErrno()
            unlink(fileName.toKString())
            _testFile = FilePtr(fdopen(fileDescriptor, "w+") ?: throw IOException.fromErrno())
            writer = OutputStreamWriter(FileOutputStream(testFile))

        }
    }

    @AfterTest
    fun closeFileAfterTest() {
        fclose(testFile.value)
        _testFile = null
    }

    @Test
    fun testAppend() {
        writer.append('5')
        rewind(testFile.value)
        memScoped {
            val readBuffer = allocArray<UByteVar>(10)
            val bytesRead = fread(readBuffer.getPointer(this), 1u, 10u, testFile.value)
            assertEquals(1u, bytesRead)
            assertEquals('5'.code, readBuffer[0].toInt())
        }
    }

    @Test
    fun write() {
        val text = "Hello world!\uD83D\uDE00 Tada!!"
        writer.apply {
            write(text)
            flush()
        }

        rewind(testFile.value)


        val actualLines = InputStreamReader(FileInputStream(testFile)).lines().toList()

        assertEquals(1, actualLines.size)
        assertEquals(text, actualLines.single())
    }

    @Test
    fun testMultiLine() {
        val lines = listOf(
            "Some text on \uD83D\uDE1C",
            "Yet another line!! "
        )
        writer.apply {
            lines.forEach(::appendLine)
            flush()
        }
        rewind(testFile.value)

        val actualLines = InputStreamReader(FileInputStream(testFile)).lines().toList()
        assertContentEquals(lines, actualLines)
    }
}

