/*
 * Copyright (c) 2020.
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

package net.devrieze.serialization.examples.dynamictagnames

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlDelegatingReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor

internal class DynamicTagReader(reader: XmlReader, descriptor: XmlDescriptor) : XmlDelegatingReader(reader) {
    private var _filterDepth = 0
    val filterDepth: Int
        /**
         * As we start already on the current start tag event the end tag event will move the filterDepth one lower
         * when determining the name of the tag.
         */
        get() = when (eventType) {
            EventType.END_ELEMENT -> _filterDepth + 1
            else                  -> _filterDepth
        }

    private val elementName = descriptor.tagName

    private val idAttrName = (0 until descriptor.elementsCount)
        .first { descriptor.serialDescriptor.getElementName(it) == "id" }
        .let { descriptor.getElementDescriptor(it) }
        .tagName

    val idValue = delegate.localName.removePrefix("Test_")

    override fun next(): EventType {
        return super.next().also {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it) {
                EventType.START_ELEMENT -> ++_filterDepth
                EventType.END_ELEMENT -> --_filterDepth
            }
        }
    }

    override val attributeCount: Int
        get() = when (filterDepth) {
            0 -> super.attributeCount + 1
            else -> super.attributeCount
        }

    override fun getAttributeNamespace(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idAttrName.namespaceURI
            else -> super.getAttributeNamespace(index - 1)
        }
        else -> super.getAttributeNamespace(index)
    }

    override fun getAttributePrefix(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idAttrName.prefix
            else -> super.getAttributePrefix(index - 1)
        }
        else -> super.getAttributePrefix(index)
    }

    override fun getAttributeLocalName(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idAttrName.localPart
            else -> super.getAttributeLocalName(index - 1)
        }
        else -> super.getAttributeLocalName(index)
    }

    override fun getAttributeName(index: Int): QName = when (filterDepth) {
        0 -> when (index) {
            0    -> idAttrName
            else -> super.getAttributeName(index - 1)
        }
        else -> super.getAttributeName(index)
    }

    override fun getAttributeValue(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idValue
            else -> super.getAttributeValue(index - 1)
        }
        else -> super.getAttributeValue(index)
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? = when {
        filterDepth == 1 && localName == idAttrName.localPart -> idValue
        else                                                  -> super.getAttributeValue(nsUri, localName)
    }

    override val namespaceURI: String
        get() = when (filterDepth) {
            0 -> elementName.namespaceURI
            else -> super.namespaceURI
        }

    override val localName: String
        get() = when (filterDepth) {
            0 -> elementName.localPart
            else -> super.localName
        }

    override val prefix: String
        get() = when (filterDepth) {
            0 -> elementName.prefix
            else -> super.prefix
        }

    override val name: QName
        get() = when (filterDepth) {
            0 -> elementName
            else -> super.name
        }
}