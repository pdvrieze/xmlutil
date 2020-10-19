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

import nl.adaptivity.xmlutil.XmlDelegatingWriter
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor

internal class DynamicTagWriter(writer: XmlWriter, descriptor: XmlDescriptor, val idValue: String) :
    XmlDelegatingWriter(writer) {
    private var filterDepth = 0

    private val elementName = descriptor.tagName

    private val idAttrName = (0 until descriptor.elementsCount)
        .first { descriptor.serialDescriptor.getElementName(it) == "id" }
        .let { descriptor.getElementDescriptor(it) }
        .tagName

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        when (filterDepth) {
            0    -> super.startTag("", "Test_$idValue", "")
            else -> super.startTag(namespace, localName, prefix)
        }
        ++filterDepth
    }

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        when {
            filterDepth == 1 &&
                    name == idAttrName.localPart
                 -> Unit
            else -> super.attribute(namespace, name, prefix, value)
        }

    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        --filterDepth
        when (filterDepth) {
            0    -> super.endTag("", "Test_$idValue", "")
            else -> super.endTag(namespace, localName, prefix)
        }
    }
}