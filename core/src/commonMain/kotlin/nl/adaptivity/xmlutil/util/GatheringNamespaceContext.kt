/*
 * Copyright (c) 2019.
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
package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.NamespaceContextImpl
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_URI
import nl.adaptivity.xmlutil.prefixesFor


/**
 * Class that gathers namespace queries and records them in the given map (prefix, namespace uri).
 * Created by pdvrieze on 20/10/15.
 */
public class GatheringNamespaceContext(
    private val parentContext: NamespaceContext?,
    private val resultMap: MutableMap<String, String>
) : NamespaceContextImpl {

    override fun getNamespaceURI(prefix: String): String? {
        return parentContext?.getNamespaceURI(prefix)?.apply {
            if (!isEmpty() && prefix != XMLNS_ATTRIBUTE) {
                resultMap[prefix] = this
            }
        }
    }

    override fun getPrefix(namespaceURI: String): String? {
        return parentContext?.getPrefix(namespaceURI)?.apply {
            if (namespaceURI != XMLNS_ATTRIBUTE_NS_URI && namespaceURI != XML_NS_URI) {
                resultMap[this] = namespaceURI
            }
        }
    }

    @Suppress(
        "UNCHECKED_CAST",
        "DEPRECATION",
        "OverridingDeprecatedMember"
    )// Somehow this type has no proper generic parameter
    override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
        if (parentContext == null) {
            return emptyList<String>().iterator()
        }
        if (namespaceURI != XMLNS_ATTRIBUTE_NS_URI && namespaceURI != XML_NS_URI) {

            val it = parentContext.prefixesFor(namespaceURI)
            while (it.hasNext()) {
                resultMap[it.next()] = namespaceURI
            }
        }
        return parentContext.prefixesFor(namespaceURI)
    }
}
