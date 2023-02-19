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

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class ASimplerClassWithUnspecifiedChildren :
    PlatformTestPolymorphicBase<ASimplerClassWithUnspecifiedChildren.Container3>(
        Container3("name2", listOf(ChildA("data"), ChildB(4, 5, 6, "xxx"), ChildA("yyy"))),
        Container3.serializer(),
        baseModule
    ) {
    override val expectedXML: String
        get() = "<container-3 xxx=\"name2\"><childA valueA=\"data\"/><childB a=\"4\" b=\"5\" c=\"6\" valueB=\"xxx\"/><childA valueA=\"yyy\"/></container-3>"
    override val expectedJson: String
        get() = "{\"xxx\":\"name2\",\"member\":[{\"type\":\"nl.adaptivity.xml.serialization.ASimplerClassWithUnspecifiedChildren.ChildA\",\"valueA\":\"data\"},{\"type\":\"childBNameFromAnnotation\",\"a\":4,\"b\":5,\"c\":6,\"valueB\":\"xxx\"},{\"type\":\"nl.adaptivity.xml.serialization.ASimplerClassWithUnspecifiedChildren.ChildA\",\"valueA\":\"yyy\"}]}"
    override val expectedNonAutoPolymorphicXML: String
        get() = "<container-3 xxx=\"name2\"><member type=\"nl.adaptivity.xml.serialization.ASimplerClassWithUnspecifiedChildren.ChildA\"><value valueA=\"data\"/></member><member type=\"childBNameFromAnnotation\"><value a=\"4\" b=\"5\" c=\"6\" valueB=\"xxx\"/></member><member type=\"nl.adaptivity.xml.serialization.ASimplerClassWithUnspecifiedChildren.ChildA\"><value valueA=\"yyy\"/></member></container-3>"
    override val expectedXSIPolymorphicXML: String
        get() = "<container-3 xxx=\"name2\">" +
                "<member xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"childA\" valueA=\"data\"/>" +
                "<member xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"childB\" a=\"4\" b=\"5\" c=\"6\" valueB=\"xxx\"/>" +
                "<member xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"childA\" valueA=\"yyy\"/></container-3>"

    @Serializable
    open class Base

    @Serializable
    @XmlSerialName("childA")
    data class ChildA(val valueA: String) : Base()

    @Serializable
    @SerialName("childBNameFromAnnotation")
    @XmlSerialName("childB")
    data class ChildB(val a: Int, val b: Int, val c: Int, val valueB: String) : Base()


    @SerialName("container-3")
    @Serializable
    data class Container3(val xxx: String, @SerialName("member") val members: List<@Polymorphic Base>)

    companion object {

        private val baseModule = SerializersModule {
            polymorphic(Base::class) {
                subclass(ChildA::class)
                subclass(ChildB::class)
            }
        }

    }

}
