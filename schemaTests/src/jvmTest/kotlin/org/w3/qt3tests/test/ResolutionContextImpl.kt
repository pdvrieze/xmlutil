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

import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmDOMImplementation
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmDocument
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialException
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming
import org.w3.qt3tests.Qt3Dependency
import org.w3.qt3tests.resolved.CatalogResolutionContext
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Environment
import org.w3.qt3tests.resolved.TestSetResolutionContext

@OptIn(XPathInternal::class)
abstract class ResolutionContextImpl(
    override val base: String,
    override val xml: XML,
    override val knownEnvironments: MutableMap<String, ResolvedQt3Environment> = HashMap(),
    override val idMap: MutableMap<String, Any> = HashMap(),
    val doVerify: Boolean,
): ResolutionContext {

    override fun subContext(file: String): CatalogResolutionContext {
        val i = file.lastIndexOf('/')
        val newBase = when {
            i < 0 -> return this as? CatalogResolutionContext ?: CatalogContext(
                base,
                xml,
                knownEnvironments,
                idMap,
                doVerify
            )
            else -> "$base${file.substring(0, i + 1)}"
        }
        // copy the maps to make names hierarchical (not ordered global across files)
        return CatalogContext(newBase, xml, HashMap(knownEnvironments), HashMap(idMap), doVerify)
    }


    override fun parseDocument(relativePath: String): XdmDocument {
        val doc = XdmDOMImplementation.createDocument(null, null, null)
        val out = xmlStreaming.newWriter(doc)

        requireNotNull(javaClass.getResourceAsStream("$base$relativePath")) {
            "Could not find resource $base$relativePath"
        }.use {
            val xr = KtXmlReader(it, relaxed = true)
            try {
                while (xr.hasNext()) {
                    val _ = xr.next()
                    xr.writeCurrent(out)
                }
            } catch (e: XmlSerialException) {
                if (e.extLocationInfo == null) {
                    throw XmlSerialException(e.rawMessage!!, xr.extLocationInfo, "($base$relativePath)", e)
                        .also { it.setFileLocation("$base$relativePath") }
                } else {
                    e.setFileLocation("$base$relativePath")
                    throw e
                }
            } catch (e: XmlException) {
                if (e.locationInfo == null) {
                    throw XmlSerialException(e.rawMessage!!, xr.extLocationInfo, "($base$relativePath)", e)
                        .also { it.setFileLocation("$base$relativePath") }
                } else {
                    e.setFileLocation("$base$relativePath")
                    throw e
                }
            } catch (e: Exception) {
                throw XmlException(xr.extLocationInfo, e)
            }
        }
        return doc
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

    class CatalogContext(
        base: String,
        xml: XML,
        knownEnvironments: MutableMap<String, ResolvedQt3Environment> = HashMap(),
        idMap: MutableMap<String, Any> = HashMap(),
        doVerify: Boolean = true
    ) : ResolutionContextImpl(base, xml, knownEnvironments, idMap, doVerify), CatalogResolutionContext {



        override fun testSetContext(dependencies: List<Qt3Dependency>): TestSetResolutionContext {
            return TestSetContext(base, xml, knownEnvironments, idMap, dependencies, doVerify)
        }
    }

    class TestSetContext(
        base: String,
        xml: XML,
        knownEnvironments: MutableMap<String, ResolvedQt3Environment> = HashMap(),
        idMap: MutableMap<String, Any> = HashMap(),
        override val setDependencies: List<Qt3Dependency>,
        doVerify: Boolean
    ) : ResolutionContextImpl(base, xml, knownEnvironments, idMap, doVerify), TestSetResolutionContext
}
