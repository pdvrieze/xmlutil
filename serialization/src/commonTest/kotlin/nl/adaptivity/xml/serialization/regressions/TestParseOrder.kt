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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test for #188 */
class TestParseOrder {

    @Test
    fun testOrderedParsing() {
        val xml = XML { defaultPolicy { autoPolymorphic=true } }
        
        val data = """
            <Pnts>
                <P id="2">412178.5056976396 1322154.0438618148 -0.06104896051</P>
                <P id="3">412179.00073756033 1322154.1141126873 -0.066606280826</P>
                <P id="4">412179.05867695622 1322153.7058285919 -0.066913125887</P>
                <P id="5">412179.55371716584 1322153.7760795054 -0.063118164421</P>
                <P id="6">412178.56363703904 1322153.6355777199 -0.060903200248</P>
                <P id="7">412179.14123938745 1322153.1240322681 -0.067685237375</P>
                <P id="8">412179.63627959706 1322153.1942831816 -0.063790636278</P>
                <P id="9">412178.64619947522 1322153.0537813967 -0.060455217987</P>
            </Pnts>
        """.trimIndent()

        val parsed = xml.decodeFromString(ListSerializer(LandXMLPoint.serializer()), data)
        assertEquals(8, parsed.size)
        assertEquals((2..9).toList(), parsed.map { it.id })
        assertEquals("412178.5056976396 1322154.0438618148 -0.06104896051", parsed[0].dataStr)
        assertEquals("412179.00073756033 1322154.1141126873 -0.066606280826", parsed[1].dataStr)
    }

    @Serializable
    @XmlSerialName("LandXML", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXML(
        val date: String,
        val time: String,
        val units: LandXMLUnits,
        val coordinateSystem: LandXMLCoordinateSystem?,
        val project: LandXMLProject,
        val application: LandXMLApplication,
//        @XmlSerialName("Surfaces", "http://www.landxml.org/schema/LandXML-1.2", "")
//        @XmlChildrenName("Surface", "http://www.landxml.org/schema/LandXML-1.2", "")
        val surfaces: LandXMLSurfaces,
    )

    @Serializable
    @XmlSerialName("Units", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLUnits(
        val metric: LandXMLMetric?,
    )

    @Serializable
    @XmlSerialName("Metric", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLMetric(
        val linearUnit: String,
        val areaUnit: String,
        val volumeUnit: String,
        val temperatureUnit: String,
        val pressureUnit: String,
        val angularUnit: String,
        val directionUnit: String,
    )

    @Serializable
    @XmlSerialName("CoordinateSystem", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLCoordinateSystem(
        val desc: String,
        val horizontalDatum: String,
        val verticalDatum: String,
        val datum: String,
        val horizontalCoordinateSystemName: String,
        val projectedCoordinateSystemName: String,
        val verticalCoordinateSystemName: String,
    )

    @Serializable
    @XmlSerialName("Project", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLProject(
        val name: String,
    )

    @Serializable
    @XmlSerialName("Application", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLApplication(
        val name: String,
        val manufacturer: String,
        val desc: String,
        val manufacturerURL: String,
        val timeStamp: String,
    )

    @Serializable
    @XmlSerialName("Surfaces", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLSurfaces(
        val name: String?,
        val surfaces: List<LandXMLSurface>,
    )

    @Serializable
    @XmlSerialName("Surface", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLSurface(
        val name: String?,
        val desc: String?,
        val definition: LandXMLDefinition,
    )

    @Serializable
    @XmlSerialName("Definition", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLDefinition(
        val surfType: String,
        val points: LandXMLPoints,
        val triangles: LandXMLTriangles,
    )

    @Serializable
    @XmlSerialName("Pnts", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLPoints(
        val points: List<LandXMLPoint>,
    )

    @Serializable
    @XmlSerialName("P", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLPoint(
        val id: Int,
        @XmlValue(true) val dataStr: String,
    )

    @Serializable
    @XmlSerialName("Faces", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLTriangles(
        val triangles: List<LandXMLTriangle>,
    )

    @Serializable
    @XmlSerialName("F", "http://www.landxml.org/schema/LandXML-1.2", "")
    data class LandXMLTriangle(
        @XmlValue(true) val dataStr: String,
    )
}
