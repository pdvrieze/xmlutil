/*
 * Copyright (c) 2025-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.dom.PlatformText
import nl.adaptivity.xmlutil.isXmlWhitespace

public actual interface Text : CharacterData, PlatformText {
    actual override fun cloneNode(deep: Boolean): Text

    override fun splitText(offset: Int): Text? {
        if (offset >= length) return null
        val newText = ownerDocument.createTextNode(substringData(offset, length-offset))
        setNodeValue(substringData(0, offset))
        getParentElement()?.insertBefore(newText, nextSibling)
        return newText
    }

    override fun isElementContentWhitespace(): Boolean {
        return isXmlWhitespace(data)
    }

    override fun getWholeText(): String {
        var node: Text? = this
        while (node?.previousSibling?.getNodetype() == NodeType.TEXT_NODE) { node = node.previousSibling as Text}

        return buildString {
            while (node?.getNodetype() == NodeType.TEXT_NODE) {
                append(getData())

                node = node.nextSibling as Text?
            }
        }
    }

    override fun replaceWholeText(content: String?): org.w3c.dom.Text? {
        TODO("not implemented")
    }
}
