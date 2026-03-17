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

import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3.dom.nthElement
import org.w3.qt3tests.Qt3Catalog
import org.w3.qt3tests.Qt3TestSet
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Environment
import kotlin.test.Test

class TestParseCatalog {

    @Test
    fun testParseFnDoc() {
        val xml = XML.v1{}
        val testSet = KtXmlReader(javaClass.getResourceAsStream("/xpath/fn/doc.xml")!!).use { reader ->
            xml.decodeFromReader<Qt3TestSet>(reader)
        }

        val resolutionContext = ResolutionContextImpl("/xpath/fn/", xml)
        context(resolutionContext) {
            val _= testSet.resolve()
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
        val xml = XML.v1{
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
                        return XmlConfig.DEFAULT_UNKNOWN_CHILD_HANDLER.handleUnknownChildRecovering(input, inputKind, descriptor, name, candidates)
                    }
                }
            }
        }
        val resolutionContext = ResolutionContextImpl("/xpath/", xml)

        val catalog = context(resolutionContext) {
            resolutionContext.parseFile(Qt3Catalog.serializer(), "catalog.xml").resolve()
        }

        assertEquals(13, catalog.environments.size)
        val atomicDoc = catalog.environments.first { it.name=="atomic" }.sources.single().content
        assertEquals("duration", atomicDoc.documentElement!!.nthElement(0).localName)
        assertEquals("gMonthDay", atomicDoc.documentElement!!.nthElement(6).localName)

        println(catalog)
    }

    @Test
    fun testParseApply() {
        val xml = XML.v1{}
        val resolutionContext = ResolutionContextImpl("/xpath/fn/", xml)

        val testSet = context(resolutionContext) {
            resolutionContext.parseFile(org.w3.qt3tests.Qt3TestSet.serializer(), "apply.xml")
        }
        println(testSet)
    }
}

class ResolutionContextImpl(
    override val base: String,
    override val xml: XML,
    override val knownEnvironments: MutableMap<String, ResolvedQt3Environment> = HashMap(),
    override val idMap: MutableMap<String, Any> = HashMap(),
): ResolutionContext {

    override fun subContext(file: String): ResolutionContext {
        val i = file.lastIndexOf('/')
        val newBase = when {
            i < 0 -> return this
            else -> "$base${file.substring(0, i + 1)}"
        }
        // copy the maps to make names hierarchical (not ordered global across files)
        return ResolutionContextImpl(newBase, xml, HashMap(knownEnvironments), HashMap(idMap))
    }


    override fun parseDocument(relativePath: String): Document {
        val out = xmlStreaming.newWriter()

            requireNotNull(javaClass.getResourceAsStream("$base$relativePath")){
                "Could not find resource $base$relativePath"
            }.use {
                val xr = KtXmlReader(it, relaxed = true)
                try {
                    while (xr.hasNext()) {
                        val _ = xr.next()
                        if (!xr.isIgnorable()) xr.writeCurrent(out)
                    }
                } catch (e: XmlSerialException) {
                    if (e.extLocationInfo == null) {
                        throw XmlSerialException(e.rawMessage!!, xr.extLocationInfo, e)
                            .also { it.setFileLocation("$base$relativePath") }
                    } else {
                        e.setFileLocation("$base$relativePath")
                        throw e
                    }
                } catch (e: XmlException) {
                    if (e.locationInfo == null) {
                        throw XmlSerialException(e.rawMessage!!, xr.extLocationInfo, e)
                            .also { it.setFileLocation("$base$relativePath") }
                    } else {
                        e.setFileLocation("$base$relativePath")
                        throw e
                    }
                } catch (e: Exception) {
                    throw XmlException(xr.extLocationInfo, e)
                }
            }
            return out.target
    }

    override fun <T> parseFile(
        deserializer: DeserializationStrategy<T>,
        relativePath: String
    ): T {
        System.getLogger("testing").log(System.Logger.Level.INFO, "Parsing $relativePath")
        try {

            return javaClass.getResourceAsStream("$base$relativePath")!!.use {
                val xr = KtXmlReader(it, relaxed = true)
                xml.decodeFromReader(deserializer, xr)
            }
        } catch (e: XmlSerialException) {
            e.setFileLocation("$base$relativePath")
            throw e
        }
    }
}
