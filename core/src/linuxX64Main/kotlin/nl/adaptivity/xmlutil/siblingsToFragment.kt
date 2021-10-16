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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.util.CompactFragment

public actual fun XmlReader.siblingsToFragment(): CompactFragment {
    val appendable = StringBuilder()
    if (!isStarted) {
        if (hasNext()) {
            next()
        } else {
            return CompactFragment("")
        }
    }

    val startLocation = locationInfo
    try {

        val missingNamespaces:MutableMap<String, String> = mutableMapOf()
        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0


        var type: EventType? = eventType
        while (type !== EventType.END_DOCUMENT && type !== EventType.END_ELEMENT && depth >= initialDepth) {
            when (type) {
                EventType.START_ELEMENT ->
                    KtXmlWriter(appendable, isRepairNamespaces = false, xmlDeclMode = XmlDeclMode.None).use { out ->
                        out.indentString = "" // disable indents
                        val namespaceForPrefix = out.getNamespaceUri(prefix)
                        writeCurrent(out) // writes the start tag
                        if (namespaceForPrefix != namespaceURI) {
                            out.addUndeclaredNamespaces(this, missingNamespaces)
                        }
                        out.writeElementContent(missingNamespaces, this) // writes the children and end tag
                    }

                EventType.IGNORABLE_WHITESPACE ->
                    if (text.isNotEmpty()) appendable.append(text.xmlEncode())

                EventType.TEXT,
                EventType.CDSECT ->
                    appendable.append(text.xmlEncode())
                else -> {
                } // ignore
            }
            type = if (hasNext()) next() else null
        }

        if (missingNamespaces[""] == "") missingNamespaces.remove("")

        return CompactFragment(SimpleNamespaceContext(missingNamespaces), appendable.toString().toCharArray())
    } catch (e: XmlException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    } catch (e: RuntimeException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    }

}
