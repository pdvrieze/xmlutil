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
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.isIgnorable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialException
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3.dom.nthElement
import org.w3.qt3tests.Qt3Catalog
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Environment
import kotlin.test.Test

class TestParseCatalog {

    @Test
    fun testParse() {
        val xml = XML.v1{}
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
        return ResolutionContextImpl(newBase, xml, knownEnvironments, idMap)
    }

    override fun parseDocument(relativePath: String): Document {
        val out = xmlStreaming.newWriter()

        try {
            requireNotNull(javaClass.getResourceAsStream("$base$relativePath")){
                "Could not find resource $base$relativePath"
            }.use {
                val xr = KtXmlReader(it, relaxed = true)
                while (xr.hasNext()) {
                    val _ = xr.next()
                    if (!xr.isIgnorable()) xr.writeCurrent(out)
                }
            }
            return out.target
        } catch (e: XmlSerialException) {
            throw e.withFileName("$base$relativePath")
        }
    }

    override fun <T> parseFile(
        deserializer: DeserializationStrategy<T>,
        relativePath: String
    ): T {
        try {

            return javaClass.getResourceAsStream("$base$relativePath")!!.use {
                val xr = KtXmlReader(it, relaxed = true)
                xml.decodeFromReader(deserializer, xr)
            }
        } catch (e: XmlSerialException) {
            throw e.withFileName("$base$relativePath")
        }
    }
}
