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