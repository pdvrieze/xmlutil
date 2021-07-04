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

public actual interface NamespaceContext {
    public actual fun getNamespaceURI(prefix: String): String?
    public actual fun getPrefix(namespaceURI: String): String?

    @Deprecated("Don't use as unsafe", ReplaceWith("prefixesFor(namespaceURI)", "nl.adaptivity.xmlutil.prefixesFor"))
    public fun getPrefixes(namespaceURI: String): Iterator<String?>
}

public actual interface NamespaceContextImpl : NamespaceContext {
    public actual fun getPrefixesCompat(namespaceURI: String): Iterator<String>

    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixes(namespaceURI: String): Iterator<String> = getPrefixesCompat(namespaceURI)
}

@Suppress("NOTHING_TO_INLINE", "DEPRECATION", "UNCHECKED_CAST")
public actual inline fun NamespaceContext.prefixesFor(namespaceURI: String): Iterator<String> =
    getPrefixes(namespaceURI) as Iterator<String>
