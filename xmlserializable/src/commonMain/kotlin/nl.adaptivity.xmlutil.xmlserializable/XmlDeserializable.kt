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
@file:JvmMultifileClass
@file:JvmName("XmlUtilDeserializable")

package nl.adaptivity.xmlutil.xmlserializable

import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads


/**
 * Created by pdvrieze on 04/11/15.
 */
@Deprecated("Use kotlinx.serialization instead")
interface XmlDeserializable {

    /**
     * Handle the given attribute.
     * @param attributeNamespace The namespace of the the attribute.
     *
     * @param attributeLocalName The local name of the attribute
     *
     * @param attributeValue The value of the attribute
     *
     * @return `true` if handled, `false` if not. (The caller may use this for errors)
     */
    fun deserializeAttribute(
        attributeNamespace: String?,
        attributeLocalName: String,
        attributeValue: String
                            ): Boolean = false

    /** Listener called just before the children are deserialized. After attributes have been processed.  */
    fun onBeforeDeserializeChildren(reader: XmlReader) {}

    /** The name of the element, needed for the automated validation */
    @Transient
    val elementName: QName
}


@Suppress("DEPRECATION")
@Deprecated("Use kotlinx.serialization instead")
fun <T : XmlDeserializable> T.deserializeHelper(reader: XmlReader): T {
    reader.skipPreamble()

    val elementName = elementName
    assert(
        reader.isElement(elementName)
          ) { "Expected $elementName but found ${reader.localName}" }

    for (i in reader.attributeIndices.reversed()) {
        deserializeAttribute(
            reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
            reader.getAttributeValue(i)
                            )
    }

    onBeforeDeserializeChildren(reader)

    if (this is SimpleXmlDeserializable) {
        loop@ while (reader.hasNext() && reader.next() !== EventType.END_ELEMENT) {
            when (reader.eventType) {
                EventType.START_ELEMENT          -> if (!deserializeChild(reader)) reader.unhandledEvent()
                EventType.TEXT, EventType.CDSECT -> if (!deserializeChildText(reader.text)) reader.unhandledEvent()
                // If the text was not deserialized, then just fall through
                else                             -> reader.unhandledEvent()
            }
        }
    } else if (this is ExtXmlDeserializable) {
        deserializeChildren(reader)

        reader.require(EventType.END_ELEMENT, elementName.namespaceURI, elementName.localPart)

    } else {// Neither, means ignore children
        if (!isXmlWhitespace(reader.siblingsToFragment().contentString)) {
            throw XmlException("Unexpected child content in element")
        }
    }
    return this
}


@JvmOverloads
internal fun XmlReader.unhandledEvent(message: String? = null) {
    val actualMessage = when (eventType) {
        EventType.ENTITY_REF,
        EventType.CDSECT,
        EventType.TEXT          -> if (!isWhitespace()) message
            ?: "Content found where not expected [$locationInfo] Text:'$text'" else null
        EventType.START_ELEMENT -> message ?: "Element found where not expected [$locationInfo]: $name"
        EventType.END_DOCUMENT  -> message ?: "End of document found where not expected"
        else                    -> null
    }// ignore

    actualMessage?.let { throw XmlException(it) }
}
