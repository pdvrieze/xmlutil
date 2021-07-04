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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.util.CombiningNamespaceContext

/** Interface that provides access to namespace queries */
expect interface NamespaceContext {
    fun getNamespaceURI(prefix: String): String?
    fun getPrefix(namespaceURI: String): String?
}

/** Helper interface for implementation.
 * @suppress
 */
@XmlUtilInternal
expect interface NamespaceContextImpl : NamespaceContext {
    @Deprecated("Don't use as unsafe", ReplaceWith("prefixesFor(namespaceURI)", "nl.adaptivity.xmlutil.prefixesFor"))
    fun getPrefixesCompat(namespaceURI: String): Iterator<String>
}

/** Namespace context that allows iterating over the namespaces. */
interface IterableNamespaceContext : NamespaceContextImpl, Iterable<Namespace>, FreezableNamespaceContext {
    override fun freeze(): IterableNamespaceContext = SimpleNamespaceContext(this)


    @Suppress("DEPRECATION")
    operator fun plus(secondary: IterableNamespaceContext): IterableNamespaceContext =
        SimpleNamespaceContext((asSequence() + secondary.asSequence()).toList())

}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE", "DEPRECATION")
expect inline fun NamespaceContext.prefixesFor(namespaceURI: String): Iterator<String>

