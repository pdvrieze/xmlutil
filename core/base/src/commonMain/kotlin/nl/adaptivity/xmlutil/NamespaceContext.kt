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

@file:JvmName("_actualAliassesKt")
package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import kotlin.jvm.JvmName

/** Interface that provides access to namespace queries */
public expect interface NamespaceContext {
    /**
     * Return the namespace uri for a given prefix. Note that some prefixes are predefined.
     * @return The uri, or `null` if not found.
     */
    public fun getNamespaceURI(prefix: String): String?

    /**
     * Return a prefix for a given namespace uri. If there are multiple candidates the returned
     * prefix is implementation defined.
     * @return `null` if no prefix is found, otherwise a matching prefix.
     */
    public fun getPrefix(namespaceURI: String): String?

    /**
     * Return an iterator that provides all prefixes for the given namespace.
     */
    public fun getPrefixes(namespaceURI: String): Iterator<String>
}

/** Namespace context that allows iterating over the namespaces. */
@MpJvmDefaultWithCompatibility
public interface IterableNamespaceContext : NamespaceContext, Iterable<Namespace> {
    /** Create a copy of the namespace context that will not change externally. */
    public fun freeze(): IterableNamespaceContext = SimpleNamespaceContext(this)


    /** Create a new context that combines both */
    public operator fun plus(secondary: IterableNamespaceContext): IterableNamespaceContext =
        SimpleNamespaceContext((asSequence() + secondary.asSequence()).toList())

    /** Retrieve an iterator returning all the known namespaces in this context. */
    override fun iterator(): Iterator<Namespace>
}

