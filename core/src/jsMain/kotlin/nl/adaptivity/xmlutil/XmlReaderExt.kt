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

import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.dom.Document
import nl.adaptivity.xmlutil.dom.Element
import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.NodeConsts
import org.w3c.dom.parsing.XMLSerializer

/**
 * Read the current element (and content) and all its siblings into a fragment.
 * @receiver The source stream.
 *
 * @return the fragment
 *
 * @throws XmlException parsing failed
 */
public actual fun XmlReader.siblingsToFragment(): CompactFragment {
    val d = (this as? DomReader)?.delegate
    val doc: Document = when (d?.nodeType) {
        NodeConsts.DOCUMENT_NODE -> d as Document
        null -> org.w3c.dom.Document() as Document
        else -> d.ownerDocument ?: org.w3c.dom.Document() as Document
    }
    val frag = doc.createDocumentFragment()
    val wrapperElement: Element = doc.createElementNS(WRAPPERNAMESPACE, WRAPPERQNAME)
    frag.appendChild(wrapperElement)
    if (!isStarted) {
        if (hasNext()) {
            next()
        } else {
            return CompactFragment("")
        }
    }

    val startLocation = locationInfo
    try {

        val missingNamespaces = mutableMapOf<String, String>()
/*
        currentElement.parentElement?.attributes?.forEach { attr ->
            if (attr.prefix=="xmlns") {
                missingNamespaces[localName] = attr.value
            } else if (attr.prefix=="" && attr.localName=="prefix") {
                missingNamespaces[""] = attr.value
            }
        }
*/

        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0
        var type: EventType? = eventType
        while (type !== EventType.END_DOCUMENT && type !== EventType.END_ELEMENT && depth >= initialDepth) {
            when (type) {
                EventType.START_ELEMENT -> {
                    val out = DomWriter(wrapperElement, true)
                    out.addUndeclaredNamespaces(this, missingNamespaces)
                    writeCurrent(out) // writes the start tag
                    out.writeElementContent(missingNamespaces, this) // writes the children and end tag
                    out.close()
                }

                EventType.IGNORABLE_WHITESPACE,
                EventType.TEXT -> wrapperElement.appendChild(wrapperElement.ownerDocument!!.createTextNode(text))

                EventType.CDSECT -> wrapperElement.appendChild(wrapperElement.ownerDocument!!.createCDATASection(text))

                EventType.COMMENT -> wrapperElement.appendChild(wrapperElement.ownerDocument!!.createComment(text))

                EventType.ENTITY_REF -> throw XmlException("Entity references are not expected here")

                EventType.ATTRIBUTE -> throw AssertionError("Attributes are not expected in the event stream")

                else -> Unit // These elements are ignored/not part of a fragment
            }
            type = if (hasNext()) next() else null
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

        val wrappedString = XMLSerializer().serializeToString(wrapperElement as org.w3c.dom.Node)
        val unwrappedString = wrappedString.substring(
            wrappedString.indexOf('>', WRAPPERQNAME.length) + 1,
            wrappedString.length - WRAPPERQNAME.length - 3
        )
        return CompactFragment(ns, unwrappedString)
    } catch (e: XmlException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    } catch (e: RuntimeException) {
        throw XmlException("Failure to parse children into string at $startLocation", e)
    }

}

/**
 * Functions that work on both js/jvm but have different implementations
 */

private const val WRAPPERPPREFIX = "SDFKLJDSF"
private const val WRAPPERELEMENT = "afjlfxkls"
private const val WRAPPERQNAME = "$WRAPPERPPREFIX:$WRAPPERELEMENT"
private const val WRAPPERNAMESPACE = "http://wrapperns"
