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

import kotlinx.serialization.ExperimentalSerializationApi
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlDelegatingReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor

/**
 * A filter that reads xml with dynamic tags and represents it as a structured xml with id attribute
 */
internal class DynamicTagReader(reader: XmlReader, descriptor: XmlDescriptor) : XmlDelegatingReader(reader) {
    private var initDepth = reader.depth
    private val filterDepth: Int
        /**
         * We want to be safe so only handle the content at relative depth 0. The way that depth is determined
         * means that the depth is the depth after the tag (and end tags are thus one level lower than the tag (and its
         * content). We correct for that here.
         */
        get() = when (eventType) {
            EventType.END_ELEMENT -> delegate.depth - initDepth + 1
            else                  -> delegate.depth - initDepth
        }

    /**
     * Store the tag name that we need to use instead of the dynamic tag
     */
    private val elementName = descriptor.tagName

    /**
     * Store the name of the id attribute that is synthetically generated. This property is initialised
     * this way to allow for name remapping in the format policy.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private val idAttrName = (0 until descriptor.elementsCount)
        .first { descriptor.serialDescriptor.getElementName(it) == "id" }
        .let { descriptor.getElementDescriptor(it) }
        .tagName

    /**
     * This filter is created when we are at the local tag. So we can already determine the value of the
     * synthetic id property. In this case just by removing the prefix.
     */
    val idValue = delegate.localName.removePrefix("Test_")

    /**
     * When we are at relative depth 0 we add an attribute at position 0 (easier than at the end). This allows
     * for other attributes (actually written) on the tag.
     */
    override val attributeCount: Int
        get() = when (filterDepth) {
            0 -> super.attributeCount + 1
            else -> super.attributeCount
        }

    /**
     * At relative depth 0, attribute 0 we inject the namespace for the id attribute. The other attribute indices are just
     * adjusted.
     */
    override fun getAttributeNamespace(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idAttrName.namespaceURI
            else -> super.getAttributeNamespace(index - 1)
        }
        else -> super.getAttributeNamespace(index)
    }

    /**
     * At relative depth 0, attribute 0 we inject the prefix for the id attribute. The other attribute indices are just
     * adjusted.
     */
    override fun getAttributePrefix(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idAttrName.prefix
            else -> super.getAttributePrefix(index - 1)
        }
        else -> super.getAttributePrefix(index)
    }

    /**
     * At relative depth 0, attribute 0 we inject the local name for the id attribute. The other attribute indices are just
     * adjusted.
     */
    override fun getAttributeLocalName(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idAttrName.localPart
            else -> super.getAttributeLocalName(index - 1)
        }
        else -> super.getAttributeLocalName(index)
    }

    /**
     * At relative depth 0, attribute 0 we inject the value for the id attribute. The other attribute indices are just
     * adjusted.
     */
    override fun getAttributeValue(index: Int): String = when (filterDepth) {
        0 -> when (index) {
            0    -> idValue
            else -> super.getAttributeValue(index - 1)
        }
        else -> super.getAttributeValue(index)
    }

    /**
     * When attribute values are retrieved by name, pick up the synthetic id attribute at relative depth 0.
     * Note that while the xml format does not use this method it is good practice to override it anyway.
     */
    override fun getAttributeValue(nsUri: String?, localName: String): String? = when {
        filterDepth == 0 &&
                (nsUri ?: "") == idAttrName.namespaceURI &&
                localName == idAttrName.localPart -> idValue
        else                                      -> super.getAttributeValue(nsUri, localName)
    }

    /**
     * When we are at relative depth 0 we return the synthetic name rather than the original.
     */
    override val namespaceURI: String
        get() = when (filterDepth) {
            0 -> elementName.namespaceURI
            else -> super.namespaceURI
        }

    /**
     * When we are at relative depth 0 we return the synthetic name rather than the original.
     */
    override val localName: String
        get() = when (filterDepth) {
            0 -> elementName.localPart
            else -> super.localName
        }

    /**
     * When we are at relative depth 0 we return the synthetic name rather than the original.
     */
    override val prefix: String
        get() = when (filterDepth) {
            0 -> elementName.prefix
            else -> super.prefix
        }
}