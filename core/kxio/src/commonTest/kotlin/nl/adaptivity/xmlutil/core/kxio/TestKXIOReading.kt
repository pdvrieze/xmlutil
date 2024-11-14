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

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.test.TestCommonReader
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test

class TestKXIOReading: TestCommonReader() {

    private fun createReader(it: String): XmlReader = xmlStreaming.newGenericReader(it)

    @Test
    fun testReadCompactFragmentWithNamespaceInOuter() {
        testReadCompactFragmentWithNamespaceInOuter(::createReader)
    }

    @Test
    fun testNamespaceDecls() {
        testNamespaceDecls(::createReader)
    }

    @Test
    fun testReadCompactFragment() {
        testReadCompactFragment(::createReader)
    }

    @Test
    fun testReadSingleTag() {
        testReadSingleTag(::createReader)
    }

    @Test
    fun testGenericReadEntity() {
        testReadEntity(::createReader)
    }

    @Test
    fun testReadUnknownEntity() {
        testReadUnknownEntity(::createReader)
    }

    @Test
    fun testIgnorableWhitespace() {
        testIgnorableWhitespace(::createReader)
    }

    @Test
    fun testReaderWithBOM() {
        testReaderWithBOM(::createReader)
    }

    @Test
    fun testProcessingInstruction() {
        testProcessingInstruction(::createReader) { KtXmlWriter(StringWriter()) }
    }

    @Test
    fun testReadEntity() {
        testReadEntity(::createReader)
    }

    @Test
    fun testReadToDom() {
        testReadToDom(::createReader)
    }


}
