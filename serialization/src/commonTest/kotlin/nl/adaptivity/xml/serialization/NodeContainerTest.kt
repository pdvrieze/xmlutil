/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.dom.Document
import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.createElement
import nl.adaptivity.xmlutil.dom.createTextNode
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.impl.createDocument
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeContainerTest : PlatformTestBase<NodeContainerTest.NodeContainer>(
    NodeContainer(
        createDocument(QName("c")).let { doc ->
            listOf<Node>(
                doc.createElement("a").also {
                    it.appendChild(doc.createTextNode("Foo"))
                },
                doc.createTextNode("Bar"),
                doc.createElement("b").also { e ->
                    e.setAttribute("attr", "zzz")
                    e.appendChild(doc.createElement("c").also { e2 ->
                        e2.appendChild(doc.createTextNode("buzz"))
                    })
                }
            )
        }

    ),
    NodeContainer.serializer(),
    baseXmlFormat = XML { recommended_0_86_3() }
) {
    override val expectedXML: String = "<c><a>Foo</a>Bar<b attr='zzz'><c>buzz</c></b></c>"

    //language=JSON
    override val expectedJson: String =
        """{"content":[["element",{"localname":"a","content":[["text","Foo"]]}],["text","Bar"],["element",{"localname":"b","attributes":{"attr":"zzz"},"content":[["element",{"localname":"c","content":[["text","buzz"]]}]]}]]}"""

    @Test
    override fun testDeserializeXml() {
        super.testDeserializeXml()
    }

    @Serializable
    @XmlSerialName("c")
    data class NodeContainer(@XmlValue val content: List<SerializableNode>) {
        override fun equals(other: Any?): Boolean {
            if (other == null || other::class != NodeContainer::class) return false
            other as NodeContainer
            assertEquals(content.size, other.content.size)
            for (idx in content.indices) {
                val thisJson = Json.encodeToString(NodeSerializer, content[idx])
                val otherJson = Json.encodeToString(NodeSerializer, other.content[idx])
                assertEquals(thisJson, otherJson)
            }
            return true
        }
    }

}
