/*
 * Copyright (c) 2026.
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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.dom.DocumentImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestDom {

    @Test
    fun testNormalize() {
        val doc = DocumentImpl(null)
        val docElem = doc.createElement("outer")
        doc.appendChild(docElem)
        assertEquals(doc, docElem.parentNode, "Parent node should be the document element")
        assertEquals(doc, docElem.ownerDocument, "Owner documents should match")

        val text1 = doc.createTextNode("text1")
        val text2 = doc.createTextNode("text2")
        val cdata = doc.createCDATASection("cdata")
        docElem.appendChild(text1)
        docElem.appendChild(text2)
        docElem.appendChild(cdata)

        assertEquals(3, docElem.childNodes.length, "Three child nodes should be present before normalization")

        doc.normalize()

        assertEquals(2, docElem.childNodes.length, "Two child nodes should be present after normalization")

        assertNull(text2.parentNode)
        assertNull(text2.previousSibling)
        assertNull(text2.nextSibling)

        assertEquals(text1, docElem.firstChild)
        assertEquals("text1text2", text1.textContent)
        assertEquals(cdata, text1.nextSibling)
    }
}
