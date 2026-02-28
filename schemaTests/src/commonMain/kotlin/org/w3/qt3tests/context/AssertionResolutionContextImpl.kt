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

import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.serialization.XML
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Environment

class AssertionResolutionContextImpl(private val orig: ResolutionContext, val environment: ResolvedQt3Environment?) :
    ResolutionContext, AssertionResolutionContext {
    override val base: String get() = orig.base

    override val xml: XML get() = orig.xml

    override val knownEnvironments: MutableMap<String, ResolvedQt3Environment> = when (val n = environment?.name) {
        null -> mutableMapOf()
        else -> mutableMapOf(n to environment)
    }

    override val idMap: MutableMap<String, Any> = mutableMapOf()

    override fun parseDocument(relativePath: String): Document {
        return orig.parseDocument(relativePath)
    }

    override fun <T> parseFile(
        deserializer: DeserializationStrategy<T>,
        relativePath: String
    ): T {
        return orig.parseFile(deserializer, relativePath)
    }

    override fun subContext(file: String): ResolutionContext {
        return orig.subContext(file)
    }

    override val namespaceContext: NamespaceContext = object : NamespaceContext {
        override fun getNamespaceURI(prefix: String): String? {
            return environment?.namespaces?.firstOrNull { it.prefix == prefix }?.uri?.value
        }

        override fun getPrefix(namespaceURI: String): String? {
            return environment?.namespaces?.firstOrNull { it.uri.value == namespaceURI }?.prefix
        }

        override fun getPrefixes(namespaceURI: String): Iterator<String> {
            return environment?.namespaces?.filter { it.uri.value == namespaceURI }?.map { it.prefix }?.iterator()
                ?: emptyList<String>().iterator()
        }

    }

}
