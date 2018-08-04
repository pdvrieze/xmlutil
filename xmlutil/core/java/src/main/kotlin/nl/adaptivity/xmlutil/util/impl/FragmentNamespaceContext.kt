/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.util.impl

import nl.adaptivity.xmlutil.SimpleNamespaceContext
import java.util.HashSet

class FragmentNamespaceContext(val parent: FragmentNamespaceContext?,
                               prefixes: Array<String>,
                               namespaces: Array<String>) : SimpleNamespaceContext(prefixes, namespaces) {

    override fun getNamespaceURI(prefix: String): String {
        return parent?.getNamespaceURI(prefix) ?: super.getNamespaceURI(prefix)
    }

    override fun getPrefix(namespaceURI: String): String? {
        return super.getPrefix(namespaceURI) ?: parent?.getPrefix(namespaceURI)
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun getPrefixes(namespaceURI: String): Iterator<String> {
        if (parent == null) {
            return super.getPrefixes(namespaceURI)
        }
        val prefixes = HashSet<String>()

        super.getPrefixes(namespaceURI).forEach { prefixes.add(it) }

        parent.getPrefixes(namespaceURI).asSequence()
            .filter { prefix -> getLocalNamespaceUri(prefix)==null }
            .forEach { prefixes.add(it) }

        return prefixes.iterator()
    }

    private fun getLocalNamespaceUri(prefix: String): String? {
        return indices.lastOrNull { prefix == getPrefix(it) }?.let { getNamespaceURI(it) }
    }
}