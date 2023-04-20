/*
 * Copyright (c) 2021.
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

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.NamespaceContextImpl
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlWriter

internal class NamespaceCollectingXmlWriter(
    private val prefixToUriMap: MutableMap<String, String>,
    private val uriToPrefixMap: MutableMap<String, String>,
    private val pendingNamespaces: MutableSet<String>
) : XmlWriter {

    override var depth: Int = 0

    override var indentString: String = ""

    private fun recordNamespace(prefix: String, namespaceUri: String) {
        if (namespaceUri !in uriToPrefixMap) {
            if (namespaceUri.isEmpty()) { // always special case the default namespace
                val existingDefaultNamespace = prefixToUriMap[""]
                if (existingDefaultNamespace!=null) {
                    uriToPrefixMap.remove(existingDefaultNamespace)
                    pendingNamespaces.add(existingDefaultNamespace)
                }
                uriToPrefixMap[""] = ""
                prefixToUriMap[""] = ""
            } else if (prefix in prefixToUriMap) {
                pendingNamespaces.add(namespaceUri)
            } else {
                if (namespaceUri in pendingNamespaces) {
                    pendingNamespaces.remove(namespaceUri)
                }
                prefixToUriMap[prefix] = namespaceUri
                uriToPrefixMap[namespaceUri] = prefix
            }
        }
    }

    override fun setPrefix(prefix: String, namespaceUri: String) {
        recordNamespace(prefix, namespaceUri)
    }

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        recordNamespace(namespacePrefix, namespaceUri)
    }

    override fun getNamespaceUri(prefix: String): String? = prefixToUriMap[prefix]

    override fun getPrefix(namespaceUri: String?): String? = uriToPrefixMap[namespaceUri]

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (namespace == XMLConstants.XMLNS_ATTRIBUTE_NS_URI) {
            if (prefix == XMLConstants.XMLNS_ATTRIBUTE) {
                namespaceAttr(prefix, value)
            } else if (prefix == "") {
                namespaceAttr(name, value)
            }
        }
    }

    override val namespaceContext: NamespaceContext
        get() = object : NamespaceContextImpl {
            override fun getPrefix(namespaceURI: String): String? =
                this@NamespaceCollectingXmlWriter.getPrefix(namespaceURI)

            override fun getNamespaceURI(prefix: String): String? {
                return getNamespaceUri(prefix)
            }

            override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
                return listOfNotNull(getPrefix(namespaceURI)).iterator()
            }
        }

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        ++depth
    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        --depth
    }

    override fun close() {}

    override fun flush() {}

    override fun comment(text: String) {}

    override fun text(text: String) {}

    override fun cdsect(text: String) {}

    override fun entityRef(text: String) {}

    override fun processingInstruction(text: String) {}

    override fun ignorableWhitespace(text: String) {}

    override fun docdecl(text: String) {}

    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {}

    override fun endDocument() {}
}
