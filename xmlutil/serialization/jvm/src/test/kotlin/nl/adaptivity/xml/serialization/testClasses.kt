/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@XmlSerialName("address")
data class Address(val houseNumber: String, val street: String, val city: String)

@Serializable
open class Base

@Serializable
@XmlSerialName("childA")
data class ChildA(val valueA: String): Base()

@Serializable
@XmlSerialName("childB")
data class ChildB(val valueB: String): Base()

@Serializable
data class Container(val label: String, val member: Base)

@Serializable
data class Container2(val name:String, @XmlPolyChildren(arrayOf("ChildA", "ChildB=better")) val children: List<Base>)

@Serializable
data class Container3(val xxx: String, @SerialName("member") val members: List<Base>)

@Serializable
sealed /*open*/ class SealedParent

@Serializable
data class SealedA(val data: String, val extra:String="2"): SealedParent()
@Serializable
data class SealedB(val main: String, val ext: Float=0.5F): SealedParent()

@Serializable
data class Sealed(val name: String, val members: List<SealedParent>)

@Serializable
data class SealedSingle(val name: String, val member: SealedA)

@Serializable
data class Business(val name: String, val headOffice: Address?)

@Serializable
@XmlSerialName("chamber")
data class Chamber(val name: String, @SerialName("member") val members: List<Business>)

@Serializable
@XmlSerialName("localname", "urn:namespace")
data class Special(val paramA: String = "valA",
                   @XmlSerialName("paramb", namespace = "urn:ns2", prefix = "")
                   @XmlElement(true) val paramB: Int = 1,
                   @SerialName("flags")
                   @XmlChildrenName("flag", namespace="urn:flag", prefix="f")
                   val param: List<Int> = listOf(2, 3, 4, 5, 6))

@Serializable
data class Inverted(@XmlElement(true)
                    val elem:String = "value",
                    val arg: Short=6)