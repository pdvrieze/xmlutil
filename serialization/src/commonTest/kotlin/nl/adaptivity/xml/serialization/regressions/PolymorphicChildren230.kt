/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlConfig.Companion.IGNORING_UNKNOWN_CHILD_HANDLER
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test
import kotlin.test.assertEquals

class PolymorphicChildren230 {



    @Serializable
    @SerialName("modules")
    @XmlSerialName("modules")
    data class ModulesSerialization(
        @XmlSerialName(value = "resId") val ressortId: Int?,
        val contentUrl: String?,
        @XmlSerialName(value = "module") val modules: List<ModuleSerialization>?,
    )

    @Serializable
    @SerialName("module")
    @XmlSerialName("module")
    data class ModuleSerialization(
        val id: Int?,
        val title: String?,
        @XmlChildrenName("fsdfij") val objects: List</*@Polymorphic*/ HttpObjectSerialization>?,
    )

    @Serializable
    sealed class HttpObjectSerialization

    @Serializable
    @SerialName("document")
    @XmlSerialName("document")
    data class DocumentSerialization(
        val id: Int?,
        val title: String?,
    ) : HttpObjectSerialization()

    @Serializable
    @SerialName("otherObject")
    @XmlSerialName("otherObject")
    data class OtherObjectSerialization(
        val id: Int?,
    ) : HttpObjectSerialization()

    val baseModule = SerializersModule {
        polymorphic (HttpObjectSerialization::class) {
            subclass(DocumentSerialization::class)
            subclass(OtherObjectSerialization::class)
        }
    }

    val xml = XML(serializersModule = baseModule) {
        fast {
            unknownChildHandler = IGNORING_UNKNOWN_CHILD_HANDLER
            isUnchecked = true
        }
    }

    @Test
    fun testEncode() {
        val data = ModulesSerialization(
            ressortId = 1,
            contentUrl = "Test",
            modules = listOf(
                ModuleSerialization(
                    id = 1,
                    title = "Module 1",
                    objects = listOf(
                        DocumentSerialization(id = 1, title = "Title1"),
                        DocumentSerialization(id = 2, title = "Title2"),
                        DocumentSerialization(id = 3, title = "Title3"),
                        OtherObjectSerialization(id = 4),
                    ),
                )
            ),
        )

        val actual = xml.encodeToString(data)

        assertXmlEquals(
            "<?xml version='1.1' ?>" +
                    "<modules resId=\"1\" contentUrl=\"Test\">" +
                    "<module id=\"1\" title=\"Module 1\">" +
                    "<objects>" +
                    "<document id=\"1\" title=\"Title1\" />" +
                    "<document id=\"2\" title=\"Title2\" />" +
                    "<document id=\"3\" title=\"Title3\" />" +
                    "<otherObject id=\"4\" />" +
                    "</objects>" +
                    "</module></modules>",
            actual
        )

    }

    @Test
    fun testDecode() {
        val xmlData =
            "<?xml version='1.1' ?><modules resId=\"1\" contentUrl=\"Test\"><module id=\"1\" title=\"Module 1\"><objects><document id=\"1\" title=\"Title1\" /><document id=\"2\" title=\"Title2\" /><document id=\"3\" title=\"Title3\" /><otherObject id=\"4\" /></objects></module></modules>"
        val actual = xml.decodeFromString<ModulesSerialization>(xmlData)
        assertEquals(1, actual.modules!!.size)
        val module = actual.modules[0]

        assertEquals(4, module.objects!!.size)
    }
}
