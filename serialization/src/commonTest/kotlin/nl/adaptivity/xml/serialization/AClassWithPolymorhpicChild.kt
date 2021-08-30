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

class AClassWithPolymorhpicChild : TestPolymorphicBase<AClassWithPolymorhpicChild.Container>(
    Container("lbl", ChildA("data")),
    Container.serializer(),
    baseModule
                                                                 ) {
    override val expectedXML: String
        get() = "<Container label=\"lbl\"><childA valueA=\"data\"/></Container>"
    override val expectedJson: String
        get() = "{\"label\":\"lbl\",\"member\":{\"type\":\"nl.adaptivity.xml.serialization.AClassWithPolymorhpicChild.ChildA\",\"valueA\":\"data\"}}"
    override val expectedNonAutoPolymorphicXML: String get() = "<Container label=\"lbl\"><member type=\".ChildA\"><value valueA=\"data\"/></member></Container>"


    @Serializable
    data class Container(val label: String, @Polymorphic val member: Base)

    @Serializable
    open class Base

    @Serializable
    @XmlSerialName("childA")
    data class ChildA(val valueA: String) : Base()

    @Serializable
    @SerialName("childBNameFromAnnotation")
    @XmlSerialName("childB")
    data class ChildB(val a: Int, val b: Int, val c: Int, val valueB: String) : Base()

    companion object {

        private val baseModule = SerializersModule {
            polymorphic(Base::class) {
                subclass(ChildA::class)
                subclass(ChildB::class)
            }
        }

    }

}
