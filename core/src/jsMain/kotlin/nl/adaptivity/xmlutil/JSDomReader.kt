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

import kotlinx.dom.isElement
import kotlinx.dom.isText
import nl.adaptivity.js.util.*
import org.w3c.dom.*

public actual typealias PlatformXmlReader = DomReader

@Deprecated("Just use DomReader", ReplaceWith("DomReader"))
public typealias JSDomReader = DomReader

private fun Short.toEventType(endOfElement: Boolean): EventType {
    return when (this) {
        Node.ATTRIBUTE_NODE              -> EventType.ATTRIBUTE
        Node.CDATA_SECTION_NODE          -> EventType.CDSECT
        Node.COMMENT_NODE                -> EventType.COMMENT
        Node.DOCUMENT_TYPE_NODE          -> EventType.DOCDECL
        Node.ENTITY_REFERENCE_NODE       -> EventType.ENTITY_REF
        Node.DOCUMENT_NODE               -> if (endOfElement) EventType.START_DOCUMENT else EventType.END_DOCUMENT
//    Node.DOCUMENT_NODE -> EventType.END_DOCUMENT
        Node.PROCESSING_INSTRUCTION_NODE -> EventType.PROCESSING_INSTRUCTION
        Node.TEXT_NODE                   -> EventType.TEXT
        Node.ELEMENT_NODE                -> if (endOfElement) EventType.END_ELEMENT else EventType.START_ELEMENT
//    Node.ELEMENT_NODE -> EventType.END_ELEMENT
        else                             -> throw XmlException("Unsupported event type")
    }
}
