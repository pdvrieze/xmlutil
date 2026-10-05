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
class TestMultiByteEolAtBufferBoundary394 {


    fun readForTesting(stream: KtXmlReader) {
        while (stream.hasNext()) {
            stream.next()
        }
    }

    @Test
    fun readStream() {
        readForTesting(KtXmlReader(StringReader(data)))
    }

    @Test
    fun readStreamString() {
        readForTesting(KtXmlReader(StringReader(data)))
    }

    companion object {
        private val data = " ".repeat(4093)+"<x>"+"_".repeat(4095)+"\r\n"+"_".repeat(32)+"</x>"
//        private val data = "<x>"+"y".repeat(4083)+"\r"+"_".repeat(32)+"</x>"

    }

}
