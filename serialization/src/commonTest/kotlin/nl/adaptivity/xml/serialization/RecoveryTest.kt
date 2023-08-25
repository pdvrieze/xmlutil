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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.StructureKind
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecoveryTest {

    @Serializable
    data class Data(val a: String, val b: String)

    @Serializable
    @XmlSerialName("Container", "", "")
    data class Container(val stat: Stat)

    @Serializable
    @XmlSerialName("Stat", "SomeNs", "link")
    data class Stat(val value: String)

    @Test
    fun testDeserializeNonRecovering() {
        val input = "<Container><link:Stat xmlns:link=\"SomeNs\" value=\"foo\"/></Container>"
        val parsed = XML.decodeFromString<Container>(input)
        assertEquals(Container(Stat("foo")), parsed)
    }

    /**
     * Test in response to #160
     */
    @Test
    fun testDeserializeRecoveringWithParser() {
        val xml = XML {
            policy = object: DefaultXmlSerializationPolicy(true) {
                @ExperimentalXmlUtilApi
                override fun handleUnknownContentRecovering(
                    input: XmlReader,
                    inputKind: InputKind,
                    descriptor: XmlDescriptor,
                    name: QName?,
                    candidates: Collection<Any>
                ): List<XML.ParsedData<*>> {
                    XmlSerializationPolicy.recoverNullNamespaceUse(inputKind, descriptor, name)?.let { return it }
                    return super.handleUnknownContentRecovering(input, inputKind, descriptor, name, candidates)
                }
            }
        }
        val input = "<Container><Stat value=\"foo\"/></Container>"
        val parsed = xml.decodeFromString<Container>(input)
        assertEquals(Container(Stat("foo")), parsed)
    }

    @OptIn(ExperimentalXmlUtilApi::class, ExperimentalSerializationApi::class)
    @Test
    fun testDeserializeRecovering() {
        val serialized = "<Data a=\"foo\" c=\"bar\" />"

        val xml = XML {
            defaultPolicy {
                unknownChildHandler = UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
                    assertEquals(QName("c"), name)
                    assertEquals(InputKind.Attribute, inputKind)
                    assertEquals(StructureKind.CLASS, descriptor.kind)
                    assertEquals(QName("a"), descriptor.getElementDescriptor(0).tagName)
                    assertEquals(QName("b"), descriptor.getElementDescriptor(1).tagName)
                    assertEquals(
                        listOf(
                            PolyInfo(QName("a"), 0, descriptor.getElementDescriptor(0)),
                            PolyInfo(QName("b"), 1, descriptor.getElementDescriptor(1))
                        ),
                        candidates
                    )

                    listOf(XML.ParsedData(1, input.getAttributeValue(name!!)))
                }
            }
        }
        val parsed: Data = xml.decodeFromString(serialized)
        val expected = Data("foo", "bar")
        assertEquals(expected, parsed)
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun testDeserializeRecoveringNotProvidingRequired() {
        val serialized = "<Data a=\"foo\" c=\"bar\" />"

        val xml = XML {
            defaultPolicy {
                unknownChildHandler = UnknownChildHandler { _, _, _, name, _ ->
                    assertEquals(QName("c"), name)
                    emptyList()
                }
            }
        }
        val e = assertFailsWith<SerializationException> { xml.decodeFromString<Data>(serialized) }
        assertContains(e.message!!, "Field 'b' is required")
        assertContains(e.message!!, ", but it was missing")
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun testDeserializeRecoveringDuplicateData() {
        val serialized = "<Data a=\"foo\" c=\"bar\" />"

        val xml = XML {
            defaultPolicy {
                unknownChildHandler = UnknownChildHandler { input, _, _, name, _ ->
                    assertEquals(QName("c"), name)
                    listOf(
                        XML.ParsedData(1, input.getAttributeValue(name!!)),
                        XML.ParsedData(1, "baz"),
                    )
                }
            }
        }
        val d = xml.decodeFromString<Data>(serialized)
        assertEquals("baz", d.b)
    }

}
