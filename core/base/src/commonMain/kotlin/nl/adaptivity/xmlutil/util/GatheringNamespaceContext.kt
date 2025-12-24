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
package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_URI
import nl.adaptivity.xmlutil.XmlUtilInternal


/**
 * Class that gathers namespace queries and records them in the given map (prefix, namespace uri).
 */
@XmlUtilInternal
public class GatheringNamespaceContext(
    private val parentContext: NamespaceContext?,
    private val resultMap: MutableMap<String, String>
) : NamespaceContext {

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

    override fun getPrefixes(namespaceURI: String): Iterator<String> {
        if (parentContext == null) {
            return emptyList<String>().iterator()
        }
        if (namespaceURI != XMLNS_ATTRIBUTE_NS_URI && namespaceURI != XML_NS_URI) {

            val it = parentContext.getPrefixes(namespaceURI)
            while (it.hasNext()) {
                resultMap[it.next()] = namespaceURI
            }
        }
        return parentContext.getPrefixes(namespaceURI)
    }

}
