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
import nl.adaptivity.xmlutil.XmlDelegatingWriter
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor

/**
 * This filter takes the writing of the proper tag and replaces it with writing the dynamic tag. It
 * also ignores the writing of the id attribute. The id attribute is passed as parameter so it is not
 * necessary to delay writing the tag until the id attribute has been written (which, while possible,
 * introduces a lot of complexity which is unneeded in this case).
 */
internal class DynamicTagWriter(private val writer: XmlWriter, descriptor: XmlDescriptor, private val idValue: String) :
    XmlDelegatingWriter(writer) {
    private val initDepth = writer.depth
    private val filterDepth: Int
        /**
         * We want to be safe so only handle the content at relative depth 0. The way that depth is determined
         * means that the depth is the depth after the tag (and end tags are thus one level lower than the tag (and its
         * content). We correct for that here.
         */
        get() = writer.depth - initDepth

    @OptIn(ExperimentalSerializationApi::class)
    private val idAttrName = (0 until descriptor.elementsCount)
        .first { descriptor.serialDescriptor.getElementName(it) == "id" }
        .let { descriptor.getElementDescriptor(it) }
        .tagName

    /**
     * When writing a start tag, if we are at relative depth 0 we write the dynamic tag instead of the
     * default. Otherwise we just pass along the request directly to the parent.
     */
    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        when (filterDepth) {
            0 -> super.startTag("", "Test_$idValue", "")
            else -> super.startTag(namespace, localName, prefix)
        }
    }

    /**
     * Once we have written the start tag we are at depth 1. In that case ignore the id attribute.
     */
    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        when {
            filterDepth == 1 &&
                    (namespace ?: "") == idAttrName.namespaceURI &&
                    name == idAttrName.localPart
            -> Unit

            else -> super.attribute(namespace, name, prefix, value)
        }

    }

    /**
     * Also fix the end tag to actually write the dynamic name.
     */
    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        when (filterDepth) {
            1 -> super.endTag("", "Test_$idValue", "")
            else -> super.endTag(namespace, localName, prefix)
        }
    }
}
