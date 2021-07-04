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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.*


/**
 * A namespace context that combines two namespace contexts. Resolution will first attempt to use
 * the `primary` context, The secondary namespace is a fallback.
 *
 * @property primary The context to first use for looking up
 * @property secondary The fallback context if the name cannot be resolved on the primary.
 */
@XmlUtilInternal
@Deprecated("This type is really only for internal use. It will be moved to a better location")
public class CombiningNamespaceContext(
    public val primary: IterableNamespaceContext,
    public val secondary: IterableNamespaceContext
) : IterableNamespaceContext, NamespaceContextImpl {

    override fun getNamespaceURI(prefix: String): String? {
        val namespaceURI = primary.getNamespaceURI(prefix)
        return if (namespaceURI == null || XMLConstants.NULL_NS_URI == namespaceURI) {
            secondary.getNamespaceURI(prefix)
        } else namespaceURI
    }

    override fun getPrefix(namespaceURI: String): String? {
        val prefix = primary.getPrefix(namespaceURI)
        return if (prefix == null || XMLConstants.NULL_NS_URI == namespaceURI && XMLConstants.DEFAULT_NS_PREFIX == prefix) {
            secondary.getPrefix(namespaceURI)
        } else prefix
    }

    override fun freeze(): IterableNamespaceContext = when {
        primary is SimpleNamespaceContext &&
                secondary is SimpleNamespaceContext -> this

        !secondary.iterator().hasNext() -> primary.freeze()

        !primary.iterator().hasNext() -> secondary.freeze()

        else -> {
            val frozenPrimary = primary.freeze()
            val frozenSecondary = secondary.freeze()
            if (frozenPrimary === primary && frozenSecondary == secondary) {
                this
            } else {
                @Suppress("DEPRECATION")
                CombiningNamespaceContext(primary.freeze(), secondary.freeze())
            }
        }
    }

    override fun iterator(): Iterator<Namespace> {
        return (primary.asSequence() + secondary.asSequence()).iterator()
    }

    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
        val prefixes1 = primary.prefixesFor(namespaceURI)
        val prefixes2 = secondary.prefixesFor(namespaceURI)
        val prefixes = hashSetOf<String>()
        while (prefixes1.hasNext()) {
            prefixes.add(prefixes1.next())
        }
        while (prefixes2.hasNext()) {
            prefixes.add(prefixes2.next())
        }
        return prefixes.iterator()
    }

    override fun plus(secondary: FreezableNamespaceContext): FreezableNamespaceContext =
        @Suppress("DEPRECATION")
        CombiningNamespaceContext(this, secondary)
}
