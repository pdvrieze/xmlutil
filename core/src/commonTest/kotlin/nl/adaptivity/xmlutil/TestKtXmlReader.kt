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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import kotlin.test.Test

class TestKtXmlReader : TestCommonReader() {

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
    fun testProcessingInstructionDom() {
        val domWriter = DomWriter()
        testProcessingInstruction(::createReader) { domWriter }

        val expectedXml = """
                <?xpacket begin='' id='from_166'?>
                <a:root xmlns:a="foo" a:b="42">bar</a:root>
                <?xpacket end='w'?>
            """
        val expected = xmlStreaming.newReader(expectedXml)
        assertXmlEquals(expected, DomReader(domWriter.target))

        val fromDom = StringWriter()
        KtXmlWriter(fromDom).use { writer ->
            DomReader(domWriter.target).use { reader ->
                while(reader.hasNext()) {
                    reader.next()
                    reader.writeCurrent(writer)
                }
            }
        }
         assertXmlEquals(expectedXml, fromDom.toString())
    }

}
