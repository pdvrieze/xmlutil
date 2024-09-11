/*
 * Copyright (c) 2024.
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

import nl.adaptivity.xmlutil.core.impl.dom.DocumentImpl
import nl.adaptivity.xmlutil.core.impl.idom.IElement
import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.util.CompactFragment
import org.w3c.dom.parsing.XMLSerializer
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Document as DomDocument

/**
 * Read the current element (and content) and all its siblings into a fragment.
 * @receiver The source stream.
 *
 * @return the fragment
 *
 * @throws XmlException parsing failed
 */
public actual fun XmlReader.siblingsToFragment(): CompactFragment {
    @Suppress("DEPRECATION")
    val d = (this as? DomReader)?.delegate
    val doc: DocumentImpl = when {
        d == null -> DocumentImpl(DomDocument())
        d is DocumentImpl -> d
        d.getNodeType() == NodeConsts.DOCUMENT_NODE -> DocumentImpl(d as DomDocument)
        else -> DocumentImpl((d.getOwnerDocument() as? DomDocument) ?: DomDocument())
    }
    val frag = doc.createDocumentFragment()
    val wrapperElement: IElement = doc.createElementNS(WRAPPERNAMESPACE, WRAPPERQNAME)
    frag.appendChild(wrapperElement)
    if (!isStarted) {
        if (hasNext()) {
            next()
        } else {
            return CompactFragment("")
        }
    }

    val startLocation = extLocationInfo
    try {

        val missingNamespaces = mutableMapOf<String, String>()

        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0
        var type: EventType? = eventType
        while (type !== EventType.END_DOCUMENT && (type !== EventType.END_ELEMENT || depth > initialDepth)) {
            when (type) {
                EventType.START_ELEMENT -> {
                    @Suppress("DEPRECATION")
                    val out = DomWriter(wrapperElement as Node2, true)
                    @Suppress("DEPRECATION")
                    out.addUndeclaredNamespaces(this, missingNamespaces)
                    writeCurrent(out) // writes the start tag
                    out.writeElementContent(missingNamespaces, this) // writes the children and end tag
                    out.close()
                }

                EventType.IGNORABLE_WHITESPACE,
                EventType.TEXT -> wrapperElement.appendChild(wrapperElement.getOwnerDocument().createTextNode(text))

                EventType.CDSECT -> wrapperElement.appendChild(
                    wrapperElement.getOwnerDocument().createCDATASection(text)
                )

                EventType.COMMENT -> wrapperElement.appendChild(wrapperElement.getOwnerDocument().createComment(text))

                EventType.ENTITY_REF -> throw XmlException("Entity references are not expected here")

                EventType.ATTRIBUTE -> throw AssertionError("Attributes are not expected in the event stream")

                else -> Unit // These elements are ignored/not part of a fragment
            }
            type = if (hasNext()) next() else break
        }

        if (missingNamespaces[""] == "") missingNamespaces.remove("")

        val ns = missingNamespaces.entries.asSequence()
            .filter { (prefix, uri) -> prefix != "" || uri != "" }
            .map { (prefix, uri) ->
                wrapperElement.setAttributeNS(
                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                    if (prefix == "") "xmlns" else "xmlns:$prefix",
                    uri
                )
                XmlEvent.NamespaceImpl(prefix, uri)
            }.toList()

        val wrappedString = XMLSerializer().serializeToString(wrapperElement.delegate)
        val tagEndIdx = wrappedString.indexOf('>', WRAPPERQNAME.length)
        val unwrappedString = when {
            wrappedString[tagEndIdx-1]=='/' -> ""
            else -> wrappedString.substring(
                tagEndIdx + 1,
                wrappedString.length - WRAPPERQNAME.length - 3
            )
        }
        return CompactFragment(ns, unwrappedString)
    } catch (e: XmlException) {
        throw XmlException("Failure to parse children into string", startLocation ?: e.locationInfo, e)
    } catch (e: RuntimeException) {
        throw XmlException("Failure to parse children into string", startLocation, e)
    }

}

/**
 * Functions that work on both js/jvm but have different implementations
 */

private const val WRAPPERPPREFIX = "SDFKLJDSF"
private const val WRAPPERELEMENT = "afjlfxkls"
private const val WRAPPERQNAME = "$WRAPPERPPREFIX:$WRAPPERELEMENT"
private const val WRAPPERNAMESPACE = "http://wrapperns"
