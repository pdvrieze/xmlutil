/*
 * Copyright (c) 2021.
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

import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import kotlin.test.Test

class TestXmlReader : TestCommonReader() {
    
    private fun createReader(it: String): XmlReader = xmlStreaming.newReader(it)

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
    fun testReadEntity() {
        testReadEntity(::createReader)
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
    fun testProcessingInstructionDom() {
        testProcessingInstruction(::createReader) { DomWriter() }
    }

    @Test
    fun testReadToDom() {
        testReadToDom(::createReader)
    }

}
