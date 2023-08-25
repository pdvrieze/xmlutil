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

@file:UseSerializers(QNameSerializer::class)
@file:OptIn(ExperimentalXmlUtilApi::class)

package nl.adaptivity.xml.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*
import kotlin.test.*

/**
 * Test to check that namespace collecting works "better": #135
 */
class CollectingDefaultNamespaceTest : PlatformTestBase<CollectingDefaultNamespaceTest.GPXv11>(
    GPXv11("kotlinx.serialization", tracks = listOf(
        GPXv11.Track("Test", GPXv11.Track.Extensions(
            GPXv11.Track.Extensions.TrackExtension("red"),
            GPXv11.Track.Extensions.DefaultNsExtension("Some comment")
        ))
    )),
    GPXv11.serializer(),
    baseXmlFormat = XML { isCollectingNSAttributes = true; autoPolymorphic = true; indent = 4 },
    baseJsonFormat = Json { encodeDefaults = false }
) {
    override val expectedXML: String =
        """<ns1:gpx xmlns:ns1="http://www.topografix.com/GPX/1/1" xmlns:gpxx="http://www.garmin.com/xmlschemas/GpxExtensions/v3"  creator="kotlinx.serialization" version="1.1">
                <ns1:trk>
                    <ns1:name>Test</ns1:name>
                    <ns1:extensions>
                        <gpxx:TrackExtension>
                            <gpxx:DisplayColor>red</gpxx:DisplayColor>
                        </gpxx:TrackExtension>
                        <comment data="Some comment"/>
                    </ns1:extensions>
                </ns1:trk>
            </ns1:gpx>"""
    override val expectedJson: String =
        "{\"creator\":\"kotlinx.serialization\",\"tracks\":[{\"name\":\"Test\",\"extensions\":{\"gpxx\":[{\"type\":\"nl.adaptivity.xml.serialization.CollectingDefaultNamespaceTest.GPXv11.Track.Extensions.TrackExtension\",\"DisplayColor\":\"red\"},{\"type\":\"nl.adaptivity.xml.serialization.CollectingDefaultNamespaceTest.GPXv11.Track.Extensions.DefaultNsExtension\",\"data\":\"Some comment\"}]}}]}"

    @Test
    override fun testGenericSerializeXml() {
        super.testGenericSerializeXml()
    }

    @Test
    fun testNamespaceDecls() {
        val serialized = baseXmlFormat.encodeToString(serializer, value)
        val lines = serialized.lines()
        for ((idx, line) in lines.drop(1).withIndex()) {
            assertFalse(actual = "xmlns" in line, "Namespace declaration found in line ${idx+1}: $line")
        }
        val xmlDecls = lines[0].split(" ")
            .filter { it.startsWith("xmlns:") }

        assertContains(xmlDecls, "xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"")
        assertEquals(1, xmlDecls.count { "=\"http://www.topografix.com/GPX/1/1\"" in it }, "Namespace declaration for topographix not found")
        assertEquals(2, xmlDecls.size)
    }

    @Serializable
    @XmlSerialName("gpx", GPXv11.NAMESPACE, "")
    data class GPXv11(
        val creator: String,
        val version: String = "1.1",
        val tracks: List<Track>
    ) {
        companion object {
            const val NAMESPACE = "http://www.topografix.com/GPX/1/1"
        }

        @Serializable
        @SerialName("trk")
        data class Track(
            @XmlElement(true)
            val name: String? = null,
            @XmlElement(true)
            val extensions: Extensions? = null
        ) {
            @Serializable
            @SerialName("extensions")
            class Extensions(
                vararg val gpxx: Extension,
            ) {



                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other == null || this::class != other::class) return false

                    other as Extensions

                    return gpxx.contentEquals(other.gpxx)
                }

                override fun hashCode(): Int {
                    return gpxx.contentHashCode()
                }

                @Serializable
                sealed class Extension

                /// http://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd
                @Serializable
                @XmlSerialName("TrackExtension", "http://www.garmin.com/xmlschemas/GpxExtensions/v3", "gpxx")
                data class TrackExtension(
                    @SerialName("DisplayColor") @XmlElement(true)
                    val displayColor: String? = null,
                ): Extension()

                @Serializable
                @XmlSerialName("comment", "", "")
                data class DefaultNsExtension(val data: String): Extension()
            }
        }
    }

}
