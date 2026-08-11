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

package org.w3.qt3tests.context

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.XQueryVersion
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmDocument
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.XML
import org.w3.qt3tests.Qt3SpecDependency
import org.w3.qt3tests.resolved.CatalogResolutionContext
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Environment
import org.w3.qt3tests.resolved.TestSetResolutionContext

@OptIn(XPathInternal::class)
class AssertionResolutionContextImpl(
    private val orig: ResolutionContext,
    override val environment: ResolvedQt3Environment?,
    override val specDep: Qt3SpecDependency?,
    override val doVerify: Boolean
) : AssertionResolutionContext {

    constructor(orig: TestSetResolutionContext, environment: ResolvedQt3Environment?, specDep: Qt3SpecDependency?) :
        this(orig, environment, specDep, orig.doVerify)


    override val base: String get() = orig.base

    override val xml: XML get() = orig.xml

    override val knownEnvironments: MutableMap<String, ResolvedQt3Environment> = when (val n = environment?.name) {
        null -> mutableMapOf()
        else -> mutableMapOf(n to environment)
    }

    override val idMap: MutableMap<String, Any> = mutableMapOf()

    override val minRequiredXPath: XPathVersion? =
        specDep.let {
            when (it) {
                null -> XPathVersion.XPath3_1 // Consider XPath 1.0
                else -> it.supportedXPath().minByOrNull { it.ordinal }
            }
    }

    override val minRequiredXQuery: XQueryVersion? = specDep?.run {
        supportedXQuery().minByOrNull { it.ordinal }
    }


    override fun parseDocument(relativePath: String): XdmDocument {
        return orig.parseDocument(relativePath)
    }

    override fun <T> parseFile(
        deserializer: DeserializationStrategy<T>,
        relativePath: String
    ): T {
        return orig.parseFile(deserializer, relativePath)
    }

    override fun subContext(file: String): CatalogResolutionContext {
        return orig.subContext(file)
    }

    override val namespaceContext: NamespaceContext = object : NamespaceContext {
        override fun getNamespaceURI(prefix: String): String? {
            return when (prefix) {
                "xml" -> XMLConstants.XML_NS_URI
                "xmlns" -> XMLConstants.XMLNS_ATTRIBUTE_NS_URI
                else -> environment?.namespaces?.firstOrNull { it.prefix == prefix }?.uri?.value
            }
        }

        override fun getPrefix(namespaceURI: String): String? {
            return when (namespaceURI) {
                XMLConstants.XML_NS_URI -> "xml"
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> "xmlns"
                else -> environment?.namespaces?.firstOrNull { it.uri.value == namespaceURI }?.prefix
            }
        }

        override fun getPrefixes(namespaceURI: String): Iterator<String> {
            return when (namespaceURI) {
                XMLConstants.XML_NS_URI -> listOf("xml").iterator()
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> listOf("xmlns").iterator()
                else -> environment?.namespaces?.filter { it.uri.value == namespaceURI }?.map { it.prefix }?.iterator()
                ?: emptyList<String>().iterator()
            }
        }

    }

}
