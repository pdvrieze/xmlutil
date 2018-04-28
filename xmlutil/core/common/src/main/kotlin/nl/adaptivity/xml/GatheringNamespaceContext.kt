/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */
package nl.adaptivity.xml

import nl.adaptivity.xml.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xml.XMLConstants.XML_NS_URI


/**
 * Class that gathers namespace queries and records them in the given map.
 * Created by pdvrieze on 20/10/15.
 */
class GatheringNamespaceContext(private val parentContext: NamespaceContext?,
                                private val resultMap: MutableMap<String, String>) : NamespaceContext {

    override fun getNamespaceURI(prefix: String): String? {
        return parentContext?.getNamespaceURI(prefix)?.apply {
            if (!isEmpty() && prefix != XMLNS_ATTRIBUTE) {
                resultMap.put(prefix, this)
            }
        }
    }

    override fun getPrefix(namespaceURI: String): String? {
        return parentContext?.getPrefix(namespaceURI)?.apply {
            if (namespaceURI != XMLNS_ATTRIBUTE_NS_URI && namespaceURI != XML_NS_URI) {
                resultMap.put(this, namespaceURI)
            }
        }
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")// Somehow this type has no proper generic parameter
    override fun getPrefixes(namespaceURI: String): Iterator<String> {
        if (parentContext == null) {
            return emptyList<String>().iterator()
        }
        if (namespaceURI != XMLNS_ATTRIBUTE_NS_URI && namespaceURI != XML_NS_URI) {

            val it = parentContext.getPrefixes(namespaceURI) as Iterator<String>
            while (it.hasNext()) {
                resultMap.put(it.next(), namespaceURI)
            }
        }
        return parentContext.getPrefixes(namespaceURI) as Iterator<String>
    }
}
