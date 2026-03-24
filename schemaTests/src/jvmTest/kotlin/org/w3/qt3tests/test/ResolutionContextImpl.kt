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
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialException
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming
import org.w3.qt3tests.Qt3Dependency
import org.w3.qt3tests.resolved.CatalogResolutionContext
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Environment
import org.w3.qt3tests.resolved.TestSetResolutionContext

abstract class ResolutionContextImpl(
    override val base: String,
    override val xml: XML,
    override val knownEnvironments: MutableMap<String, ResolvedQt3Environment> = HashMap(),
    override val idMap: MutableMap<String, Any> = HashMap(),
): ResolutionContext {

    override fun subContext(file: String): CatalogResolutionContext {
        val i = file.lastIndexOf('/')
        val newBase = when {
            i < 0 -> return this as? CatalogResolutionContext ?: Catalog(base, xml, knownEnvironments, idMap)
            else -> "$base${file.substring(0, i + 1)}"
        }
        // copy the maps to make names hierarchical (not ordered global across files)
        return Catalog(newBase, xml, HashMap(knownEnvironments), HashMap(idMap))
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
                        xr.writeCurrent(out)
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

    class Catalog(
        base: String,
        xml: XML,
        knownEnvironments: MutableMap<String, ResolvedQt3Environment> = HashMap(),
        idMap: MutableMap<String, Any> = HashMap()
    ) : ResolutionContextImpl(base, xml, knownEnvironments, idMap), CatalogResolutionContext {



        override fun testSetContext(dependencies: List<Qt3Dependency>): TestSetResolutionContext {
            return TestSet(base, xml, knownEnvironments, idMap, dependencies)
        }
    }

    class TestSet(
        base: String,
        xml: XML,
        knownEnvironments: MutableMap<String, ResolvedQt3Environment> = HashMap(),
        idMap: MutableMap<String, Any> = HashMap(),
        override val setDependencies: List<Qt3Dependency>
    ) : ResolutionContextImpl(base, xml, knownEnvironments, idMap), TestSetResolutionContext
}
