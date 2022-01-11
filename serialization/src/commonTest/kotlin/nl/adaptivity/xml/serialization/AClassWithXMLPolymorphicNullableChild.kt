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
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class AClassWithXMLPolymorphicNullableChild : PlatformTestPolymorphicBase<AClassWithXMLPolymorphicNullableChild.Container4>(
    Container4("name2", ChildA("data")),
    Container4.serializer(),
    baseModule
                                                                             ) {
    override val expectedXML: String
        get() = "<Container4 name=\"name2\"><childA valueA=\"data\"/></Container4>"
    override val expectedNonAutoPolymorphicXML: String
        get() = expectedXML
    override val expectedXSIPolymorphicXML: String
        get() = expectedXML
    override val expectedJson: String
        get() = "{\"name\":\"name2\",\"child\":{\"type\":\"nl.adaptivity.xml.serialization.AClassWithXMLPolymorphicNullableChild.ChildA\",\"valueA\":\"data\"}}"


    @Serializable
    open class Base

    @Serializable
    @XmlSerialName("childA")
    data class ChildA(val valueA: String) : Base()

    @Serializable
    @SerialName("childBNameFromAnnotation")
    @XmlSerialName("childB")
    data class ChildB(val a: Int, val b: Int, val c: Int, val valueB: String) : Base()

    @Serializable
    data class Container4(val name: String, @XmlPolyChildren(arrayOf(".ChildA", "childBNameFromAnnotation=better")) val child: @Polymorphic Base?)

    companion object {

        private val baseModule = SerializersModule {
            polymorphic(Base::class) {
                subclass(ChildA::class)
                subclass(ChildB::class)
            }
        }

    }

}
