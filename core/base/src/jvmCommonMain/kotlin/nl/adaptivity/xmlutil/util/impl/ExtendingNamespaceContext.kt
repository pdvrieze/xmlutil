/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil.util.impl

import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlEvent

internal class ExtendingNamespaceContext(val parent: NamespaceContext = SimpleNamespaceContext("", "")) :
    NamespaceContext {

    private val localNamespaces = mutableListOf<Namespace>()

    override fun getNamespaceURI(prefix: String): String? {
        return localNamespaces.firstOrNull { it.prefix == prefix }?.namespaceURI ?: parent.getNamespaceURI(prefix)
    }

    override fun getPrefix(namespaceURI: String): String? {
        return localNamespaces.firstOrNull { it.namespaceURI == namespaceURI }?.prefix ?: parent.getPrefix(namespaceURI)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixes(namespaceURI: String): Iterator<String?> {
        return buildSet {
            localNamespaces.asSequence()
                .filter { it.namespaceURI == namespaceURI }
                .mapTo(this) { it.prefix }
            parent.getPrefixes(namespaceURI).forEach { add(it) }
        }.iterator()
    }

    fun addNamespace(prefix: String, namespaceURI: String) {
        localNamespaces.add(XmlEvent.NamespaceImpl(prefix, namespaceURI))
    }

    fun extend() = ExtendingNamespaceContext(this)
}
