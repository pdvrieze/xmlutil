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

class AClassWithMultipleChildren : TestPolymorphicBase<AClassWithMultipleChildren.Container2>(
    Container2("name2", listOf(ChildA("data"), ChildB(1, 2, 3, "xxx"))),
    Container2.serializer(),
    baseModule
                                                                                             ) {
    override val expectedXML: String
        get() = "<Container2 name=\"name2\"><childA valueA=\"data\"/><better a=\"1\" b=\"2\" c=\"3\" valueB=\"xxx\"/></Container2>"
    override val expectedNonAutoPolymorphicXML: String
        get() = expectedXML
    override val expectedJson: String
        get() = "{\"name\":\"name2\",\"children\":[{\"type\":\"nl.adaptivity.xml.serialization.AClassWithMultipleChildren.ChildA\",\"valueA\":\"data\"},{\"type\":\"childBNameFromAnnotation\",\"a\":1,\"b\":2,\"c\":3,\"valueB\":\"xxx\"}]}"


    @Serializable
    open class Base

    @Serializable
    @XmlSerialName("childA", namespace = "", prefix = "")
    data class ChildA(val valueA: String) : Base()

    @Serializable
    @SerialName("childBNameFromAnnotation")
    @XmlSerialName("childB", namespace = "", prefix = "")
    data class ChildB(val a: Int, val b: Int, val c: Int, val valueB: String) : Base()

    @Serializable
    data class Container2(val name: String, @XmlPolyChildren(arrayOf(".ChildA", "childBNameFromAnnotation=better")) val children: List<@Polymorphic Base>)

    companion object {

        private val baseModule = SerializersModule {
            polymorphic(Base::class) {
                subclass(ChildA::class)
                subclass(ChildB::class)
            }
        }

    }

}