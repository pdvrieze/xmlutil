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

package nl.adaptivity.xmlutil.test.regressions

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import kotlin.test.Test

/**
 * Test for issue #394
 */
class TestEolAtBufferBoundary394 {


    fun readForTesting(stream: KtXmlReader) {
        while (stream.hasNext()) {
            val _ = stream.next()
        }
    }

    fun readForTesting(str: String) {
        readForTesting(KtXmlReader(StringReader(str)))
        readForTesting(KtXmlReader(str))
    }

    @Test
    fun testLFStreamRight() = readForTesting(getData2("\n", 1))

    @Test
    fun testCRStreamRight() = readForTesting(getData2("\r", 1))

    @Test
    fun testNELStreamRight() = readForTesting(getData2("\u0085", 1))

    @Test
    fun testLSStreamRight() = readForTesting(getData2("\u2028", 1))

    @Test
    fun testCRLFStreamRight() = readForTesting(getData2("\r\n", 1))

    @Test
    fun testCRNELStreamRight() = readForTesting(getData2("\r\u0085", 1))

    @Test
    fun testLFStreamLeft() = readForTesting(getData2("\n", 0))

    @Test
    fun testCRStreamLeft() = readForTesting(getData2("\r", 0))

    @Test
    fun testNELStreamLeft() = readForTesting(getData2("\u0085", 0))

    @Test
    fun testLSStreamLeft() = readForTesting(getData2("\u2028", 0))

    @Test
    fun testCRLFStreamLeft() = readForTesting(getData2("\r\n", -1))

    @Test
    fun testCRNELStreamLeft() = readForTesting(getData2("\r\u0085", -1))

    @Test
    fun testCRLFStreamMid() = readForTesting(getData2("\r\n"))

    @Test
    fun testCRNELStreamMid() = readForTesting(getData2("\r\u0085"))

    @Test
    fun readMbStreamMid() {
        readForTesting(KtXmlReader(StringReader(dataMbMid)))
    }

    @Test
    fun readMbStreamStringMid() {
        readForTesting(KtXmlReader(StringReader(dataMbMid)))
    }

    @Test
    fun readMbStreamLeft() {
        readForTesting(KtXmlReader(StringReader(dataMbLeft)))
    }

    @Test
    fun readMbStreamStringLeft() {
        readForTesting(KtXmlReader(StringReader(dataMbLeft)))
    }

    @Test
    fun readMbStreamRight() {
        readForTesting(KtXmlReader(StringReader(dataMbRight)))
    }

    @Test
    fun readMbStreamStringRight() {
        readForTesting(KtXmlReader(StringReader(dataMbRight)))
    }

    companion object {
        private fun getData2(eol: String, offset: Int = 0): String {
            return "<r><x>" + "y".repeat(4086 + offset - eol.length) + "</x>$eol</r>"
        }

        private val dataMbMid get() = " ".repeat(4093)+"<x>"+"_".repeat(4095)+"\r\n"+"_".repeat(32)+"</x>"
        private val dataMbLeft get() = " ".repeat(4093)+"<x>"+"_".repeat(4094)+"\r\n"+"_".repeat(32)+"</x>"
        private val dataMbRight get() = " ".repeat(4093)+"<x>"+"_".repeat(4096)+"\r\n"+"_".repeat(32)+"</x>"
//        private val data = "<x>"+"y".repeat(4083)+"\r"+"_".repeat(32)+"</x>"

    }

}
