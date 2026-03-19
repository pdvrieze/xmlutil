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

package org.w3.qt3tests.test

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.serialization.InputKind
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.w3.dom.nthElement
import org.w3.qt3tests.Qt3Catalog
import org.w3.qt3tests.Qt3TestSet
import org.w3.qt3tests.Qt3TestSetReference
import org.w3.qt3tests.resolved.ResolutionContext
import kotlin.test.Test

class TestParseCatalog {

    @Test
    fun testParseFnDoc() {
        val xml = XML.v1{}
        val testSet = KtXmlReader(javaClass.getResourceAsStream("/xpath/fn/doc.xml")!!).use { reader ->
            xml.decodeFromReader<Qt3TestSet>(reader)
        }

        val resolutionContext = ResolutionContextImpl.Catalog("/xpath/fn/", xml)
        context(resolutionContext) {
            val _= testSet.resolve()
        }
    }

    private val KtXmlReader.col: Int
        get() = (startLocationInfo as XmlReader.ExtLocationInfo).col

    private val KtXmlReader.line: Int
        get() = (startLocationInfo as XmlReader.ExtLocationInfo).line

    private val KtXmlReader.pos: String
        get() = "${line}:${col}"

    @Test
    fun testParseFnApply() {
        val xml = XML.v1{}
        val testSet = KtXmlReader(javaClass.getResourceAsStream("/xpath/fn/apply.xml")!!).use { reader ->
            assertEquals("1:1", reader.pos)
            assertEquals(EventType.START_DOCUMENT, reader.next())
            assertEquals("1:1", reader.pos)

            assertEquals(EventType.IGNORABLE_WHITESPACE, reader.next())
            assertEquals("1:42", reader.pos)

            assertEquals(EventType.START_ELEMENT, reader.next())
            assertEquals("2:1", reader.pos)

            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("3:4", reader.pos)

            assertEquals(EventType.TEXT, reader.next())
            assertEquals("3:17", reader.pos)

            assertEquals(EventType.END_ELEMENT, reader.next())
            assertEquals("3:43", reader.pos)

            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("4:4", reader.pos)
            assertEquals(EventType.END_ELEMENT, reader.nextTag())

            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("6:4", reader.pos)
            assertEquals(EventType.END_ELEMENT, reader.nextTag())

            assertEquals(EventType.START_ELEMENT, reader.nextTag())
            assertEquals("7:4", reader.pos)
            assertEquals(EventType.END_ELEMENT, reader.nextTag())

            var i = 0
            while (i < 7) {
                if (reader.next() == EventType.START_ELEMENT) i+=1
            }

            assertEquals("15:6", reader.pos)

            assertEquals(EventType.TEXT, reader.next())
            assertEquals("15:17", reader.pos)
        }
    }

    @Test
    fun testParseBcIsInvalid() {

        val doc = KtXmlReader(javaClass.getResourceAsStream("/xpath/fn/id/BCisInvalid.xml")!!, relaxed = true).use { reader ->
            assertEquals(EventType.START_DOCUMENT, reader.next())
            assertEquals(EventType.DOCDECL, reader.next())
            assertEquals(EventType.IGNORABLE_WHITESPACE, reader.next())
            assertEquals(EventType.START_ELEMENT, reader.next())
            assertEquals("germanÃ", reader.localName)
        }
    }



    @Test
    fun testParse() {
        val catalog = context(createResolutionContext()) {
            parseCatalogCommon().resolve()
        }

        assertEquals(13, catalog.environments.size)
        val atomicDoc = catalog.environments.first { it.name=="atomic" }.sources.single().content
        assertEquals("duration", atomicDoc.documentElement!!.nthElement(0).localName)
        assertEquals("gMonthDay", atomicDoc.documentElement!!.nthElement(6).localName)

        println(catalog)
    }

    @Test
    fun testParseAnalyzeString() {
        testParseTestSet(getTestSetSpec("fn-analyze-string"))
    }

    @Test
    fun testParseMapMerge() {
        testParseTestSet(getTestSetSpec("map-merge"))
    }

    @Test
    fun testParseFormatNumber() {
        testParseTestSet(getTestSetSpec("fn-format-number"))
    }

    @Test
    fun testParseOpExcept() {
        testParseTestSet(getTestSetSpec("op-except"))
    }

    context(ctx: ResolutionContext)
    private fun parseCatalogCommon(): Qt3Catalog {
        return ctx.parseFile(Qt3Catalog.serializer(), "catalog.xml")
    }

    @Test
    fun testParseApply() {
        val xml = XML.v1{}
        val resolutionContext = ResolutionContextImpl.Catalog("/xpath/fn/", xml)

        val testSet = context(resolutionContext) {
            resolutionContext.parseFile(org.w3.qt3tests.Qt3TestSet.serializer(), "apply.xml")
        }
        println(testSet)
    }

    @ParameterizedTest()
    @MethodSource("getTestSetSpecs")
    fun testParseTestSet(spec: TestSetSpec) {
        val _ = context(spec.resolutionContext) {
            spec.testSet.resolve()
        }
    }

/*
    @DisplayName("Test set")
    @TestFactory
    fun createTestSetTests(): List<DynamicTest> {
        val ctx = createResolutionContext("/xpath/")
        context(ctx) {
            val catalog = parseCatalogCommon()

            return catalog.testSets.map {
                val uri = File(ctx.base).toURI().resolve(URI.create(it.file.toString()))
                    .resolve("#default")

                DynamicTest.dynamicTest(it.name, uri) {
                    val _ = it.resolve()
                }
            }
        }
    }
*/

    class TestSetSpec(val resolutionContext: ResolutionContext, val testSet: Qt3TestSetReference)

    companion object {
        fun getTestSetSpec(name: String): TestSetSpec {
            return getTestSetSpecs().first {
                it.name == name
            }.payload
        }

        @JvmStatic
        fun getTestSetSpecs(): List<Named<TestSetSpec>> {
            val ctx = createResolutionContext("/xpath/")
            val catalog = ctx.parseFile(Qt3Catalog.serializer(), "catalog.xml")

            val resolvedEnvironments = context(ctx) {
                catalog.environments.map { it.resolve() }
            }
            require(ctx.knownEnvironments.isNotEmpty())

            return catalog.testSets.map {
                Named.of(it.name, TestSetSpec(ctx, it))
            }

        }

        private fun createResolutionContext(base: String = "/xpath/"): ResolutionContextImpl.Catalog {
            val xml = XML.v1 {
                policy {
                    unknownChildHandler = object : UnknownChildHandler {
                        override fun handleUnknownChildRecovering(
                            input: XmlReader,
                            inputKind: InputKind,
                            descriptor: XmlDescriptor,
                            name: QName?,
                            candidates: Collection<Any>
                        ): List<XML.ParsedData<*>> {
                            if (inputKind == InputKind.Attribute &&
                                input.eventType == EventType.START_ELEMENT &&
                                input.localName == "query" &&
                                name != null &&
                                name.isEquivalent(QName("uri"))
                            ) {
                                return emptyList()
                            }
                            return XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER.handleUnknownChildRecovering(
                                input,
                                inputKind,
                                descriptor,
                                name,
                                candidates
                            )
                        }
                    }
                }
            }
            return ResolutionContextImpl.Catalog(base, xml)
        }

    }
}

