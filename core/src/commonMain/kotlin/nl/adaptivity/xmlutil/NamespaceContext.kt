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
public expect interface NamespaceContext {
    public fun getNamespaceURI(prefix: String): String?
    public fun getPrefix(namespaceURI: String): String?
}

/** Helper interface for implementation.
 * @suppress
 */
@XmlUtilInternal
public expect interface NamespaceContextImpl : NamespaceContext {
    @Deprecated("Don't use as unsafe", ReplaceWith("prefixesFor(namespaceURI)", "nl.adaptivity.xmlutil.prefixesFor"))
    public fun getPrefixesCompat(namespaceURI: String): Iterator<String>
}

/** Namespace context that allows iterating over the namespaces. */
public interface IterableNamespaceContext : NamespaceContextImpl, Iterable<Namespace> {
    public fun freeze(): IterableNamespaceContext = SimpleNamespaceContext(this)


    @Suppress("DEPRECATION")
    public operator fun plus(secondary: IterableNamespaceContext): IterableNamespaceContext =
        SimpleNamespaceContext((asSequence() + secondary.asSequence()).toList())

}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE", "DEPRECATION")
public expect inline fun NamespaceContext.prefixesFor(namespaceURI: String): Iterator<String>

