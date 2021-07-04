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

public actual typealias NamespaceContext = javax.xml.namespace.NamespaceContext

public actual interface NamespaceContextImpl : javax.xml.namespace.NamespaceContext {
    override fun getPrefixes(namespaceURI: String): Iterator<String> = getPrefixesCompat(namespaceURI)
    public actual fun getPrefixesCompat(namespaceURI: String): Iterator<String>
}

@Suppress("NOTHING_TO_INLINE", "USELESS_CAST", "UNCHECKED_CAST")
public actual inline fun NamespaceContext.prefixesFor(namespaceURI: String): Iterator<String> =
    getPrefixes(namespaceURI) as Iterator<String> // This cast is needed on JDK8
