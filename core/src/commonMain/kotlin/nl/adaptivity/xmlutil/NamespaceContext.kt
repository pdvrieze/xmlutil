/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
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

@file:JvmName("_actualAliassesKt")
package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import kotlin.jvm.JvmName

/** Interface that provides access to namespace queries */
public expect interface NamespaceContext {
    public fun getNamespaceURI(prefix: String): String?
    public fun getPrefix(namespaceURI: String): String?
    public fun getPrefixes(namespaceURI: String): Iterator<String>
}

@Deprecated("Just use NamespaceContext", ReplaceWith("NamespaceContext", "nl.adaptivity.xmlutil.NamespaceContext"))
@XmlUtilInternal
public interface NamespaceContextImpl : NamespaceContext

/** Namespace context that allows iterating over the namespaces. */
@Suppress("DEPRECATION")
@MpJvmDefaultWithCompatibility
public interface IterableNamespaceContext : NamespaceContextImpl, Iterable<Namespace> {
    public fun freeze(): IterableNamespaceContext = SimpleNamespaceContext(this)


    public operator fun plus(secondary: IterableNamespaceContext): IterableNamespaceContext =
        SimpleNamespaceContext((asSequence() + secondary.asSequence()).toList())

}

@Suppress("NOTHING_TO_INLINE")
@Deprecated("No longer needed using JDK>8", ReplaceWith("getPrefixes(namespaceURI)"))
public inline fun NamespaceContext.prefixesFor(namespaceURI: String): Iterator<String> =
    getPrefixes(namespaceURI)

