/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.util.impl

import nl.adaptivity.xmlutil.*

public class FragmentNamespaceContext(
    public val parent: FragmentNamespaceContext?,
    prefixes: Array<String>,
    namespaces: Array<String>
) : IterableNamespaceContext/*(prefixes, namespaces)*/ {

    private val delegate = SimpleNamespaceContext(prefixes, namespaces)


    override fun getNamespaceURI(prefix: String): String? = when (val uri = delegate.getNamespaceURI(prefix)) {
        "" -> parent?.getNamespaceURI(prefix) ?: ""
        else -> uri
    }

    override fun getPrefix(namespaceURI: String): String {
        return delegate.getPrefix(namespaceURI) ?: parent?.getPrefix(namespaceURI) ?: ""
    }

    override fun iterator(): Iterator<Namespace> = when {
        parent == null ||
                !parent.iterator().hasNext()
        -> delegate.iterator()
        delegate.size == 0 -> parent.iterator()
        else -> (parent.iterator().asSequence() + delegate.iterator().asSequence()).iterator()
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
        if (parent == null) {
            return delegate.getPrefixesCompat(namespaceURI)
        }
        val prefixes = HashSet<String>()

        delegate.getPrefixesCompat(namespaceURI).forEach { prefixes.add(it) }

        parent.prefixesFor(namespaceURI).asSequence()
            .filter { prefix -> getLocalNamespaceUri(prefix) == null }
            .forEach { prefixes.add(it) }

        return prefixes.iterator()
    }

    private fun getLocalNamespaceUri(prefix: String): String? {
        return delegate.indices.lastOrNull { prefix == delegate.getPrefix(it) }?.let { delegate.getNamespaceURI(it) }
    }
}
